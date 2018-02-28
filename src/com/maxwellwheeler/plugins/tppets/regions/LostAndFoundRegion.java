package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;
import org.bukkit.World;

public class LostAndFoundRegion extends Region {

    @Override
    public String toString() {
        return String.format("zoneName = %s; worldName = %s; x1: %d; y1: %d; z1: %d; x2: %d; y2: %d; z2: %d", zoneName, worldName, minLoc.getBlockX(), minLoc.getBlockY(), minLoc.getBlockZ(), maxLoc.getBlockX(), maxLoc.getBlockY(), maxLoc.getBlockZ());
    }
    
    public LostAndFoundRegion(String zoneName, String worldName, int minX, int minY, int minZ, int maxX, int maxY,
            int maxZ) {
        super(zoneName, worldName, minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    public LostAndFoundRegion(String zoneName, String worldName, Location minLoc, Location maxLoc) {
        super(zoneName, worldName, minLoc, maxLoc);
    }
    
    public LostAndFoundRegion(String zoneName, World world, Location minLoc, Location maxLoc) {
        super(zoneName, world, minLoc, maxLoc);
    }
    
    private int getMiddleInt(int min, int max) {
        return min + ((max-min)/2);
    }
    
    public Location getApproxCenter() {
        return new Location(minLoc.getWorld(), getMiddleInt(minLoc.getBlockX(), maxLoc.getBlockX()), getMiddleInt(minLoc.getBlockY(), maxLoc.getBlockY()), getMiddleInt(minLoc.getBlockZ(), maxLoc.getBlockZ()));
    }
}
