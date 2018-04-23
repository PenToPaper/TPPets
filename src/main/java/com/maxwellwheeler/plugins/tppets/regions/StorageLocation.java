package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;

/**
 * Represents a storage location from the database
 * @author GatheringExp
 */
public class StorageLocation {
    private String playerUUID;
    private String storageName;
    private Location loc;

    /**
     * General constructor, takes into account all data from the storage location database tables
     * @param playerUUID The storage location owner's UUID
     * @param storageName The storage location's name
     * @param loc The location
     */
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
