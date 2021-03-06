package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.helpers.ToolsChecker;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Hashtable;
import java.util.List;
import java.util.Objects;

/**
 * The event listener that handles player events
 * @author GatheringExp
 *
 */
public class TPPetsPlayerListener implements Listener {
    private TPPets thisPlugin;
    private Hashtable<String, List<Material>> customTools;

    /**
     * General constructor, saves reference to TPPets plugin
     * @param thisPlugin The TPPets plugin reference
     * @param customTools The customTools object that represents the tools used for operations on pets
     */
    public TPPetsPlayerListener(TPPets thisPlugin, Hashtable<String, List<Material>> customTools) {
        this.thisPlugin = thisPlugin;
        this.customTools = customTools;
    }
    
    /**
     * Event handler for PlayerInteractEntityEvent.
     * If the player shift right-clicks a pet with shears, it checks if the player owns it or can untame it through the permission node, and untames it, also removing it from the database.
     * If the player shift right-clicks a pet with a bone, it tells them whether or not it is tamed. 
     * @param e The event
     */
    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (!e.isCancelled()) {
            if (thisPlugin.getAllowUntamingPets() && e.getHand().equals(EquipmentSlot.HAND) && isApplicableInteraction(e.getRightClicked(), e.getPlayer(), "untame_pets")) {
                Tameable tameableTemp = (Tameable) e.getRightClicked();
                if (thisPlugin.getDatabase() != null && tameableTemp.isTamed() && tameableTemp.getOwner() != null) {
                    if (!tameableTemp.getOwner().equals(e.getPlayer()) && !e.getPlayer().hasPermission("tppets.untameother")) {
                        e.getPlayer().sendMessage(ChatColor.RED + "You don't have permission to do that");
                        return;
                    }
                    EntityActions.setStanding(e.getRightClicked());
                    thisPlugin.getDatabase().deletePet(e.getRightClicked());
                    tameableTemp.setTamed(false);
                    tameableTemp.setOwner(null);
                    if (e.getRightClicked() instanceof SkeletonHorse || e.getRightClicked() instanceof ZombieHorse) {
                        tameableTemp.setTamed(true);
                    }
                    thisPlugin.getLogWrapper().logSuccessfulAction("Player " + e.getPlayer().getName() + " untamed entity with UUID " + e.getRightClicked().getUniqueId().toString());
                    e.getPlayer().sendMessage(ChatColor.BLUE + "Un-tamed pet.");
                }
                e.setCancelled(true);
            } else if (e.getHand().equals(EquipmentSlot.HAND) && isApplicableInteraction(e.getRightClicked(), e.getPlayer(), "get_owner")) {
                Tameable tameableTemp = (Tameable) e.getRightClicked();
                if (!tameableTemp.isTamed() || tameableTemp.getOwner() == null) {
                    e.getPlayer().sendMessage(ChatColor.BLUE + "This pet does not belong to anybody.");
                } else {
                    List<PetStorage> psList = thisPlugin.getDatabase().getPetsFromUUIDs(e.getRightClicked().getUniqueId().toString(), tameableTemp.getOwner().getUniqueId().toString());
                    if (psList.size() == 1) {
                        e.getPlayer().sendMessage(ChatColor.BLUE + "This pet is named " + ChatColor.WHITE + psList.get(0).petName + ChatColor.BLUE + " and belongs to " + ChatColor.WHITE + tameableTemp.getOwner().getName() + ".");
                    } else {
                        e.getPlayer().sendMessage(ChatColor.RED + "Error getting pet data.");
                    }
                }
                e.setCancelled(true);
            }
        }
    }

    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (ToolsChecker.isInList(customTools, "select_region", e.getMaterial())) {
            try {
                Location getBlockLocation = Objects.requireNonNull(e.getClickedBlock()).getLocation();
                if (e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                    this.thisPlugin.getRegionSelectionManager().setStartLocation(e.getPlayer(), getBlockLocation);
                    e.getPlayer().sendMessage(ChatColor.BLUE + "First position set!" + (this.thisPlugin.getRegionSelectionManager().getSelectionSession(e.getPlayer()).isCompleteSelection() ? " Selection is complete." : ""));
                } else if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    this.thisPlugin.getRegionSelectionManager().setEndLocation(e.getPlayer(), getBlockLocation);
                    e.getPlayer().sendMessage(ChatColor.BLUE + "Second position set!" + (this.thisPlugin.getRegionSelectionManager().getSelectionSession(e.getPlayer()).isCompleteSelection() ? " Selection is complete." : ""));
                }
            } catch (NullPointerException ignored){}
        }
    }


    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent e) {
        this.thisPlugin.getRegionSelectionManager().clearPlayerSession(e.getPlayer());
    }
    
    /**
     * Checks if the interaction is applicable based on certain parameters.
     * @param ent The entity that was interacted with.
     * @param pl The player that did the interacting.
     * @param key The type of material data expected, per config options
     * @return if the entity is of the correct type, the player is sneaking, and the player is holding the right item
     */
    private boolean isApplicableInteraction(Entity ent, Player pl, String key) {
        return ent instanceof Tameable && !PetType.getEnumByEntity(ent).equals(PetType.Pets.UNKNOWN) && pl.isSneaking() && ToolsChecker.isInList(customTools, key, pl.getInventory().getItemInMainHand().getType());
    }
}
