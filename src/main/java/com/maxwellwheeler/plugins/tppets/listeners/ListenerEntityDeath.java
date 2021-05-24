package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.sql.SQLException;

/**
 * An event listener that handles pet deaths.
 * @author GatheringExp
 */
public class ListenerEntityDeath implements Listener {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ListenerEntityDeath(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * An event listener for the {@link EntityDeathEvent}. If the entity that died is a logged pet, it removes the pet
     * from the plugin, and logs its death, if enabled.
     * @param event The supplied {@link EntityDeathEvent}.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeathEvent(EntityDeathEvent event) {
        try {
            if (PetType.isPetTracked(event.getEntity()) && this.thisPlugin.getDatabase().removePet(event.getEntity().getUniqueId().toString())) {
                this.thisPlugin.getLogWrapper().logUpdatedPet("Pet " + event.getEntity().getUniqueId() + " has died. Removed from database.");
            }
        } catch (SQLException ignored) {
            this.thisPlugin.getLogWrapper().logErrors("SQL Error - removing pet after death");
        }
    }
}
