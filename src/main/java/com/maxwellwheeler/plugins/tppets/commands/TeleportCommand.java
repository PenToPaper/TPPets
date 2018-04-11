package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.util.*;

public abstract class TeleportCommand {
    protected TPPets thisPlugin;

    public TeleportCommand(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    abstract protected boolean hasPermissionToTp(Player pl, OfflinePlayer petOwner, String petUUID);

    protected void loadApplicableChunks(List<PetStorage> psList) {
        for (PetStorage ps : psList) {
            World world = Bukkit.getWorld(ps.petWorld);
            if (world != null) {
                Chunk tempLoadedChunk = getChunkFromCoords(world, ps.petX, ps.petZ);
                tempLoadedChunk.load();
            }
        }
    }

    protected Chunk getChunkFromCoords(World world, int x, int z) {
        return new Location(world, x, 64, z).getChunk();
    }

    protected boolean teleportSpecificPet(Location sendTo, OfflinePlayer petOwner, String name, PetType.Pets pt, boolean setSitting, boolean kickPlayerOff, boolean strictType) {
        if (thisPlugin.getDatabase() != null && petOwner != null && name != null && (!pt.equals(PetType.Pets.UNKNOWN) || !strictType)) {
            List<PetStorage> psList = new ArrayList<>();
            if (strictType) {
                psList = thisPlugin.getDatabase().getPetsFromOwnerNamePetType(petOwner.getUniqueId().toString(), name, pt);
            } else {
                PetStorage ps = thisPlugin.getDatabase().getPetByName(petOwner.getUniqueId().toString(), name);
                if (ps != null) {
                    psList.add(ps);
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
                for (Entity ent : sendTo.getWorld().getEntitiesByClasses(PetType.getClassTranslate(pt))) {
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

    protected boolean isTeleportablePet(OfflinePlayer sendFrom, Entity ent, PetType.Pets pt) {
        if (ent instanceof Tameable) {
            Tameable tameableTemp = (Tameable) ent;
            return tameableTemp.isTamed() && tameableTemp.getOwner() != null && tameableTemp.getOwner().equals(sendFrom) && PetType.getEnumByEntity(ent).equals(pt);
        }
        return false;
    }

    /**
     * Teleports all pets owned by sendFrom of type pt to sendTo
     * @param sendTo The player to send the pets to
     * @param sendFrom The player who owns the pets
     * @param pt The type of pets to teleport
     * @return A set of UUIDs of the pets that have been teleported
     */
    protected Set<UUID> getPetsAndTeleport(Location sendTo, OfflinePlayer sendFrom, PetType.Pets pt, boolean setSitting, boolean kickPlayerOff) {
        List<World> worlds = Bukkit.getServer().getWorlds();
        Set<UUID> teleportedEnts = new HashSet<>();
        // If youc an teleport between worlds, check every world
        if (thisPlugin.getAllowTpBetweenWorlds()) {
            for (World world : worlds) {
                Set<UUID> teleportedTemp = loadAndTp(sendTo, sendFrom, world, pt, teleportedEnts, setSitting, kickPlayerOff);
                if (teleportedTemp != null) {
                    teleportedEnts.addAll(teleportedTemp);
                }
            }
        } else {
            // If you can't teleport between worlds, check only the world the player is in
            Set<UUID> teleportedTemp = loadAndTp(sendTo, sendFrom, sendTo.getWorld(), pt, teleportedEnts, setSitting, kickPlayerOff);
            if (teleportedTemp != null) {
                teleportedEnts.addAll(teleportedTemp);
            }
        }
        return teleportedEnts;
    }

    /**
     * Teleports all pets owned by sendFrom of type pt in world world not already in set alreadyTeleportedPets to sendTo
     * @param sendTo The player to send the pets to
     * @param sendFrom The player who owns the pets
     * @param world The world to check
     * @param pt The type of pets to teleport
     * @param alreadyTeleportedPets A set representing pets that have already been teleported, so that duplicates can't exist
     * @return alreadyTeleportedPets plus the new pets teleported by this command
     */
    protected Set<UUID> loadAndTp(Location sendTo, OfflinePlayer sendFrom, World world, PetType.Pets pt, Set<UUID> alreadyTeleportedPets, boolean setSitting, boolean kickPlayerOff) {
        if (thisPlugin.getDatabase() != null && sendFrom != null && sendTo != null && world != null) {
            List<PetStorage> unloadedPetsInWorld = thisPlugin.getDatabase().getPetsGeneric(sendFrom.getUniqueId().toString(), world.getName(), pt);
            loadApplicableChunks(unloadedPetsInWorld);
            for (Entity ent : world.getEntitiesByClasses(PetType.getClassTranslate(pt))) {
                if (isTeleportablePet(sendFrom, ent, pt)) {
                    if (!alreadyTeleportedPets.contains(ent.getUniqueId())) {
                        teleportPet(sendTo, ent, !sendTo.equals(sendFrom) && setSitting, kickPlayerOff);
                        alreadyTeleportedPets.add(ent.getUniqueId());
                    } else {
                        ent.remove();
                    }
                }
            }
            return alreadyTeleportedPets;
        }
        return null;
    }

    protected boolean shouldKeepSitting(Player sender, OfflinePlayer petOwner) {
        return !sender.equals(petOwner) && sender.hasPermission("tppets.teleportother");
    }

}
