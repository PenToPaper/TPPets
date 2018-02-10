package com.maxwellwheeler.plugins.tppets.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class ProtectedRegion extends Region {
    private String enterMessage;
    private LostAndFoundRegion lfReference;
    
    private LostAndFoundRegion getLFReference (String lfReference) {
        return ((TPPets)(Bukkit.getServer().getPluginManager().getPlugin("TPPets"))).getLostRegion(lfReference);
    }
    
    @Override
    public String toString() {
        return String.format("zoneName = %s; enterMessage = %s; worldName = %s; x1: %d; y1: %d; z1: %d; x2: %d; y2: %d; z2: %d", getZoneName(), enterMessage, getZoneName(), getMinLoc().getBlockX(), getMinLoc().getBlockY(), getMinLoc().getBlockZ(), getMaxLoc().getBlockX(), getMaxLoc().getBlockY(), getMaxLoc().getBlockZ());
    }
    
    public ProtectedRegion(String zoneName, String enterMessage, String worldName, int xOne, int yOne, int zOne, int xTwo, int yTwo, int zTwo, String lfString) {
        super(zoneName, worldName, xOne, yOne, zOne, xTwo, yTwo, zTwo);
        System.out.printf("zoneName = %s, enterMessage = %s, worldName = %s, xOne = %d, yOne = %d, zOne = %d, xTwo = %d, yTwo = %d, zTwo = %d, lfString = %s", zoneName, enterMessage, worldName, xOne, yOne, zOne, xTwo, yTwo, zTwo, lfString);
        this.enterMessage = enterMessage;
        this.lfReference = getLFReference(lfString);
    }
    
    public ProtectedRegion(String zoneName, String enterMessage, String worldName, Location locOne, Location locTwo, String lfString) {
        super(zoneName, worldName, locOne, locTwo);
        this.enterMessage = enterMessage;
        this.lfReference = getLFReference(lfString);
    }
    
    public ProtectedRegion(String configKey, TPPets thisPlugin) {
        super("forbidden_zones." + configKey, thisPlugin);
    }
    
    public void tpToLostRegion(Entity ent) {
        ent.teleport(lfReference.getApproxCenter());
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
    
    public void setLfReference(String lfString) {
        this.lfReference = getLFReference(lfString);
    }
}
