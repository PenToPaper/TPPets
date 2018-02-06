package com.maxwellwheeler.plugins.tppets.region;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class LostAndFoundRegion extends Region {

    @Override
    public String toString() {
        return String.format("zoneName = %s; worldName = %s; x1: %d; y1: %d; z1: %d; x2: %d; y2: %d; z2: %d", getZoneName(), getZoneName(), getMinLoc().getBlockX(), getMinLoc().getBlockY(), getMinLoc().getBlockZ(), getMaxLoc().getBlockX(), getMaxLoc().getBlockY(), getMaxLoc().getBlockZ());
    }
    
    public LostAndFoundRegion(String worldName, int xOne, int yOne, int zOne, int xTwo, int yTwo,
            int zTwo) {
        super("primary", worldName, xOne, yOne, zOne, xTwo, yTwo, zTwo);
    }
    
    public LostAndFoundRegion(String worldName, Location locOne, Location locTwo) {
        super("primary", worldName, locOne, locTwo);
    }
    
    public LostAndFoundRegion(TPPets thisPlugin) {
        super("lost_and_found.primary", thisPlugin);
    }
    
    @Override
    public void writeToConfig(TPPets thisPlugin) {
        FileConfiguration config = thisPlugin.getConfig();
        config.set("lost_and_found." + getZoneName() + ".world_name", getWorldName());
        int coordinatesTemp[] = new int[]{getMinLoc().getBlockX(), getMinLoc().getBlockY(), getMinLoc().getBlockZ(), getMaxLoc().getBlockX(), getMaxLoc().getBlockY(), getMaxLoc().getBlockZ()};
        config.set("lost_and_found." + getZoneName() + ".coordinates", coordinatesTemp);
        thisPlugin.saveConfig();
    }
    
    private int getMiddleInt(int min, int max) {
        return min + ((max-min)/2);
    }
    
    public Location getApproxCenter() {
        return new Location(getMinLoc().getWorld(), getMiddleInt(getMinLoc().getBlockX(), getMaxLoc().getBlockX()), getMiddleInt(getMinLoc().getBlockY(), getMaxLoc().getBlockY()), getMiddleInt(getMinLoc().getBlockZ(), getMaxLoc().getBlockZ()));
    }
}
