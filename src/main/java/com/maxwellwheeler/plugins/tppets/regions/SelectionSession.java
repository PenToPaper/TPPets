package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;
import org.bukkit.World;

public class SelectionSession {
    private World world;
    private Location startLocation;
    private Location endLocation;

    SelectionSession(World world, Location startLocation, Location endLocation) {
        this.world = world;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
    }

    public void setWorld(World world) {
        if (!world.equals(this.world)) {
            this.world = world;
            this.startLocation = null;
            this.endLocation = null;
        }
    }

    public void setStartLocation(Location startLocation) {
        if (startLocation.getWorld() != null) {
            this.setWorld(startLocation.getWorld());
            this.startLocation = startLocation;
        }
    }

    public void setEndLocation(Location endLocation) {
        if (endLocation.getWorld() != null) {
            this.setWorld(endLocation.getWorld());
            this.endLocation = endLocation;
        }
    }

    public boolean isCompleteSelection() {
        return this.world != null && this.startLocation != null && this.endLocation != null;
    }

    public Location getMinimumLocation() {
        if (this.isCompleteSelection()) {
            return new Location(this.world, Math.min(this.startLocation.getX(), this.endLocation.getX()), Math.min(this.startLocation.getY(), this.endLocation.getY()), Math.min(this.startLocation.getZ(), this.endLocation.getZ()));
        }
        return null;
    }

    public Location getMaximumLocation() {
        if (this.isCompleteSelection()) {
            return new Location(this.world, Math.max(this.startLocation.getX(), this.endLocation.getX()), Math.max(this.startLocation.getY(), this.endLocation.getY()), Math.max(this.startLocation.getZ(), this.endLocation.getZ()));
        }
        return null;
    }

    public World getWorld() {
        return this.world;
    }
}
