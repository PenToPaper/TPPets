package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;

/**
 * Represents a storage location.
 * @author GatheringExp
 */
public class StorageLocation {
    /** The user-formatted name of the storage location, for display. */
    protected final String storageName;
    /** The non-formatted name of the storage location, for operations. */
    protected final String effectiveStorageName;
    /** The location of the storage location. */
    protected final Location loc;

    /**
     * Initializes instance variables.
     * @param storageName The storage location's name.
     * @param effectiveStorageName The storage location's effective name.
     * @param loc The location of the storage location.
     */
    public StorageLocation(String storageName, String effectiveStorageName, Location loc) {
        this.storageName = storageName;
        this.effectiveStorageName = effectiveStorageName;
        this.loc = loc;
    }

    /**
     * Gets the non-formatted name of the storage location, for operations.
     * @return The storage name that {@link com.maxwellwheeler.plugins.tppets.TPPets} uses to operate.
     */
    public String getEffectiveStorageName () {
        return this.effectiveStorageName;
    }

    /**
     * Gets the formatted name of the storage location, for display.
     * @return The storage name that {@link com.maxwellwheeler.plugins.tppets.TPPets} uses to display.
     */
    public String getStorageName() {
        return this.storageName;
    }

    /**
     * Gets the storage location's {@link Location} on the server.
     * @return The storage location's location.
     */
    public Location getLoc() {
        return this.loc;
    }
}
