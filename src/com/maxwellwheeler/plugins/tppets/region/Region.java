package com.maxwellwheeler.plugins.tppets.region;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;

import com.maxwellwheeler.plugins.tppets.TPPets;

public abstract class Region {
    private String zoneName;
    private String worldName;
    private World world;
    private Location minLoc;
    private Location maxLoc;
    public List<Chunk> chunkList;
    protected TPPets thisPlugin;
    
    public Region(String zoneName, String worldName, int xOne, int yOne, int zOne, int xTwo, int yTwo, int zTwo) {
        this.zoneName = zoneName;
        this.worldName = worldName;
        this.world = Bukkit.getServer().getWorld(worldName);
        this.minLoc = new Location(this.world, xOne, yOne, zOne);
        this.maxLoc = new Location(this.world, xTwo, yTwo, zTwo);
        this.chunkList = initializeChunkList();
        this.thisPlugin = (TPPets) Bukkit.getServer().getPluginManager().getPlugin("TPPets");
    }
    
    public Region(String zoneName, String worldName, Location locOne, Location locTwo) {
        this.zoneName = zoneName;
        this.worldName = worldName;
        this.world = Bukkit.getServer().getWorld(worldName);
        this.minLoc = locOne;
        this.maxLoc = locTwo;
        this.chunkList = initializeChunkList();
    }
    
    public Region (String configKey, TPPets thisPlugin) {
        FileConfiguration config = thisPlugin.getConfig();
        this.zoneName = configKey.replaceAll("\\w+\\.", configKey);
        this.worldName = config.getString(configKey + ".world_name");
        this.world = Bukkit.getServer().getWorld(worldName);
        List<Integer> coordinateList = config.getIntegerList(configKey + ".coordinates");
        if (coordinateList.size() == 6) {
            this.minLoc = new Location(this.world, coordinateList.get(0), coordinateList.get(1), coordinateList.get(2));
            this.maxLoc = new Location(this.world, coordinateList.get(3), coordinateList.get(4), coordinateList.get(5));
        }
        this.chunkList = initializeChunkList();
    }
    
    protected static int nearestChunkCoord(int xOrY) {
        return xOrY/16;
    }
    
    protected void teleportPet(Location lc, Entity entity) {
        entity.teleport(lc);
        if (entity instanceof Sittable) {
            Sittable tempSittable = (Sittable) entity;
            tempSittable.setSitting(true);
        }
        TPPets plugin = (TPPets)(Bukkit.getServer().getPluginManager().getPlugin("TPPets"));
        plugin.getSQLite().updateOrInsertPet(entity);
    }
    
    protected List<Chunk> initializeChunkList() {
        List<Chunk> ret = new ArrayList<Chunk>();
        int minX = nearestChunkCoord(this.minLoc.getBlockX());
        int minZ = nearestChunkCoord(this.minLoc.getBlockZ());
        int maxX = nearestChunkCoord(this.maxLoc.getBlockX());
        int maxZ = nearestChunkCoord(this.maxLoc.getBlockZ());
        for (int i = minX; i <= maxX; i++) {
            for (int j = minZ; j <= maxZ; j++) {
                ret.add(this.world.getChunkAt(i, j));
            }
        }
        return ret;
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
    
    public List<Chunk> getChunkList() {
        return chunkList;
    }
    
    public abstract void writeToConfig(TPPets thisPlugin);
    
    public abstract String toString();
    
    protected TPPets getPlugin() {
        return thisPlugin;
    }
}
