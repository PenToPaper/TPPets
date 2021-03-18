package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeathListener implements Listener {
    // TODO: Can database ever be null?
    // TODO: Log database errors to console?
    private final TPPets thisPlugin;

    public EntityDeathListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    @EventHandler(priority= EventPriority.MONITOR)
    public void onEntityDeathEvent(EntityDeathEvent event) {
        if (PetType.isPetTracked(event.getEntity())) {
            this.thisPlugin.getDatabase().deletePet(event.getEntity());
        }
    }
}
