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

/**
 * An event listener that listens for players attempting to examine the owner and name of a pet.
 * @author GatheringExp
 */
public class ListenerPlayerInteractPetExamine implements Listener {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ListenerPlayerInteractPetExamine(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Generic handler of the actual examine pet action. Determines the owner and name of the entity, and reports any
     * success to the user. Event status is returned for the calling function to handle.
     * @param event The supplied {@link PlayerInteractEntityEvent}.
     * @return A {@link EventStatus} representing the result of the operation. Possible return values: {@link EventStatus#NO_OWNER},
     * {@link EventStatus#DB_FAIL}, {@link EventStatus#SUCCESS}.
     */
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

    /**
     * Displays the status of a {@link ListenerPlayerInteractPetExamine#onPlayerExaminePet(PlayerInteractEntityEvent)}
     * to the player.
     * @param examiningPlayer The player examining the pet.
     * @param eventStatus The event status to display.
     */
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

    /**
     * Determines if the generic {@link PlayerInteractEntityEvent} is a player crouching, right-clicking with the main
     * hand, and with a valid get_type tool.
     * @param event The {@link PlayerInteractEntityEvent} to evaluate.
     * @return true if it is a pet examine event, false if not.
     */
    private boolean isPetExamineEvent(PlayerInteractEntityEvent event) {
        return event.getHand().equals(EquipmentSlot.HAND) && event.getPlayer().isSneaking() && this.thisPlugin.getToolsManager().isMaterialValidTool("get_owner", event.getPlayer().getInventory().getItemInMainHand().getType());
    }

    /**
     * An event listener for the PlayerInteractEntityEvent. It determines if the event is a pet examine event, examines
     * the pet, and reports the results to the user.
     * @param event The supplied {@link PlayerInteractEntityEvent}.
     */
    @EventHandler(priority= EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (isPetExamineEvent(event)) {
            EventStatus status = onPlayerExaminePet(event);
            displayCommandStatus(event.getPlayer(), status);
        }
    }
}
