package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ListenerPlayerInteractRegionSelection implements Listener {
    private final TPPets thisPlugin;

    public ListenerPlayerInteractRegionSelection(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    @EventHandler(priority=EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (!this.thisPlugin.getToolsManager().isMaterialValidTool("select_region", e.getMaterial()) || e.getClickedBlock() == null) {
            return;
        }

        Location getBlockLocation = e.getClickedBlock().getLocation();
        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            this.thisPlugin.getRegionSelectionManager().setStartLocation(e.getPlayer(), getBlockLocation);
            e.getPlayer().sendMessage(ChatColor.BLUE + "First position set!" + (this.thisPlugin.getRegionSelectionManager().getSelectionSession(e.getPlayer()).isCompleteSelection() ? " Selection is complete." : ""));
        } else if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            this.thisPlugin.getRegionSelectionManager().setEndLocation(e.getPlayer(), getBlockLocation);
            e.getPlayer().sendMessage(ChatColor.BLUE + "Second position set!" + (this.thisPlugin.getRegionSelectionManager().getSelectionSession(e.getPlayer()).isCompleteSelection() ? " Selection is complete." : ""));
        }
    }

    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.thisPlugin.getRegionSelectionManager().clearPlayerSession(e.getPlayer());
    }
}
