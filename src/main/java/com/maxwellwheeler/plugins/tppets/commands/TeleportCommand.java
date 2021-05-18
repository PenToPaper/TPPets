package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

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

    /**
     * Teleports a specific pet to a location
     * @param sendTo The location to send the pet
     * @param setSitting Whether or not the pet should be set sitting after the teleportation
     * @param kickPlayerOff Whether or not any players riding the pet should be kicked off before teleportation
     * @return True if the pet was found and teleported, false otherwise
     */

    protected boolean teleportPetsFromStorage(Location sendTo, @NotNull PetStorage petStorage, boolean setSitting, boolean kickPlayerOff) throws SQLException {
        World world = Bukkit.getWorld(petStorage.petWorld);
        if (world != null) {
            Chunk petChunk = world.getChunkAt(petStorage.petX >> 4, petStorage.petZ >> 4);
            petChunk.load();
            Entity entity = getEntityFromWorld(world, petStorage);
            if (entity != null && teleportPet(sendTo, entity, setSitting, kickPlayerOff)) {
                this.thisPlugin.getDatabase().updatePetLocation(entity);
                return true;
            }
        }
        return false;
    }

    protected Entity getEntityFromWorld(World world, PetStorage petStorage) {
        // Internally, using the getEntitiesByClasses goes through this same loop. So this isn't a performance loss. It's actually more efficient because it only loops through it all once.
        String petId = UUIDUtils.unTrimUUID(petStorage.petId);
        for (Entity entity : world.getEntities()) {
            if (entity.getUniqueId().toString().equals(petId)) {
                return entity;
            }
        }
        return null;
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
        if (!kickPlayerOff && entity.getPassengers().size() != 0) {
            return false;
        }
        EntityActions.setStanding(entity);
        if (kickPlayerOff) {
            entity.eject();
        }
        entity.teleport(loc);
        if (setSitting) {
            EntityActions.setSitting(entity);
        }
        return true;
    }

    protected boolean canTpToWorld(Player player, String petWorldName) {
        return this.thisPlugin.getAllowTpBetweenWorlds() || player.getWorld().getName().equals(petWorldName) || player.hasPermission("tppets.tpanywhere");
    }

    /**
     * Formats the location in a readable way
     * @param lc Location to format
     * @return The readable string of location data
     */
    static String formatLocation(Location lc) {
        return "x: " + lc.getBlockX() + ", " + "y: " + lc.getBlockY() + ", " + "z: " + lc.getBlockZ();
    }

}
