package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

/**
 * The event listener that handles inventory events
 * @author GatheringExp
 *
 */
public class TPPetsInventoryListener implements Listener {
    private TPPets thisPlugin;

    /**
     * General constructor, saves reference to TPPets plugin
     * @param thisPlugin The TPPets plugin reference
     */
    public TPPetsInventoryListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Responds to InventoryClickEvent. If a player is already in an inventory of a pet, make absolutely sure they're allowed to edit that before allowing it.
     * @param e The InventoryClickEvent
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Tameable && !PetType.getEnumByEntity((Entity)e.getInventory().getHolder()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getInventory().getHolder();
            Entity entTemp = (Entity) e.getInventory().getHolder();
            if (tameableTemp.isTamed() && tameableTemp.getOwner() != null && !e.getWhoClicked().hasPermission("tppets.mountother") && !e.getWhoClicked().equals(tameableTemp.getOwner()) && !thisPlugin.isAllowedToPet(entTemp.getUniqueId().toString(), e.getWhoClicked().getUniqueId().toString())) {
                e.setCancelled(true);
                e.getWhoClicked().sendMessage(ChatColor.RED + "You don't have permission to do that.");
                thisPlugin.getLogWrapper().logUnsuccessfulAction("Player with UUID " + e.getWhoClicked().getUniqueId().toString() + " was denied permission to access pet " + entTemp.getUniqueId().toString() + "'s inventory");
            }
        }
    }

    /**
     * Responds to InventoryOpenEvent. The most important of mob inventory protections are made here. It prevents players that aren't allowed to a pet from opening its inventory.
     * @param e The InventoryOpenEvent
     */
    @EventHandler (priority = EventPriority.LOW)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getInventory().getHolder() instanceof Tameable && !PetType.getEnumByEntity((Entity)e.getInventory().getHolder()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getInventory().getHolder();
            Entity entTemp = (Entity) e.getInventory().getHolder();
            if (tameableTemp.isTamed() && tameableTemp.getOwner() != null && !e.getPlayer().hasPermission("tppets.mountother") && !e.getPlayer().equals(tameableTemp.getOwner()) && !thisPlugin.isAllowedToPet(entTemp.getUniqueId().toString(), e.getPlayer().getUniqueId().toString())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to do that.");
                thisPlugin.getLogWrapper().logUnsuccessfulAction("Player with UUID " + e.getPlayer().getUniqueId().toString() + " was denied permission to access pet " + entTemp.getUniqueId().toString() + "'s inventory");
            }
        }
    }
}
