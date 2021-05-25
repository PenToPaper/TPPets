package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;

/**
 * Represents a shared storage location for the server.
 * @author GatheringExp
 */
public class ServerStorageLocation extends StorageLocation {

    /**
     * Initializes instance variables.
     * @param storageName The storage location's name.
     * @param effectiveStorageName The storage location's effective name.
     * @param loc The location of the storage location.
     */
    public ServerStorageLocation(String storageName, String effectiveStorageName, Location loc) {
        super(storageName, effectiveStorageName, loc);
    }
}
