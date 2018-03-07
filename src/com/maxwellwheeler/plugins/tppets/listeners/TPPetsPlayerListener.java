package com.maxwellwheeler.plugins.tppets.listeners;

import org.bukkit.Material;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.PetType;

import java.util.Hashtable;

/**
 * The event listener that handles player events
 * @author GatheringExp
 *
 */
public class TPPetsPlayerListener implements Listener {
    TPPets thisPlugin;
    Hashtable<String, ProtectedRegion> protRegions;
    
    /**
     * General constructor, saves reference to TPPets plugin
     * @param thisPlugin The TPPets plugin reference
     */
    public TPPetsPlayerListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
        this.protRegions = thisPlugin.getProtectedRegions();
    }
    
    /**
     * Event handler for PlayerMoveEvent. It checks if the player is within a {@link ProtectedRegion}. If they are, it removes pets that shouldn't be there that are within their vicinity.
     * @param e
     */
    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent e) {
        ProtectedRegion pr = thisPlugin.getProtectedRegionWithin(e.getTo());
        if (pr != null) {
            for (Entity ent : e.getPlayer().getNearbyEntities(10, 10, 10)) {
                if (ent instanceof Tameable && ent instanceof Sittable && pr.isInZone(ent.getLocation())) {
                    Tameable tameableTemp = (Tameable) ent;
                    if (tameableTemp.isTamed()) {
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
        if (thisPlugin.getAllowUntamingPets() && e.getHand().equals(EquipmentSlot.HAND) && isApplicableInteraction(e.getRightClicked(), e.getPlayer(), Material.SHEARS)) {
            Tameable tameableTemp = (Tameable) e.getRightClicked();
            if (thisPlugin.getDatabase() != null && tameableTemp.getOwner().equals(e.getPlayer()) || e.getPlayer().hasPermission("tppets.untameall")) {
                Sittable sittableTemp = (Sittable) e.getRightClicked();
                sittableTemp.setSitting(false);
                tameableTemp.setTamed(false);
                thisPlugin.getDatabase().deletePet(e.getRightClicked());
                String ownerUUIDString = e.getPlayer().getUniqueId().toString();
                String entityUUIDString = e.getRightClicked().getUniqueId().toString();
                thisPlugin.getPetIndex().removePetTamed(ownerUUIDString, entityUUIDString, PetType.getEnumByEntity(e.getRightClicked()));
                thisPlugin.getLogger().info("Player " + e.getPlayer().getName() + " untamed entity with UUID " + e.getRightClicked().getUniqueId());
                e.getPlayer().sendMessage(ChatColor.BLUE + "Un-tamed pet.");
            }
        } else if (e.getHand().equals(EquipmentSlot.HAND) && isApplicableInteraction(e.getRightClicked(), e.getPlayer(), Material.BONE)) {
            Tameable tameableTemp = (Tameable) e.getRightClicked();
            if (tameableTemp.getOwner() != null) {
                e.getPlayer().sendMessage(ChatColor.BLUE + "This pet belongs to " + ChatColor.WHITE + tameableTemp.getOwner().getName() + ".");
            } else {
                e.getPlayer().sendMessage(ChatColor.BLUE + "This pet does not belong to anybody.");
            }
        }
    }
    
    /**
     * Checks if the interaction is applicable based on certain parameters.
     * @param ent The entity that was interacted with.
     * @param pl The player that did the interacting.
     * @param mat The material the player is expected to be holding.
     * @return if the entity is of the correct type, the player is sneaking, and the player is holding the right item
     */
    private boolean isApplicableInteraction(Entity ent, Player pl, Material mat) {
        return ent instanceof Sittable && ent instanceof Tameable && pl.isSneaking() && pl.getInventory().getItemInMainHand().getType().equals(mat);
    }
}
