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
import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.PetType;

import net.md_5.bungee.api.ChatColor;

import java.util.List;

public class TPPetsPlayerListener implements Listener {
    TPPets pluginInstance;
    List<ProtectedRegion> protRegions;
    
    public TPPetsPlayerListener(TPPets pluginInstance) {
        this.pluginInstance = pluginInstance;
        this.protRegions = pluginInstance.getProtectedRegions();
    }
    
    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent e) {
        for (ProtectedRegion pr : protRegions) {
            if (pr.isInZone(e.getTo())) {
                for (Entity ent : e.getPlayer().getNearbyEntities(10, 10, 10)) {
                    if (ent instanceof Tameable && ent instanceof Sittable && pr.isInZone(ent.getLocation())) {
                        Tameable tameableTemp = (Tameable) ent;
                        Sittable sittableTemp = (Sittable) ent;
                        if (tameableTemp.isTamed()) {
                            // TODO implement permissions
                            if (!onlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere") && (!pluginInstance.getVaultEnabled() || !offlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere", pr.getWorld()))) {
                                sittableTemp.setSitting(false);
                                pr.tpToLostRegion(ent);
                                pluginInstance.getSQLite().updateOrInsertPet(ent);
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
        return (at instanceof OfflinePlayer && pluginInstance.getPerms().playerHas(world.getName(), (OfflinePlayer) at, "tppets.tpanywhere"));
    }
    
    // If player right-clicks a pet they've tamed with shears while crouching, untames that entity
    @EventHandler (priority=EventPriority.LOW)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        if (pluginInstance.getAllowUntamingPets() && e.getRightClicked() instanceof Sittable && e.getRightClicked() instanceof Tameable && e.getPlayer().isSneaking() && e.getHand().equals(EquipmentSlot.HAND) && e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.SHEARS)) {
            Sittable sittableTemp = (Sittable) e.getRightClicked();
            sittableTemp.setSitting(false);
            Tameable tameableTemp = (Tameable) e.getRightClicked();
            tameableTemp.setTamed(false);
            pluginInstance.getSQLite().deletePet(e.getRightClicked().getUniqueId(), e.getPlayer().getUniqueId());
            String ownerUUIDString = e.getPlayer().getUniqueId().toString();
            String entityUUIDString = e.getRightClicked().getUniqueId().toString();
            pluginInstance.getPetIndex().removePetTamed(ownerUUIDString, entityUUIDString, PetType.getEnumByEntity(e.getRightClicked()));
            pluginInstance.getLogger().info("Player " + e.getPlayer().getName() + " untamed entity with UUID " + e.getRightClicked().getUniqueId());
            e.getPlayer().sendMessage(ChatColor.BLUE + "Un-taming pet.");
        }
    }
}
