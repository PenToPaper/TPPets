package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.util.*;

/**
 * Object used for store commands
 * @author GatheringExp
 */
public abstract class TeleportCommand extends BaseCommand {
    /**
     * Generic constructor, takes TPPets plugin instance.
     * @param thisPlugin The TPPets plugin instance.
     */
    public TeleportCommand(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

//    /**
//     * Whether or not the player has permission to teleport the pet
//     * @param pl The player to check the permissions of
//     * @param petOwner The owner of the pet
//     * @param petUUID The UUID of the pet
//     * @return If the player has permission to tp
//     */
//    abstract protected boolean hasPermissionToTp(Player pl, OfflinePlayer petOwner, String petUUID);

    /**
     * Loads the chunks needed to teleport pets from psList
     * @param psList A list of PetStorage objects representing the last place the pets were found at
     */
    protected void loadApplicableChunks(List<PetStorage> psList) {
        for (PetStorage ps : psList) {
            World world = Bukkit.getWorld(ps.petWorld);
            if (world != null) {
                Chunk tempLoadedChunk = getChunkFromCoords(world, ps.petX, ps.petZ);
                tempLoadedChunk.getEntities();
                tempLoadedChunk.load();
            }
        }
    }

    /**
     * Gets chunk from normal coordinates
     * @param world The world the chunk is in
     * @param x The x coordinate of the location
     * @param z The z coordinate of the location
     * @return The found chunk, could be null
     */
    protected Chunk getChunkFromCoords(World world, int x, int z) {
        return world.getChunkAt(x, z);
    }

    /**
     * Teleports a specific pet to a location
     * @param sendTo The location to send the pet
     * @param petOwner The owner of the pet
     * @param name The name of the pet
     * @param pt The type of the pet
     * @param setSitting Whether or not the pet should be set sitting after the teleportation
     * @param kickPlayerOff Whether or not any players riding the pet should be kicked off before teleportation
     * @param strictType Whether or not the pet should strictly be of the pt type
     * @return True if the pet was found and teleported, false otherwise
     */

    protected boolean teleportPetsFromStorage(Location sendTo, List<PetStorage> petStorageList, boolean setSitting, boolean kickPlayerOff) {
        boolean teleportResult = false;
        for (PetStorage petStorage : petStorageList) {
            World world = Bukkit.getWorld(petStorage.petWorld);
            if (world != null && (thisPlugin.getAllowTpBetweenWorlds() || world.equals(sendTo.getWorld()))) {
                Chunk petChunk = getChunkFromCoords(world, petStorage.petX, petStorage.petZ);
                petChunk.load();
                Entity entity = getEntityInChunk(petChunk, petStorage);
                if (entity != null && teleportPet(sendTo, entity, setSitting, kickPlayerOff)) {
                    thisPlugin.getDatabase().updateOrInsertPet(entity);
                    teleportResult = true;
                }
            }
        }
        return teleportResult;
    }

    protected Entity getEntityInChunk(Chunk chunk, PetStorage petStorage) {
        for (Entity entity : chunk.getEntities()) {
            if (UUIDUtils.trimUUID(entity.getUniqueId()).equals(petStorage.petId)) {
                return entity;
            }
        }
        return null;
    }

    protected boolean teleportSpecificPet(Location sendTo, OfflinePlayer petOwner, String name, PetType.Pets pt, boolean setSitting, boolean kickPlayerOff, boolean strictType) {
        if (thisPlugin.getDatabase() != null && petOwner != null && name != null && (!pt.equals(PetType.Pets.UNKNOWN) || !strictType)) {
            List<PetStorage> psList = new ArrayList<>();
            if (strictType) {
                psList = thisPlugin.getDatabase().getPetsFromOwnerNamePetType(petOwner.getUniqueId().toString(), name, pt);
            } else {
                psList = thisPlugin.getDatabase().getPetByName(petOwner.getUniqueId().toString(), name);
                if (psList == null) {
                    return false;
                }
            }
            if (psList.size() < 1) {
                return false;
            }
            loadApplicableChunks(psList);
            // If you can teleport between worlds, check every world
            if (thisPlugin.getAllowTpBetweenWorlds()) {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity ent : strictType ? world.getEntitiesByClasses(PetType.getClassTranslate(pt)) : world.getEntitiesByClasses(org.bukkit.entity.Tameable.class)) {
                        for (PetStorage ps : psList) {
                            if (UUIDUtils.trimUUID(ent.getUniqueId()).equals(ps.petId)) {
                                boolean tpResult = teleportPet(sendTo, ent, !sendTo.equals(petOwner) && setSitting, kickPlayerOff);
                                thisPlugin.getDatabase().updateOrInsertPet(ent);
                                return tpResult;
                            }
                        }
                    }
                }
            } else {
                // If you can't teleport between worlds, check only the world the player is in.
                for (Entity ent : strictType ? sendTo.getWorld().getEntitiesByClasses(PetType.getClassTranslate(pt)) : sendTo.getWorld().getEntitiesByClasses(org.bukkit.entity.Tameable.class)) {
                    for (PetStorage ps : psList) {
                        if (UUIDUtils.trimUUID(ent.getUniqueId()).equals(ps.petId)) {
                            boolean tpResult = teleportPet(sendTo, ent, !sendTo.equals(petOwner) && setSitting, kickPlayerOff);
                            thisPlugin.getDatabase().updateOrInsertPet(ent);
                            return tpResult;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Teleports an individual pet to a location
     * @param loc The location to teleport the pet
     * @param entity The entity to teleport
     * @param setSitting Whether or not the pet should be set to sit after the teleporting
     * @param kickPlayerOff Whether or not any players riding the entity should be kicked off before teleportation
     * @return True if successful, false if not
     */
    protected boolean teleportPet(Location loc, Entity entity, boolean setSitting, boolean kickPlayerOff) {
        if (!kickPlayerOff && (entity.getPassengers() != null && entity.getPassengers().size() != 0)) {
            return false;
        }
        EntityActions.setStanding(entity);
        if (kickPlayerOff) {
            EntityActions.removePassenger(entity);
        }
        entity.teleport(loc);
        if (setSitting) {
            EntityActions.setSitting(entity);
        }
        return true;
    }


    /**
     * Formats the location in a readable way
     * @param lc Location to format
     * @return The readable string of location data
     */
    static String formatLocation(Location lc) {
        return "x: " + Integer.toString(lc.getBlockX()) + ", " + "y: " + Integer.toString(lc.getBlockY()) + ", " + "z: " + Integer.toString(lc.getBlockZ());
    }

}
