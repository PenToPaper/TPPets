package com.maxwellwheeler.plugins.tppets.region;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.TPPets;

public abstract class Region {
    private String zoneName;
    private String worldName;
    private World world;
    private Location minLoc;
    private Location maxLoc;
    
    public static World getWorldFromWorldName(String worldName) {
        for (World wld : Bukkit.getServer().getWorlds()) {
            if (wld.getName().equals(worldName)) {
                return wld;
            }
        }
        return null;
    }
    
    public Region(String zoneName, String worldName, int xOne, int yOne, int zOne, int xTwo, int yTwo, int zTwo) {
        this.zoneName = zoneName;
        this.worldName = worldName;
        this.world = getWorldFromWorldName(worldName);
        this.minLoc = new Location(this.world, xOne, yOne, zOne);
        this.maxLoc = new Location(this.world, xTwo, yTwo, zTwo);
    }
    
    public Region(String zoneName, String worldName, Location locOne, Location locTwo) {
        this.zoneName = zoneName;
        this.worldName = worldName;
        this.world = getWorldFromWorldName(worldName);
        this.minLoc = locOne;
        this.maxLoc = locTwo;
    }
    
    public Region (String configKey, TPPets thisPlugin) {
        FileConfiguration config = thisPlugin.getConfig();
        this.zoneName = configKey.replaceAll("\\w+\\.", configKey);
        this.worldName = config.getString(configKey + ".world_name");
        this.world = getWorldFromWorldName(this.worldName);
        List<Integer> coordinateList = config.getIntegerList(configKey + ".coordinates");
        if (coordinateList.size() == 6) {
            this.minLoc = new Location(this.world, coordinateList.get(0), coordinateList.get(1), coordinateList.get(2));
            this.maxLoc = new Location(this.world, coordinateList.get(3), coordinateList.get(4), coordinateList.get(5));
        }
    }
    
    private boolean isBetween(int min, int middle, int max) {
        return (middle >= min && middle <= max);
    }
    
    public boolean isInZone(Player pl) {
        return isInZone(pl.getLocation());
    }
    
    public boolean isInZone(Location lc) {
        return (lc.getWorld().equals(minLoc.getWorld()) && isBetween(minLoc.getBlockX(), lc.getBlockX(), maxLoc.getBlockX()) && isBetween(minLoc.getBlockY(), lc.getBlockY(), maxLoc.getBlockY()) && isBetween(minLoc.getBlockZ(), lc.getBlockZ(), maxLoc.getBlockZ()));
    }
    
    public String getZoneName() {
        return zoneName;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public World getWorld() {
        return world;
    }
    
    public Location getMinLoc() {
        return minLoc;
    }
    
    public Location getMaxLoc() {
        return maxLoc;
    }
    
    public abstract void writeToConfig(TPPets thisPlugin);
    
    public abstract String toString();
}
