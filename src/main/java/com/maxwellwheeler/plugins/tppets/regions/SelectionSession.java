package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Used to manage a single region cube selection session.
 * @author GatheringExp
 */
public class SelectionSession {
    /** The world the selection session is in. */
    private World world;
    /** The start location of the selection session. This does not have to be the minimum or maximum point. */
    private Location startLocation;
    /** The end location of the selection session. This does not have to be the minimum or maximum point. */
    private Location endLocation;

    /**
     * Initializes instance variables.
     * @param world The world the selection session is in.
     * @param startLocation The start location of the selection session. This does not have to be the minimum or maximum point.
     * @param endLocation The end location of the selection session. This does not have to be the minimum or maximum point.
     */
    SelectionSession(World world, Location startLocation, Location endLocation) {
        this.world = world;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
    }

    /**
     * Sets the session's world. If it is different than the current world, clears the start and end locations, as
     * selections can only be in one world.
     * @param world The world to set the session in.
     */
    public void setWorld(World world) {
        if (!world.equals(this.world)) {
            this.world = world;
            this.startLocation = null;
            this.endLocation = null;
        }
    }

    /**
     * Sets the session's start location. If it is in a different world than the session is currently in, it clears the
     * session before setting the start location.
     * @param startLocation The start location to set.
     * @see SelectionSession#setWorld(World)
     */
    public void setStartLocation(Location startLocation) {
        if (startLocation.getWorld() != null) {
            this.setWorld(startLocation.getWorld());
            this.startLocation = startLocation;
        }
    }

    /**
     * Sets the session's end location. If it is in a different world than the session is currently in, it clears the
     * session before setting the end location.
     * @param endLocation The end location to set.
     * @see SelectionSession#setWorld(World)
     */
    public void setEndLocation(Location endLocation) {
        if (endLocation.getWorld() != null) {
            this.setWorld(endLocation.getWorld());
            this.endLocation = endLocation;
        }
    }

    /**
     * Determines if the session has both a start and an end location, and a valid world.
     * @return true if the selection has a valid world and a start and end location, false if not.
     */
    public boolean isCompleteSelection() {
        return this.world != null && this.startLocation != null && this.endLocation != null;
    }

    /**
     * Gets the location with the minimum x, y, and z coordinates, between the start and end location. Returns null if
     * the selection is incomplete.
     * @return The minimum x, y, and z coordinates of the cube selection, or null if the selection is incomplete.
     */
    public Location getMinimumLocation() {
        if (this.isCompleteSelection()) {
            return new Location(this.world, Math.min(this.startLocation.getX(), this.endLocation.getX()), Math.min(this.startLocation.getY(), this.endLocation.getY()), Math.min(this.startLocation.getZ(), this.endLocation.getZ()));
        }
        return null;
    }

    /**
     * Gets the location with the maximum x, y, and z coordinates, between the start and end location. Returns null if
     * the selection is incomplete.
     * @return The maximum x, y, and z coordinates of the cube selection, or null if the selection is incomplete.
     */
    public Location getMaximumLocation() {
        if (this.isCompleteSelection()) {
            return new Location(this.world, Math.max(this.startLocation.getX(), this.endLocation.getX()), Math.max(this.startLocation.getY(), this.endLocation.getY()), Math.max(this.startLocation.getZ(), this.endLocation.getZ()));
        }
        return null;
    }

    /**
     * Gets the current world of the selection. This can be null.
     * @return The current world of the selection, or null if there is none.
     */
    public World getWorld() {
        return this.world;
    }
}
