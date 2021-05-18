package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class ListenerEntityDamage implements Listener {
    private final TPPets thisPlugin;

    public ListenerEntityDamage(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (PetType.isPetTracked(event.getEntity())) {
            Tameable pet = (Tameable) event.getEntity();
            if (this.thisPlugin.getMobDamageManager().isPreventedEntityDamage(event.getDamager(), pet)) {
                this.thisPlugin.getLogWrapper().logPreventedDamage("Prevented " + event.getDamager().getType() + " from damaging " + pet.getUniqueId());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (PetType.isPetTracked(event.getEntity()) && this.thisPlugin.getMobDamageManager().isPreventedEnvironmentalDamage(event.getCause())) {
            this.thisPlugin.getLogWrapper().logPreventedDamage("Prevented " + event.getCause() + " from damaging " + event.getEntity().getUniqueId());
            event.setCancelled(true);
        }
    }

}
