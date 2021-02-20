package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

/**
 * Checks online and/or offline player permissions, if there's vault.
 * @author GatheringExp
 *
 */
public class PermissionChecker {
    /**
     * Checks if an online player has permissions
     * @param at The AnimalTamer that might be an online player (specifically, instanceof {@link Permissible})
     * @param permission The permission to check
     * @return Whether or not the player has the permission
     */
    public static boolean onlineHasPerms(AnimalTamer at, String permission) {
        return (at instanceof Permissible && ((Permissible)at).hasPermission(permission));
    }
    
    /**
     * Checks if an offline player has permissions with vault
     * @param at The AnimalTamer that should be an OfflinePlayer
     * @param world The world in which the player might have the permission
     * @param permission The permission to check
     * @param thisPlugin a pointer to the {@link TPPets} plugin instance
     * @return Whether or not the player has the permission, or false if vault is disabled
     */
    public static boolean offlineHasPerms(AnimalTamer at, String permission, World world, TPPets thisPlugin) {
        // Player extends OfflinePlayer
        return (world != null && thisPlugin.getVaultEnabled() && at instanceof OfflinePlayer && thisPlugin.getPerms().playerHas(world.getName(), (OfflinePlayer) at, permission));
    }

    public static boolean hasPermissionToTeleportType(PetType.Pets petType, Player player) {
        String test = "tppets." + petType.toString().toLowerCase() + "s";
        return player.hasPermission(test);
    }
}
