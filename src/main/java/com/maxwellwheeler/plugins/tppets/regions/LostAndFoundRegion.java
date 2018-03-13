package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a lost and found region, a region where pets that are in a given {@link ProtectedRegion} are teleported to if they enter
 * @author GatheringExp
 *
 */
public class LostAndFoundRegion extends Region {

    @Override
    public String toString() {
        return String.format("zoneName = %s; worldName = %s; x1: %d; y1: %d; z1: %d; x2: %d; y2: %d; z2: %d", zoneName, worldName, minLoc.getBlockX(), minLoc.getBlockY(), minLoc.getBlockZ(), maxLoc.getBlockX(), maxLoc.getBlockY(), maxLoc.getBlockZ());
    }
    
    /**
     * General constructor, just sends arguments to superclass.
     * @param zoneName The zone's name.
     * @param worldName The zone's world's name.
     * @param minX The box that represents the region's space has a minimum point and maximum point. This represents its minimum point's x coordinate.
     * @param minY The box that represents the region's space has a minimum point and maximum point. This represents its minimum point's y coordinate.
     * @param minZ The box that represents the region's space has a minimum point and maximum point. This represents its minimum point's z coordinate.
     * @param maxX The box that represents the region's space has a minimum point and maximum point. This represents its maximum point's x coordinate.
     * @param maxY The box that represents the region's space has a minimum point and maximum point. This represents its maximum point's y coordinate.
     * @param maxZ The box that represents the region's space has a minimum point and maximum point. This represents its maximum point's z coordinate.
     */
    public LostAndFoundRegion(String zoneName, String worldName, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {
        super(zoneName, worldName, minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * General constructor, just sends arguments to superclass.
     * @param zoneName The zone's name.
     * @param worldName The zone's world's name.
     * @param minLoc A location object representing the region's minimum point.
     * @param maxLoc A location object representing the region's maximum point.
     */
    public LostAndFoundRegion(String zoneName, String worldName, Location minLoc, Location maxLoc) {
        super(zoneName, worldName, minLoc, maxLoc);
    }
    
    /**
     * General constructor, just sends arguments to superclass.
     * @param zoneName The zone's name.
     * @param worldName The zone's world's name.
     * @param world The zone's world.
     * @param minLoc A location object representing the region's minimum point.
     * @param maxLoc A location object representing the region's maximum point.
     */
    public LostAndFoundRegion(String zoneName, String worldName, World world, Location minLoc, Location maxLoc) {
        super(zoneName, worldName, world, minLoc, maxLoc);
    }
    
    /**
     * Gets an integer roughly in the center of the min and max provided.
     * @param min The lower bound.
     * @param max The upper bound.
     * @return A value roughly in the middle of the min and max.
     */
    private int getMiddleInt(int min, int max) {
        return min + ((max-min)/2);
    }
    
    /**
     * Gets the approximate center of the lost and found region
     * @return Location data for the center of the lost and found region
     */
    public Location getApproxCenter() {
        return new Location(minLoc.getWorld(), getMiddleInt(minLoc.getBlockX(), maxLoc.getBlockX()), getMiddleInt(minLoc.getBlockY(), maxLoc.getBlockY()), getMiddleInt(minLoc.getBlockZ(), maxLoc.getBlockZ()));
    }
}
