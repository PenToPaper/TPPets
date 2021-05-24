package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * An event listener that prevents damage to protected pets.
 * @author GatheringExp
 */
public class ListenerEntityDamage implements Listener {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ListenerEntityDamage(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * An event listener for the EntityDamageByEntityEvent. This uses {@link com.maxwellwheeler.plugins.tppets.helpers.MobDamageManager}
     * to determine if the damage is stranger damage, guest damage, owner damage, mob damage, or environmental damage
     * being done to a protected pet, prevents it, and logs it if enabled.
     * @param event The supplied {@link EntityDamageByEntityEvent}.
     */
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

    /**
     * An event listener for the generic EntityDamageEvent. This uses {@link com.maxwellwheeler.plugins.tppets.helpers.MobDamageManager}
     * to determine if the damage is environmental damage being done to a protected pet, prevents it, and logs it if
     * enabled.
     * @param event The supplied {@link EntityDamageEvent}.
     */
    @EventHandler (priority = EventPriority.LOW)
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (PetType.isPetTracked(event.getEntity()) && this.thisPlugin.getMobDamageManager().isPreventedEnvironmentalDamage(event.getCause())) {
            this.thisPlugin.getLogWrapper().logPreventedDamage("Prevented " + event.getCause() + " from damaging " + event.getEntity().getUniqueId());
            event.setCancelled(true);
        }
    }

}
