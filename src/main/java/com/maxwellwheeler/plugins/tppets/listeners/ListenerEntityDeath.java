package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.sql.SQLException;

public class ListenerEntityDeath implements Listener {
    // TODO: Log database errors to console?
    private final TPPets thisPlugin;

    public ListenerEntityDeath(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    @EventHandler(priority= EventPriority.MONITOR)
    public void onEntityDeathEvent(EntityDeathEvent event) {
        try {
            if (PetType.isPetTracked(event.getEntity())) {
                this.thisPlugin.getDatabase().removePet(event.getEntity());
            }
        } catch (SQLException ignored) {}
    }
}
