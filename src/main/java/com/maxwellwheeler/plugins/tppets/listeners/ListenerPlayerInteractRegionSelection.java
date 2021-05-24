package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * An event listener that listens for players interacting with a block to create a region selection.
 * @author GatheringExp
 */
public class ListenerPlayerInteractRegionSelection implements Listener {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ListenerPlayerInteractRegionSelection(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Determines if a player has permission to create a region of any type.
     * @param player The player to evaluate.
     * @return true if player has either tppets.protected or tppets.lost.
     */
    private boolean hasRegionPermission(Player player) {
        return player.hasPermission("tppets.protected") || player.hasPermission("tppets.lost");
    }

    /**
     * An event listener for the PlayerInteractEvent. It determines if the event is a region selection event, creates
     * the region selection, reports successes to the user, and cancels successful events.
     * @param event The supplied {@link PlayerInteractEvent}.
     */
    @EventHandler(priority=EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!this.thisPlugin.getToolsManager().isMaterialValidTool("select_region", event.getMaterial()) || event.getClickedBlock() == null || !hasRegionPermission(event.getPlayer())) {
            return;
        }

        Location getBlockLocation = event.getClickedBlock().getLocation();
        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            this.thisPlugin.getRegionSelectionManager().setStartLocation(event.getPlayer(), getBlockLocation);
            event.getPlayer().sendMessage(ChatColor.BLUE + "First position set!" + (this.thisPlugin.getRegionSelectionManager().getSelectionSession(event.getPlayer()).isCompleteSelection() ? " Selection is complete." : ""));
            event.setCancelled(true);
        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            this.thisPlugin.getRegionSelectionManager().setEndLocation(event.getPlayer(), getBlockLocation);
            event.getPlayer().sendMessage(ChatColor.BLUE + "Second position set!" + (this.thisPlugin.getRegionSelectionManager().getSelectionSession(event.getPlayer()).isCompleteSelection() ? " Selection is complete." : ""));
            event.setCancelled(true);
        }
    }

    /**
     * An event listener for the PlayerQuitEvent. It clears any selections that the player had.
     * @param event The supplied {@link PlayerQuitEvent}.
     */
    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.thisPlugin.getRegionSelectionManager().clearPlayerSession(event.getPlayer());
    }
}
