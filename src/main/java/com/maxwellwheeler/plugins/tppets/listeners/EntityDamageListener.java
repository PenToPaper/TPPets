package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

// Can re-implement logging of damage prevented if people want it, but seems excessive for now
public class EntityDamageListener implements Listener {
    private final TPPets thisPlugin;

    public EntityDamageListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (PetType.isPetTracked(event.getEntity())) {
            Tameable pet = (Tameable) event.getEntity();
            if (this.thisPlugin.getMobDamageManager().isPreventedEntityDamage(event.getDamager(), pet)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler (priority=EventPriority.LOW)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (PetType.isPetTracked(event.getEntity()) && this.thisPlugin.getMobDamageManager().isPreventedEnvironmentalDamage(event.getCause())) {
            event.setCancelled(true);
        }
    }

}
