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
 * Class representing any TPP teleport command.
 * @author GatheringExp
 */
public abstract class TeleportCommand extends BaseCommand {
    /**
     * Relays data to {@link BaseCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments, depending on the expected command.
     */
    public TeleportCommand(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * Teleports a specific pet from {@link PetStorage} to a location.
     * @param sendTo The location to send the pet.
     * @param setSitting Whether or not the pet should be set sitting after the teleportation.
     * @param kickPlayerOff Whether or not any players riding the pet should be kicked off before teleportation.
     * @return true if the pet was found and teleported, false if not.
     */
    protected boolean teleportPetFromStorage(Location sendTo, @NotNull PetStorage petStorage, boolean setSitting, boolean kickPlayerOff) {
        loadChunkFromPetStorage(petStorage);
        Entity entity = getEntity(petStorage);
        if (entity != null && teleportPet(sendTo, entity, setSitting, kickPlayerOff)) {
            try {
                this.thisPlugin.getDatabase().updatePetLocation(entity);
            } catch (SQLException ignored) {}
            return true;
        }
        return false;
    }

    /**
     * Performs the teleport operation on a given entity.
     * @param sendTo The location to send the pet.
     * @param entity The entity to teleport.
     * @param setSitting Whether or not the pet should be set sitting after the teleportation.
     * @param kickPlayerOff Whether or not any players riding the pet should be kicked off before teleportation.
     * @return true if the pet was found and teleported, false if not.
     */
    protected boolean teleportPet(Location sendTo, Entity entity, boolean setSitting, boolean kickPlayerOff) {
        if (!kickPlayerOff && entity.getPassengers().size() != 0) {
            return false;
        }
        EntityActions.setStanding(entity);
        if (kickPlayerOff) {
            entity.eject();
        }
        entity.teleport(sendTo);
        if (setSitting) {
            EntityActions.setSitting(entity);
        }
        return true;
    }

    /**
     * Gets whether or not a player can teleport to a given world name, based on where the player is currently.
     * @param player The player attempting to teleport to a world.
     * @param petWorldName The destination world name.
     * @return true if can teleport to that world, false if not.
     */
    protected boolean canTpToWorld(Player player, String petWorldName) {
        return this.thisPlugin.getAllowTpBetweenWorlds() || player.getWorld().getName().equals(petWorldName) || player.hasPermission("tppets.tpanywhere");
    }

    /**
     * Gets a pet type from a string in format: [Pet Type]s or [PetType]. Case insensitive.
     * @param petType A string representing the pet type. Can be plural. Case insensitive.
     * @return A {@link PetType.Pets} value if valid input string, null if not.
     */
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
