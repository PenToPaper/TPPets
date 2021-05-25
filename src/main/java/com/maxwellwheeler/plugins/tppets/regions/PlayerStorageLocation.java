package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;

/**
 * Represents a player's personal storage location.
 * @author GatheringExp
 */
public class PlayerStorageLocation extends StorageLocation {
    /** The storage location owner's id */
    protected final String userId;

    /**
     * Initializes instance variables.
     * @param userId The storage location owner's id, represented by a trimmed string.
     * @param storageName The storage location's name.
     * @param effectiveStorageName The storage location's effective name.
     * @param loc The location of the storage location.
     */
    public PlayerStorageLocation(String userId, String storageName, String effectiveStorageName, Location loc) {
        super(storageName, effectiveStorageName, loc);
        this.userId = userId;
    }

    /**
     * Gets the storage location owner's id, represented by a trimmed string.
     * @return A trimmed representation of the storage location owner's UUID.
     */
    public String getUserId() {
        return this.userId;
    }
}
