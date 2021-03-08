package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Objects;

public class PetInventoryProtector implements Listener {
    private final TPPets thisPlugin;

    public PetInventoryProtector(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    private boolean doesAccessorHavePermission(HumanEntity player, Tameable pet) {
        return player.hasPermission("tppets.mountother") || Objects.equals(pet.getOwner(), player) || this.thisPlugin.isAllowedToPet(pet.getUniqueId().toString(), player.getUniqueId().toString());
    }

    private boolean handleInventoryAccess(InventoryHolder inventoryHolder, HumanEntity accessor) {
        if (inventoryHolder instanceof Tameable) {
            Tameable pet = (Tameable) inventoryHolder;

            if (PetType.isPetTracked(pet) && !doesAccessorHavePermission(accessor, pet)) {
                accessor.sendMessage(ChatColor.RED + "You don't have permission to do that");
                this.thisPlugin.getLogWrapper().logUnsuccessfulAction("Player with UUID " + accessor.getUniqueId() + " was denied permission to access pet " + pet.getUniqueId());
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.isCancelled() && handleInventoryAccess(event.getInventory().getHolder(), event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!event.isCancelled() && handleInventoryAccess(event.getInventory().getHolder(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
