package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.sql.SQLException;

public class ListenerPetPosition implements Listener {
    private final TPPets thisPlugin;

    public ListenerPetPosition(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    // Doesn't release pets that the player currently owns, as that would be mean
    @EventHandler(priority= EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            try {
                this.thisPlugin.getDatabase().insertOrUpdatePetLocation(entity);
            } catch (SQLException ignored) {
                this.thisPlugin.getLogWrapper().logErrors("SQL Error - updating pet location");
            }
        }
    }
}
