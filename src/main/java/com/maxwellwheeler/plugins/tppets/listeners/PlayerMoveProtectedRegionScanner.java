package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMoveProtectedRegionScanner implements Listener {
    private final TPPets thisPlugin;

    public PlayerMoveProtectedRegionScanner(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    private boolean canPetBeInPr(Tameable pet, ProtectedRegion protectedRegion) {
        return PermissionChecker.onlineHasPerms(pet.getOwner(), "tppets.tpanywhere") || PermissionChecker.offlineHasPerms(pet.getOwner(), "tppets.tpanywhere", protectedRegion.getWorld(), this.thisPlugin);
    }

    @EventHandler(priority= EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 1) Get any protected region the player is in
        ProtectedRegion pr = this.thisPlugin.getProtectedRegionWithin(event.getTo());

        // 2) If the player isn't in one, return without doing anything
        if (pr == null || pr.getLfReference() == null || pr.getWorld() == null) {
            return;
        }

        // 3) If they are, get entities near the player
        for (Entity entity : event.getPlayer().getNearbyEntities(10, 10, 10)) {
            // If the entity is near the player and tracked by TPPets
            if (pr.isInRegion(entity.getLocation()) && PetType.isPetTracked(entity) && !canPetBeInPr((Tameable) entity, pr) && pr.tpToLostRegion(entity)) {
                this.thisPlugin.getLogWrapper().logSuccessfulAction("Teleported pet with UUID " + entity.getUniqueId().toString() + " away from " + pr.getRegionName() + " to " + pr.getLfReference().getRegionName());
                this.thisPlugin.getDatabase().updateOrInsertPet(entity);
            }
        }
    }
}
