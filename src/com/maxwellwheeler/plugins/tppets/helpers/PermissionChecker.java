package com.maxwellwheeler.plugins.tppets.helpers;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.permissions.Permissible;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class PermissionChecker {
    public static boolean onlineHasPerms(AnimalTamer at, String permission) {
        return (at instanceof Permissible && ((Permissible)at).hasPermission(permission));
    }
    
    public static boolean offlineHasPerms(AnimalTamer at, String permission, World world, TPPets thisPlugin) {
        // Player extends OfflinePlayer
        return (at instanceof OfflinePlayer && thisPlugin.getPerms().playerHas(world.getName(), (OfflinePlayer) at, permission));
    }
}
