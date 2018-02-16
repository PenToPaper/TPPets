package com.maxwellwheeler.plugins.tppets.region;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sittable;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class ProtectedRegion extends Region {
    private String enterMessage;
    private LostAndFoundRegion lfReference;
    private String lfName;
    
    private LostAndFoundRegion getLFReference (String lfReference) {
        return ((TPPets)(Bukkit.getServer().getPluginManager().getPlugin("TPPets"))).getLostRegion(lfReference);
    }
    
    public void updateLFReference() {
        this.lfReference = getLFReference(lfName);
    }
    
    @Override
    public String toString() {
        return String.format("zoneName = %s; enterMessage = %s; worldName = %s; x1: %d; y1: %d; z1: %d; x2: %d; y2: %d; z2: %d", getZoneName(), enterMessage, getZoneName(), getMinLoc().getBlockX(), getMinLoc().getBlockY(), getMinLoc().getBlockZ(), getMaxLoc().getBlockX(), getMaxLoc().getBlockY(), getMaxLoc().getBlockZ());
    }
    
    public ProtectedRegion(String zoneName, String enterMessage, String worldName, int xOne, int yOne, int zOne, int xTwo, int yTwo, int zTwo, String lfString) {
        super(zoneName, worldName, xOne, yOne, zOne, xTwo, yTwo, zTwo);
        this.enterMessage = ChatColor.translateAlternateColorCodes('&', enterMessage);
        this.lfReference = getLFReference(lfString);
        this.lfName = lfString;
    }
    
    public ProtectedRegion(String zoneName, String enterMessage, String worldName, Location locOne, Location locTwo, String lfString) {
        super(zoneName, worldName, locOne, locTwo);
        this.enterMessage = ChatColor.translateAlternateColorCodes('&', enterMessage);
        this.lfReference = getLFReference(lfString);
        this.lfName = lfString;
    }
    
    public ProtectedRegion(String configKey, TPPets thisPlugin) {
        super("forbidden_zones." + configKey, thisPlugin);
    }
    
    public void tpToLostRegion(Entity ent) {
        if (ent instanceof Sittable) {
            ((Sittable) ent).setSitting(false);
        }
        if (lfReference != null) {
            ent.teleport(lfReference.getApproxCenter());
            getPlugin().getLogger().info("Teleported pet with UUID " + ent.getUniqueId().toString() +  " away from " + this.getZoneName() + " to " + this.getLfReference().getZoneName());
        }
    }
    
    @Override
    public void writeToConfig(TPPets thisPlugin) {
        FileConfiguration config = thisPlugin.getConfig();
        config.set("forbidden_zones." + getZoneName() + ".enter_message", enterMessage);
        config.set("forbidden_zones." + getZoneName() + ".world_name", getWorldName());
        int coordinatesTemp[] = new int[]{getMinLoc().getBlockX(), getMinLoc().getBlockY(), getMinLoc().getBlockZ(), getMaxLoc().getBlockX(), getMaxLoc().getBlockY(), getMaxLoc().getBlockZ()};
        config.set("forbidden_zones." + getZoneName() + ".coordinates", coordinatesTemp);
        thisPlugin.saveConfig();
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
    
    public void setLfReference(String lfString) {
        this.lfName = lfString;
        this.lfReference = lfString == null ? null : getLFReference(lfString);
    }
}
