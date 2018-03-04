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

public class TPPetsPlayerListener implements Listener {
    TPPets thisPlugin;
    Hashtable<String, ProtectedRegion> protRegions;
    
    public TPPetsPlayerListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
        this.protRegions = thisPlugin.getProtectedRegions();
    }
    
    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent e) {
        ProtectedRegion pr = thisPlugin.getProtectedRegionWithin(e.getTo());
        if (pr != null) {
            for (Entity ent : e.getPlayer().getNearbyEntities(10, 10, 10)) {
                if (ent instanceof Tameable && ent instanceof Sittable && pr.isInZone(ent.getLocation())) {
                    Tameable tameableTemp = (Tameable) ent;
                    if (tameableTemp.isTamed()) {
                        if (!PermissionChecker.onlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere") && pr.getWorld() != null && (!thisPlugin.getVaultEnabled() || !PermissionChecker.offlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere", pr.getWorld(), thisPlugin))) {
                            pr.tpToLostRegion(ent);
                            thisPlugin.getDatabase().updateOrInsertPet(ent);
                        }
                    }
                }
            }
        }
    }
    
    // If player right-clicks a pet they've tamed with shears while crouching, untames that entity
    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (thisPlugin.getAllowUntamingPets() && e.getHand().equals(EquipmentSlot.HAND) && isApplicableInteraction(e.getRightClicked(), e.getPlayer(), Material.SHEARS)) {
            Tameable tameableTemp = (Tameable) e.getRightClicked();
            if (tameableTemp.getOwner().equals(e.getPlayer()) || e.getPlayer().hasPermission("tppets.untameall")) {
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
    
    private boolean isApplicableInteraction(Entity ent, Player pl, Material mat) {
        return ent instanceof Sittable && ent instanceof Tameable && pl.isSneaking() && pl.getInventory().getItemInMainHand().getType().equals(mat);
    }
}
