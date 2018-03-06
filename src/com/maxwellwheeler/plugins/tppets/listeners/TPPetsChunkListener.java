package com.maxwellwheeler.plugins.tppets.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class TPPetsChunkListener implements Listener {
    private TPPets thisPlugin;
    
    public TPPetsChunkListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }
    
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
