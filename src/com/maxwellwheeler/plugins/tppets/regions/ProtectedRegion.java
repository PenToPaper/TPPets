package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sittable;


public class ProtectedRegion extends Region {
    private String enterMessage;
    private String lfName;
    private LostAndFoundRegion lfReference;
    
    private LostAndFoundRegion getLfReference (String lfName) {
        return thisPlugin.getLostRegion(lfName);
    }
    
    public void updateLFReference() {
        this.lfReference = getLfReference(lfName);
    }
    
    @Override
    public String toString() {
        return String.format("zoneName = %s; enterMessage = %s; worldName = %s; x1: %d; y1: %d; z1: %d; x2: %d; y2: %d; z2: %d", zoneName, enterMessage, worldName, minLoc.getBlockX(), minLoc.getBlockY(), minLoc.getBlockZ(), maxLoc.getBlockX(), maxLoc.getBlockY(), maxLoc.getBlockZ());
    }
    
    public ProtectedRegion(String zoneName, String enterMessage, String worldName, int xOne, int yOne, int zOne, int xTwo, int yTwo, int zTwo, String lfString) {
        this(zoneName, enterMessage, worldName, new Location(Bukkit.getServer().getWorld(worldName), xOne, yOne, zOne), new Location(Bukkit.getServer().getWorld(worldName), xTwo, yTwo, zTwo), lfString);
    }
    
    public ProtectedRegion(String zoneName, String enterMessage, String worldName, Location minLoc, Location maxLoc, String lfString) {
        this(zoneName, enterMessage, worldName, Bukkit.getServer().getWorld(worldName), minLoc, maxLoc, lfString);
    }
    
    public ProtectedRegion(String zoneName, String enterMessage, String worldName, World world, Location minLoc, Location maxLoc, String lfString) {
        super(zoneName, worldName, world, minLoc, maxLoc);
        this.enterMessage = ChatColor.translateAlternateColorCodes('&', enterMessage);
        this.lfReference = getLfReference(lfString);
        this.lfName = lfString;
    }
    
    public void tpToLostRegion(Entity ent) {
        if (ent instanceof Sittable) {
            ((Sittable) ent).setSitting(false);
        }
        if (lfReference != null && lfReference.getApproxCenter().getWorld() != null) {
            ent.teleport(lfReference.getApproxCenter());
            getPlugin().getLogger().info("Teleported pet with UUID " + ent.getUniqueId().toString() +  " away from " + zoneName + " to " + this.getLfReference().zoneName);
        }
    }

    public String getEnterMessage() {
        return enterMessage;
    }

    public LostAndFoundRegion getLfReference() {
        return lfReference;
    }
    
    public String getLfName() {
        return lfName;
    }
    
    public void setLfName(String lfString) {
        this.lfName = lfString;
    }
    
    public void setLfReference(String lfString) {
        this.lfReference = lfString == null ? null : getLfReference(lfString);
    }
}
