package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;

public class PlayerStorageLocation extends StorageLocation {
    protected final String userId;

    /**
     * General constructor, takes into account all data from the storage location database tables
     *
     * @param storageName          The storage location's name
     * @param effectiveStorageName The storage location's effective name
     * @param loc                  The location
     */
    public PlayerStorageLocation(String userId, String storageName, String effectiveStorageName, Location loc) {
        super(storageName, effectiveStorageName, loc);
        this.userId = userId;
    }

    public String getUserId() {
        return this.userId;
    }
}
