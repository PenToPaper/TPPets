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

public class TPPetsInventoryListener implements Listener {
    private TPPets thisPlugin;

    /**
     * General constructor, saves reference to TPPets plugin
     * @param thisPlugin The TPPets plugin reference
     */
    public TPPetsInventoryListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Tameable && !PetType.getEnumByEntity((Entity)e.getInventory().getHolder()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getInventory().getHolder();
            Entity entTemp = (Entity) e.getInventory().getHolder();
            if (tameableTemp.isTamed() && tameableTemp.getOwner() != null && !e.getWhoClicked().hasPermission("tppets.mountother") && !e.getWhoClicked().equals(tameableTemp.getOwner()) && !thisPlugin.isAllowedToPet(entTemp.getUniqueId().toString(), e.getWhoClicked().getUniqueId().toString())) {
                e.setCancelled(true);
                e.getWhoClicked().sendMessage(ChatColor.RED + "You don't have permission to do that.");
                thisPlugin.getLogger().info("Player with UUID " + e.getWhoClicked().getUniqueId().toString() + " was denied permission to access pet " + entTemp.getUniqueId().toString() + "'s inventory");
            }
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getInventory().getHolder() instanceof Tameable && !PetType.getEnumByEntity((Entity)e.getInventory().getHolder()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getInventory().getHolder();
            Entity entTemp = (Entity) e.getInventory().getHolder();
            if (tameableTemp.isTamed() && tameableTemp.getOwner() != null && !e.getPlayer().hasPermission("tppets.mountother") && !e.getPlayer().equals(tameableTemp.getOwner()) && !thisPlugin.isAllowedToPet(entTemp.getUniqueId().toString(), e.getPlayer().getUniqueId().toString())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to do that.");
                thisPlugin.getLogger().info("Player with UUID " + e.getPlayer().getUniqueId().toString() + " was denied permission to access pet " + entTemp.getUniqueId().toString() + "'s inventory");
            }
        }
    }
}
