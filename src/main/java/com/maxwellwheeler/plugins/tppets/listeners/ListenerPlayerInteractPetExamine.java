package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.sql.SQLException;

import static com.maxwellwheeler.plugins.tppets.listeners.EventStatus.*;

public class ListenerPlayerInteractPetExamine implements Listener {
    private final TPPets thisPlugin;

    public ListenerPlayerInteractPetExamine(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    private EventStatus onPlayerExaminePet(PlayerInteractEntityEvent event) {
        try {
            if (!PetType.isPetTracked(event.getRightClicked())) {
                return NO_OWNER;
            }

            Tameable pet = (Tameable) event.getRightClicked();

            PetStorage petStorage = this.thisPlugin.getDatabase().getSpecificPet(pet.getUniqueId().toString());

            if (petStorage == null) {
                return DB_FAIL;
            }

            event.getPlayer().sendMessage(ChatColor.BLUE + "This is " + ChatColor.WHITE + petStorage.petName + ChatColor.BLUE + " and belongs to " + ChatColor.WHITE + pet.getOwner().getName());
            event.setCancelled(true);
            return SUCCESS;

        } catch (SQLException exception) {
            return DB_FAIL;
        }
    }

    private void displayCommandStatus(Player examiningPlayer, EventStatus eventStatus) {
        switch(eventStatus) {
            case SUCCESS:
                break;
            case NO_OWNER:
                examiningPlayer.sendMessage(ChatColor.BLUE + "This pet doesn't have an owner");
                break;
            case DB_FAIL:
                examiningPlayer.sendMessage(ChatColor.RED + "Could not get pet data");
                break;
            default:
                examiningPlayer.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }

    private boolean isPetExamineEvent(PlayerInteractEntityEvent event) {
        return event.getHand().equals(EquipmentSlot.HAND) && event.getPlayer().isSneaking() && this.thisPlugin.getToolsManager().isMaterialValidTool("get_owner", event.getPlayer().getInventory().getItemInMainHand().getType());
    }

    @EventHandler(priority= EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (isPetExamineEvent(event)) {
            EventStatus status = onPlayerExaminePet(event);
            displayCommandStatus(event.getPlayer(), status);
        }
    }
}
