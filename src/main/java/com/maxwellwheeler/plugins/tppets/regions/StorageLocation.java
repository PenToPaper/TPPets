package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;

/**
 * Represents a storage location from the database
 * @author GatheringExp
 */
public class StorageLocation {
    protected final String storageName;
    protected final String effectiveStorageName;
    protected final Location loc;

    /**
     * General constructor, takes into account all data from the storage location database tables
     * @param effectiveStorageName The storage location's effective name
     * @param storageName The storage location's name
     * @param loc The location
     */
    public StorageLocation(String storageName, String effectiveStorageName, Location loc) {
        this.storageName = storageName;
        this.effectiveStorageName = effectiveStorageName;
        this.loc = loc;
    }

    public String getEffectiveStorageName () {
        return this.effectiveStorageName;
    }

    public String getStorageName() {
        return this.storageName;
    }

    public Location getLoc() {
        return this.loc;
    }
}
