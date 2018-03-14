package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * The event listener that handles chunk events
 * @author GatheringExp
 *
 */
public class TPPetsChunkListener implements Listener {
    private TPPets thisPlugin;
    
    /**
     * General constructor, saves reference to TPPets plugin
     * @param thisPlugin The TPPets plugin reference
     */
    public TPPetsChunkListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }
    
    /**
     * The handler for the ChunkUnloadEvent. It checks the chunk for pets, and logs them into the database.
     * @param e The ChunkUnloadEvent
     */
    @EventHandler (priority=EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent e) {
        for (Entity ent : e.getChunk().getEntities()) {
           if (ent instanceof Sittable && ent instanceof Tameable) {
               Tameable tameableTemp = (Tameable) ent;
               if (tameableTemp.isTamed() && thisPlugin.getDatabase() != null) {
                   thisPlugin.getDatabase().updateOrInsertPet(ent);
               }
           }
        }
    }
}
