package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;

import com.maxwellwheeler.plugins.tppets.TPPets;

public abstract class Region {
    protected String zoneName;
    protected String worldName;
    protected World world;
    protected Location minLoc;
    protected Location maxLoc;
    protected TPPets thisPlugin;
    
    // If worldName points to a non-existent world, world will be null but worldName will be what the world was.
    public Region(String zoneName, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this(zoneName, worldName, Bukkit.getServer().getWorld(worldName), new Location(Bukkit.getServer().getWorld(worldName), minX, minY, minZ), new Location(Bukkit.getServer().getWorld(worldName), maxX, maxY, maxZ));
    }
    
    public Region(String zoneName, String worldName, Location minLoc, Location maxLoc) {
        this(zoneName, worldName, Bukkit.getServer().getWorld(worldName), minLoc, maxLoc);
    }
    
    public Region(String zoneName, String worldName, World world, Location minLoc, Location maxLoc) {
        this.zoneName = zoneName;
        this.worldName = worldName;
        this.world = world;
        this.minLoc = minLoc;
        this.maxLoc = maxLoc;
        this.thisPlugin = (TPPets) Bukkit.getServer().getPluginManager().getPlugin("TPPets");
    }
    
    protected void teleportPet(Location lc, Entity entity) {
        entity.teleport(lc);
        if (entity instanceof Sittable) {
            Sittable tempSittable = (Sittable) entity;
            tempSittable.setSitting(true);
        }
        thisPlugin.getSQLite().updateOrInsertPet(entity);
    }
    
    private boolean isBetween(int min, int middle, int max) {
        return (middle >= min && middle <= max);
    }
    
    public boolean isInZone(Player pl) {
        return isInZone(pl.getLocation());
    }
    
    public boolean isInZone(Location lc) {
        return (minLoc.getWorld() != null && maxLoc.getWorld() != null && (lc.getWorld().equals(minLoc.getWorld()) && isBetween(minLoc.getBlockX(), lc.getBlockX(), maxLoc.getBlockX()) && isBetween(minLoc.getBlockY(), lc.getBlockY(), maxLoc.getBlockY()) && isBetween(minLoc.getBlockZ(), lc.getBlockZ(), maxLoc.getBlockZ())));
    }
    
    public abstract String toString();
    
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

    protected TPPets getPlugin() {
        return thisPlugin;
    }
}
