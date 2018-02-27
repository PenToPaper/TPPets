package com.maxwellwheeler.plugins.tppets.listeners;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.permissions.Permissible;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.PetType;

import net.md_5.bungee.api.ChatColor;

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
        for (String key : protRegions.keySet()) {
            ProtectedRegion pr = protRegions.get(key);
            if (pr.isInZone(e.getTo())) {
                for (Entity ent : e.getPlayer().getNearbyEntities(10, 10, 10)) {
                    if (ent instanceof Tameable && ent instanceof Sittable && pr.isInZone(ent.getLocation())) {
                        Tameable tameableTemp = (Tameable) ent;
                        Sittable sittableTemp = (Sittable) ent;
                        if (tameableTemp.isTamed()) {
                            // TODO implement permissions
                            if (!onlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere") && (!thisPlugin.getVaultEnabled() || !offlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere", pr.getWorld()))) {
                                sittableTemp.setSitting(false);
                                pr.tpToLostRegion(ent);
                                thisPlugin.getSQLite().updateOrInsertPet(ent);
                            }
                        }
                    }
                }
                break;
            }
        }
    }
    
    private boolean onlineHasPerms(AnimalTamer at, String permission) {
        return (at instanceof Permissible && ((Permissible)at).hasPermission(permission));
    }
    
    private boolean offlineHasPerms(AnimalTamer at, String permission, World world) {
        // Player extends OfflinePlayer
        return (at instanceof OfflinePlayer && thisPlugin.getPerms().playerHas(world.getName(), (OfflinePlayer) at, "tppets.tpanywhere"));
    }
    
    // If player right-clicks a pet they've tamed with shears while crouching, untames that entity
    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (thisPlugin.getAllowUntamingPets() && e.getRightClicked() instanceof Sittable && e.getRightClicked() instanceof Tameable && e.getPlayer().isSneaking() && e.getHand().equals(EquipmentSlot.HAND) && e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.SHEARS)) {
            Tameable tameableTemp = (Tameable) e.getRightClicked();
            if (tameableTemp.getOwner().equals(e.getPlayer()) || e.getPlayer().hasPermission("tppets.untameall")) {
                Sittable sittableTemp = (Sittable) e.getRightClicked();
                sittableTemp.setSitting(false);
                tameableTemp.setTamed(false);
                thisPlugin.getSQLite().deletePet(e.getRightClicked().getUniqueId(), e.getPlayer().getUniqueId());
                String ownerUUIDString = e.getPlayer().getUniqueId().toString();
                String entityUUIDString = e.getRightClicked().getUniqueId().toString();
                thisPlugin.getPetIndex().removePetTamed(ownerUUIDString, entityUUIDString, PetType.getEnumByEntity(e.getRightClicked()));
                thisPlugin.getLogger().info("Player " + e.getPlayer().getName() + " untamed entity with UUID " + e.getRightClicked().getUniqueId());
                e.getPlayer().sendMessage(ChatColor.BLUE + "Un-taming pet.");
            }
        } else if (e.getRightClicked() instanceof Sittable && e.getRightClicked() instanceof Tameable && e.getPlayer().isSneaking() && e.getHand().equals(EquipmentSlot.HAND) && e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.BONE)) {
            Tameable tameableTemp = (Tameable) e.getRightClicked();
            if (tameableTemp.getOwner() != null) {
                e.getPlayer().sendMessage(ChatColor.BLUE + "This pet belongs to " + ChatColor.WHITE + tameableTemp.getOwner().getName() + ".");
            } else {
                e.getPlayer().sendMessage(ChatColor.BLUE + "This pet does not belong to anybody.");
            }
        }
    }
}
