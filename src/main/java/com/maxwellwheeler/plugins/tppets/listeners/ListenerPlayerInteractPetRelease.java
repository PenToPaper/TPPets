package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
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

/**
 * An event listener that listens for players attempting to release a pet.
 * @author GatheringExp
 */
public class ListenerPlayerInteractPetRelease implements Listener {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ListenerPlayerInteractPetRelease(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Generic handler of the actual release pet action. Determines if the player has permission to release the pet, if
     * the server has releasing pets enabled, if the pet is tamed to begin with, and then removes the pet entry from the
     * database. It logs successful actions to {@link ListenerPlayerInteractPetRelease#thisPlugin}'s {@link com.maxwellwheeler.plugins.tppets.helpers.LogWrapper}
     * @param event The supplied {@link PlayerInteractEntityEvent}.
     * @return A {@link EventStatus} representing the result of the operation. Possible return values: {@link EventStatus#NOT_ENABLED},
     * {@link EventStatus#NO_OWNER}, {@link EventStatus#INSUFFICIENT_PERMISSIONS}, {@link EventStatus#DB_FAIL},
     * {@link EventStatus#SUCCESS}.
     */
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

            EntityActions.releasePetEntity(pet);
            event.setCancelled(true);
            this.thisPlugin.getLogWrapper().logSuccessfulAction(event.getPlayer().getName() + " - release tool - " + event.getRightClicked().getUniqueId());
            return SUCCESS;

        } catch (SQLException exception) {
            return DB_FAIL;
        }
    }

    /**
     * Displays the status of a {@link ListenerPlayerInteractPetRelease#onPlayerReleasePet(PlayerInteractEntityEvent)}
     * to the player.
     * @param releasingPlayer The player releasing the pet.
     * @param eventStatus The event status to display.
     */
    private void displayEventStatus(Player releasingPlayer, EventStatus eventStatus) {
        switch(eventStatus) {
            case SUCCESS:
                releasingPlayer.sendMessage(ChatColor.BLUE + "Pet released!");
                break;
            case NO_OWNER:
                releasingPlayer.sendMessage(ChatColor.RED + "This pet doesn't have an owner");
                break;
            case DB_FAIL:
                releasingPlayer.sendMessage(ChatColor.RED + "Could not release pet");
                break;
            case INSUFFICIENT_PERMISSIONS:
                releasingPlayer.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NOT_ENABLED:
                releasingPlayer.sendMessage(ChatColor.RED + "You can't release pets");
                break;
            default:
                releasingPlayer.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }

    /**
     * Logs the status of a {@link ListenerPlayerInteractPetRelease#onPlayerReleasePet(PlayerInteractEntityEvent)}
     * to {@link ListenerPlayerInteractPetRelease#thisPlugin}'s {@link com.maxwellwheeler.plugins.tppets.helpers.LogWrapper}.
     * @param releasingPlayer The player releasing the pet.
     * @param eventStatus The event status to display.
     */
    private void logEventStatus(Player releasingPlayer, EventStatus eventStatus) {
        if (eventStatus != EventStatus.SUCCESS) {
            this.thisPlugin.getLogWrapper().logUnsuccessfulAction(releasingPlayer.getName() + " - release tool - " + eventStatus.toString());
        }
    }

    /**
     * Determines if the generic {@link PlayerInteractEntityEvent} is a player crouching, right-clicking with the main
     * hand, and with a valid release_pets tool.
     * @param event The {@link PlayerInteractEntityEvent} to evaluate.
     * @return true if it is a pet release event, false if not.
     */
    private boolean isPetReleaseEvent(PlayerInteractEntityEvent event) {
        return event.getHand().equals(EquipmentSlot.HAND) && event.getPlayer().isSneaking() && this.thisPlugin.getToolsManager().isMaterialValidTool("release_pets", event.getPlayer().getInventory().getItemInMainHand().getType());
    }

    /**
     * Determines if a player has permission to release a pet. Returns true if the player is the owner, or if the player
     * has tppets.releaseother.
     * @param releasingPlayer The player releasing the pet.
     * @param pet The pet being released.
     * @return true if the player is the owner or has tppets.releaseother, false if not.
     */
    private boolean doesPlayerHavePermissionToRelease(Player releasingPlayer, Tameable pet) {
        return releasingPlayer.equals(pet.getOwner()) || releasingPlayer.hasPermission("tppets.releaseother");
    }

    /**
     * An event listener for the PlayerInteractEntityEvent. It determines if the event is a pet release event, releases
     * the pet, and reports the results to the user.
     * @param event The supplied {@link PlayerInteractEntityEvent}.
     */
    @EventHandler(priority= EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (isPetReleaseEvent(event)) {
            EventStatus status = onPlayerReleasePet(event);
            displayEventStatus(event.getPlayer(), status);
            logEventStatus(event.getPlayer(), status);
        }
    }
}
