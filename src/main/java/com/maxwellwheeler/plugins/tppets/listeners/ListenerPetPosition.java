package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.sql.SQLException;

/**
 * An event listener registers updated pet positions in the database.
 * @author GatheringExp
 */
public class ListenerPetPosition implements Listener {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ListenerPetPosition(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * An event listener for the ChunkUnloadEvent. Checks the unloading chunk for pets that TPPets tracks, then updates
     * or inserts pets in the database. TPPets doesn't release pets that the player currently owns, even if it's over the
     * limit. That would be mean.
     * @param event The supplied {@link ChunkUnloadEvent}.
     */
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
