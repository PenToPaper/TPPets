package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Hashtable;

public class RegionSelectionManager {
    private final Hashtable<String, SelectionSession> selectionSessions = new Hashtable<>();

    public RegionSelectionManager() {}

    public SelectionSession getSelectionSession(Player player) {
        return this.selectionSessions.get(player.getUniqueId().toString());
    }

    public void setStartLocation(Player player, Location startLocation) {
        String playerId = player.getUniqueId().toString();
        if (!this.selectionSessions.containsKey(playerId)) {
            this.selectionSessions.put(playerId, new SelectionSession(startLocation.getWorld(), startLocation, null));
        } else {
            this.selectionSessions.get(playerId).setStartLocation(startLocation);
        }
    }

    public void setEndLocation(Player player, Location endLocation) {
        String playerId = player.getUniqueId().toString();
        if (!this.selectionSessions.containsKey(playerId)) {
            this.selectionSessions.put(playerId, new SelectionSession(endLocation.getWorld(), null, endLocation));
        } else {
            this.selectionSessions.get(playerId).setEndLocation(endLocation);
        }
    }

    public void clearPlayerSession(Player player) {
        this.selectionSessions.remove(player.getUniqueId().toString());
    }
}
