package com.maxwellwheeler.plugins.tppets.listeners;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.permissions.Permissible;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;

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
}
