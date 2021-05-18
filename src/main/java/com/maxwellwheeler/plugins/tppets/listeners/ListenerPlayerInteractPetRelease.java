package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.sql.SQLException;

import static com.maxwellwheeler.plugins.tppets.listeners.EventStatus.*;

public class ListenerPlayerInteractPetRelease implements Listener {
    private final TPPets thisPlugin;

    public ListenerPlayerInteractPetRelease(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    private EventStatus onPlayerReleasePet(PlayerInteractEntityEvent event) {
        try {
            if (!this.thisPlugin.getAllowUntamingPets() && !event.getPlayer().hasPermission("tppets.releaseother")) {
                return NOT_ENABLED;
            }

            if (!PetType.isPetTracked(event.getRightClicked())) {
                return NO_OWNER;
            }

            Tameable pet = (Tameable) event.getRightClicked();

            if (!doesPlayerHavePermissionToRelease(event.getPlayer(), pet)) {
                return INSUFFICIENT_PERMISSIONS;
            }

            if (!this.thisPlugin.getDatabase().removePet(pet.getUniqueId().toString())) {
                return DB_FAIL;
            }

            releasePetEntity(pet);
            event.setCancelled(true);
            this.thisPlugin.getLogWrapper().logSuccessfulAction(event.getPlayer().getName() + " - release tool - " + event.getRightClicked().getUniqueId());
            return SUCCESS;

        } catch (SQLException exception) {
            return DB_FAIL;
        }
    }

    private void displayEventStatus(Player examiningPlayer, EventStatus eventStatus) {
        switch(eventStatus) {
            case SUCCESS:
                examiningPlayer.sendMessage(ChatColor.BLUE + "Pet released!");
                break;
            case NO_OWNER:
                examiningPlayer.sendMessage(ChatColor.RED + "This pet doesn't have an owner");
                break;
            case DB_FAIL:
                examiningPlayer.sendMessage(ChatColor.RED + "Could not release pet");
                break;
            case INSUFFICIENT_PERMISSIONS:
                examiningPlayer.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NOT_ENABLED:
                examiningPlayer.sendMessage(ChatColor.RED + "You can't release pets");
                break;
            default:
                examiningPlayer.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }

    private void logEventStatus(Player examiningPlayer, EventStatus eventStatus) {
        if (eventStatus != EventStatus.SUCCESS) {
            this.thisPlugin.getLogWrapper().logUnsuccessfulAction(examiningPlayer.getName() + " - release tool - " + eventStatus.toString());
        }
    }

    private void releasePetEntity(Tameable pet) {
        EntityActions.setStanding(pet);
        pet.setOwner(null);
        if (!(pet instanceof SkeletonHorse || pet instanceof ZombieHorse)) {
            pet.setTamed(false);
        }
    }

    private boolean isPetReleaseEvent(PlayerInteractEntityEvent event) {
        return event.getHand().equals(EquipmentSlot.HAND) && event.getPlayer().isSneaking() && this.thisPlugin.getToolsManager().isMaterialValidTool("release_pets", event.getPlayer().getInventory().getItemInMainHand().getType());
    }

    private boolean doesPlayerHavePermissionToRelease(Player releasingPlayer, Tameable pet) {
        return releasingPlayer.equals(pet.getOwner()) || releasingPlayer.hasPermission("tppets.releaseother");
    }

    @EventHandler(priority= EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (isPetReleaseEvent(event)) {
            EventStatus status = onPlayerReleasePet(event);
            displayEventStatus(event.getPlayer(), status);
            logEventStatus(event.getPlayer(), status);
        }
    }
}
