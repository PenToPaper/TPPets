package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

/**
 * Checks player permissions of online and offline players. Offline player support depends on Vault.
 * @author GatheringExp
 */
public class PermissionChecker {
    /**
     * Checks if an online {@link AnimalTamer} has a given permission. This method determines if the {@link AnimalTamer}
     * can have permissions before checking if it actually has a specific permission.
     * @param at The {@link AnimalTamer} to evaluate.
     * @param permission The permission name to check.
     * @return true if the {@link AnimalTamer} has the permission, false if they do not, or are not {@link Permissible}
     */
    public static boolean onlineHasPerms(AnimalTamer at, String permission) {
        return (at instanceof Permissible && ((Permissible)at).hasPermission(permission));
    }
    
    /**
     * Checks if an offline {@link AnimalTamer} has a given permission with Vault. This method determines if the
     * {@link AnimalTamer} is an {@link OfflinePlayer} before checking if it actually has a specific permission.
     * @param at The {@link AnimalTamer} to evaluate.
     * @param world The world context where the permission is being checked. This is necessary for Vault.
     * @param permission The permission name to check.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @return true if player has permission through Vault, false if Vault not enabled or player doesn't have permission.
     */
    public static boolean offlineHasPerms(AnimalTamer at, String permission, World world, TPPets thisPlugin) {
        // Player extends OfflinePlayer
        return (world != null && thisPlugin.getVaultEnabled() && at instanceof OfflinePlayer && thisPlugin.getPerms().playerHas(world.getName(), (OfflinePlayer) at, permission));
    }

    /**
     * Checks if a player has permission to teleport a particular pet type. Uses tppets.[Pet Type]s permissions.
     * @param petType The pet type permission to check. {@link PetType.Pets#UNKNOWN} is allowed, but no
     *               such permission exists.
     * @param player The player to check for the permission.
     * @return true if player has permission, false if not.
     */
    public static boolean hasPermissionToTeleportType(PetType.Pets petType, Player player) {
        return player.hasPermission("tppets." + petType.toString().toLowerCase() + "s");
    }
}
