package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * The event listener that handles entity events
 * @author GatheringExp
 *
 */
public class TPPetsEntityListener implements Listener {
    
    private TPPets thisPlugin;
    
    /**
     * General constructor, saves reference to TPPets plugin
     * @param thisPlugin The TPPets plugin reference
     */
    public TPPetsEntityListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }
    
    /**
     * Event handler for EntityDeathEvent. It removes it from the database if it was a previously tracked pet.
     * @param e The event
     */
    @EventHandler (priority=EventPriority.MONITOR)
    public void onEntityDeathEvent(EntityDeathEvent e) {
        if (e.getEntity() instanceof Tameable && !PetType.getEnumByEntity(e.getEntity()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed() && tameableTemp.getOwner() != null) {
                if (thisPlugin.getDatabase() != null) {
                    thisPlugin.getDatabase().deletePet(e.getEntity());
                }
            }
        }
    }
}
