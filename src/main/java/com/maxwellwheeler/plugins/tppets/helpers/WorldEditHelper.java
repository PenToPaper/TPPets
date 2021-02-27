package com.maxwellwheeler.plugins.tppets.helpers;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class WorldEditHelper {
    public static Location[] getWePoints(WorldEditPlugin worldEditPlugin, Player player) {
        Location[] ret = null;
        try {
            // TODO: Look into refactoring by using the bukkit-specific version of worldedit
            // This scuffed conversion from worldedit to bukkit worlds makes sure an error is thrown to the user if the worldedit and bukkit worlds do not have matching names.
            World bukkitWorld = Bukkit.getWorld(worldEditPlugin.getSession(player).getSelectionWorld().getName());
            Region region = worldEditPlugin.getSession(player).getSelection(worldEditPlugin.getSession(player).getSelectionWorld());
            if (region instanceof CuboidRegion) {
                ret = new Location[] {weVectorToLocation(bukkitWorld, region.getMinimumPoint()), weVectorToLocation(bukkitWorld, region.getMaximumPoint())};
            }
        } catch (IncompleteRegionException ignored) {}
        return ret;
    }

    public static Location weVectorToLocation(World world, BlockVector3 blockVector3) {
        return new Location(world, blockVector3.getX(), blockVector3.getY(), blockVector3.getZ());
    }
}
