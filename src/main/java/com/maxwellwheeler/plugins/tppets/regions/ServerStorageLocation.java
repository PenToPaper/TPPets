package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;

public class ServerStorageLocation extends StorageLocation {

    /**
     * General constructor, takes into account all data from the storage location database tables
     *
     * @param storageName          The storage location's name
     * @param effectiveStorageName The storage location's effective name
     * @param loc                  The location
     */
    public ServerStorageLocation(String storageName, String effectiveStorageName, Location loc) {
        super(storageName, effectiveStorageName, loc);
    }
}
