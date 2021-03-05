package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkUnloadPetPositionUpdater implements Listener {
    private final TPPets thisPlugin;

    public ChunkUnloadPetPositionUpdater(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    @EventHandler(priority= EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (PetType.isPetTracked(entity) && this.thisPlugin.getDatabase() != null) {
                this.thisPlugin.getDatabase().updateOrInsertPet(entity);
            }
        }
    }
}
