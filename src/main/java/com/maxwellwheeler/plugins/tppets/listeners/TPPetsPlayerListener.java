package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.helpers.ToolsChecker;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Hashtable;
import java.util.List;

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
     * Event handler for PlayerMoveEvent. It checks if the player is within a {@link ProtectedRegion}. If they are, it removes pets that shouldn't be there that are within their vicinity.
     * @param e The PlayerMoveEvent to respond to.
     */
    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent e) {
        ProtectedRegion pr = thisPlugin.getProtectedRegionWithin(e.getTo());
        if (pr != null) {
            for (Entity ent : e.getPlayer().getNearbyEntities(10, 10, 10)) {
                if (ent instanceof Tameable && !PetType.getEnumByEntity(ent).equals(PetType.Pets.UNKNOWN) && pr.isInZone(ent.getLocation())) {
                    Tameable tameableTemp = (Tameable) ent;
                    if (tameableTemp.isTamed() && tameableTemp.getOwner() != null) {
                        if (thisPlugin.getDatabase() != null && !PermissionChecker.onlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere") && pr.getWorld() != null && (!thisPlugin.getVaultEnabled() || !PermissionChecker.offlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere", pr.getWorld(), thisPlugin)) && pr.getLfReference() != null) {
                            pr.tpToLostRegion(ent);
                            thisPlugin.getDatabase().updateOrInsertPet(ent);
                        }
                    }
                }
            }
        }
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
                    if (!tameableTemp.getOwner().equals(e.getPlayer()) && !e.getPlayer().hasPermission("tppets.untameall")) {
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
