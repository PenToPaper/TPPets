package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Hashtable;

/**
 * Used to manage multiple players' {@link SelectionSession}s.
 * @author GatheringExp
 */
public class RegionSelectionManager {
    /** A hashtable of &lt;Player Id&lt;Selection Session&gt;&gt; */
    private final Hashtable<String, SelectionSession> selectionSessions;

    /**
     * Initializes the {@link SelectionSession} table.
     */
    public RegionSelectionManager() {
        this.selectionSessions = new Hashtable<>();
    }

    /**
     * Gets the given player's stored {@link SelectionSession}, or returns null if none exists.
     * @param player The player whose {@link SelectionSession} is to be retrieved.
     * @return The player's {@link SelectionSession}, or null if none exists.
     */
    public SelectionSession getSelectionSession(Player player) {
        return this.selectionSessions.get(player.getUniqueId().toString());
    }

    /**
     * Sets the given player's {@link SelectionSession#setStartLocation(Location)}. Creates a new session if the player
     * doesn't already have one.
     * @param player The player whose {@link SelectionSession} start location is to be set.
     * @param startLocation The location to start the {@link SelectionSession} at.
     */
    public void setStartLocation(Player player, Location startLocation) {
        String playerId = player.getUniqueId().toString();
        if (!this.selectionSessions.containsKey(playerId)) {
            this.selectionSessions.put(playerId, new SelectionSession(startLocation.getWorld(), startLocation, null));
        } else {
            this.selectionSessions.get(playerId).setStartLocation(startLocation);
        }
    }

    /**
     * Sets the given player's {@link SelectionSession#setEndLocation(Location)} (Location)}. Creates a new session if the player
     * doesn't already have one.
     * @param player The player whose {@link SelectionSession} end location is to be set.
     * @param endLocation The location to end the {@link SelectionSession} at.
     */
    public void setEndLocation(Player player, Location endLocation) {
        String playerId = player.getUniqueId().toString();
        if (!this.selectionSessions.containsKey(playerId)) {
            this.selectionSessions.put(playerId, new SelectionSession(endLocation.getWorld(), null, endLocation));
        } else {
            this.selectionSessions.get(playerId).setEndLocation(endLocation);
        }
    }

    /**
     * Clears the given player's {@link SelectionSession}, removing it from the table entirely.
     * @param player The player whose {@link SelectionSession} is to be removed.
     */
    public void clearPlayerSession(Player player) {
        this.selectionSessions.remove(player.getUniqueId().toString());
    }
}
