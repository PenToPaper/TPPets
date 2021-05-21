package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.Location;
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

    protected boolean teleportPetFromStorage(Location sendTo, @NotNull PetStorage petStorage, boolean setSitting, boolean kickPlayerOff) throws SQLException {
        loadChunkFromPetStorage(petStorage);
        Entity entity = getEntity(petStorage);
        if (entity != null && teleportPet(sendTo, entity, setSitting, kickPlayerOff)) {
            this.thisPlugin.getDatabase().updatePetLocation(entity);
            return true;
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

    protected PetType.Pets getPetType(String petType) {
        // If there's a PetType.Pets added in the future that has an "s" at the end of it, this will need to be reworked
        // Avoided using recursion for now to avoid potential exploits from malicious players
        try {
            if (petType.length() > 1 && petType.substring(petType.length() - 1).equalsIgnoreCase("s")) {
                return PetType.Pets.valueOf(petType.substring(0, petType.length() - 1).toUpperCase());
            } else {
                return PetType.Pets.valueOf(petType.toUpperCase());
            }
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

}
