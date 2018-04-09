package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;

public class StorageLocation {
    private String playerUUID;
    private String storageName;
    private Location loc;

    public StorageLocation(String playerUUID, String storageName, Location loc) {
        this.playerUUID = playerUUID;
        this.storageName = storageName;
        this.loc = loc;
    }

    public String getPlayerUUID () {
        return playerUUID;
    }

    public String getStorageName() {
        return storageName;
    }

    public Location getLoc() {
        return loc;
    }
}
