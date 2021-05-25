package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a lost and found region, a cube region where pets that are found in a {@link ProtectedRegion} without
 * permission can be teleported to.
 * @author GatheringExp
 */
public class LostAndFoundRegion extends Region {
    /**
     * Constructor for when the world is only known by its name. Primarily used when reconstructing regions from the db.
     * @param regionName The LostAndFoundRegion's name.
     * @param worldName The LostAndFoundRegion's world's name.
     * @param minX The cube region's minimum x coordinate.
     * @param minY The cube region's minimum y coordinate.
     * @param minZ The cube region's minimum z coordinate.
     * @param maxX The cube region's maximum x coordinate.
     * @param maxY The cube region's maximum y coordinate.
     * @param maxZ The cube region's maximum z coordinate.
     */
    public LostAndFoundRegion(String regionName, String worldName, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {
        super(regionName, worldName, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Constructor for when the world object is known.
     * @param regionName The LostAndFoundRegion's name.
     * @param worldName The LostAndFoundRegion's world's name.
     * @param world The LostAndFoundRegion's world.
     * @param minLoc The cube region's minimum point.
     * @param maxLoc The cube region's maximum point.
     */
    public LostAndFoundRegion(String regionName, String worldName, World world, Location minLoc, Location maxLoc) {
        super(regionName, worldName, world, minLoc, maxLoc);
    }
    
    /**
     * Gets an integer in the center of min and max, rounded down.
     * @param min The lower bound.
     * @param max The upper bound.
     * @return A value in the center of min and max, rounded down.
     */
    private int getMiddleInt(int min, int max) {
        return min + ((max-min)/2);
    }
    
    /**
     * Gets the center of the lost and found region, rounded down.
     * @return A location representing the center of the lost and found region, rounded down.
     */
    public Location getApproxCenter() {
        return new Location(this.minLoc.getWorld(), getMiddleInt(this.minLoc.getBlockX(), this.maxLoc.getBlockX()), getMiddleInt(this.minLoc.getBlockY(), this.maxLoc.getBlockY()), getMiddleInt(this.minLoc.getBlockZ(), this.maxLoc.getBlockZ()));
    }
}
