package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.Objects;

/**
 * An event listener that prevents strangers from accessing pets through mounting and inventory events.
 * @author GatheringExp
 */
public class ListenerPetAccess implements Listener {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ListenerPetAccess(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Determines if the {@link HumanEntity} accessing the pet has permission to do so through tppets.mountother, being
     * the owner, or being a guest.
     * @param player The player attempting to access the pet.
     * @param pet The pet being accessed.
     * @return true if the player has permission to access the pet, false if not.
     */
    private boolean doesAccessorHavePermission(HumanEntity player, Tameable pet) {
        return player.hasPermission("tppets.mountother") || Objects.equals(pet.getOwner(), player) || this.thisPlugin.getGuestManager().isGuest(pet.getUniqueId().toString(), player.getUniqueId().toString());
    }

    /**
     * Generic handler for inventory events. It determines if the inventoryHolder should be accessible to the accessor.
     * If it isn't, it sends a corresponding error message to the accessor, and logs the unsuccessful action.
     * @param inventoryHolder The inventory being accessed.
     * @param accessor The player attempting to access the inventory.
     * @return true if the access was prevented, false if not.
     */
    private boolean handleInventoryAccess(InventoryHolder inventoryHolder, HumanEntity accessor) {
        if (inventoryHolder instanceof Tameable) {
            Tameable pet = (Tameable) inventoryHolder;

            if (PetType.isPetTracked(pet) && !doesAccessorHavePermission(accessor, pet)) {
                accessor.sendMessage(ChatColor.RED + "You don't have permission to do that");
                this.thisPlugin.getLogWrapper().logUnsuccessfulAction(accessor.getName() + " - inventory - INSUFFICIENT_PERMISSIONS");
                return true;
            }
        }
        return false;
    }

    /**
     * An event listener for the InventoryClickEvent. This governs when inventory items are clicked after the inventory
     * is already opened. It forwards the event to {@link ListenerPetAccess#handleInventoryAccess(InventoryHolder, HumanEntity)},
     * which determines if the inventory should be accessible to the accessor. If it isn't, this event handler cancels it.
     * @param event The supplied {@link InventoryClickEvent}.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.isCancelled() && handleInventoryAccess(event.getInventory().getHolder(), event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    /**
     * An event listener for the InventoryOpenEvent. This governs the actual opening of the inventory. It forwards the
     * event to {@link ListenerPetAccess#handleInventoryAccess(InventoryHolder, HumanEntity)}, which determines if the
     * inventory should be accessible to the accessor. If it isn't, this event handler cancels it.
     * @param event The supplied {@link InventoryOpenEvent}.
     */
    @EventHandler (priority = EventPriority.LOW)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!event.isCancelled() && handleInventoryAccess(event.getInventory().getHolder(), event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * An event listener for the EntityMountEvent. It determines if the pet is protected, and if so, determines if the
     * player has permission to mount it. If the player does not have permission, it cancels the mount and sends an error
     * message to the mounting player.
     * @param event The supplied {@link EntityMountEvent}.
     */
    @EventHandler (priority = EventPriority.LOW)
    public void entityMountProtect(EntityMountEvent event) {
        if (!event.isCancelled() && event.getEntity() instanceof Player && PetType.isPetTracked(event.getMount()) && !doesAccessorHavePermission((Player) event.getEntity(), (Tameable) event.getMount())) {
            event.setCancelled(true);
            event.getEntity().sendMessage(ChatColor.RED + "You don't have permission to do that");
            this.thisPlugin.getLogWrapper().logUnsuccessfulAction(event.getEntity().getName() + " - mount - INSUFFICIENT_PERMISSIONS");
        }
    }
}
