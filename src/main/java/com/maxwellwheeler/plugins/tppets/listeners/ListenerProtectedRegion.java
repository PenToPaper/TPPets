package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.sql.SQLException;
import java.util.Objects;

public class ListenerProtectedRegion implements Listener {
    private final TPPets thisPlugin;

    public ListenerProtectedRegion(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    private boolean canPetBeInPr(Tameable pet, World world) {
        return PermissionChecker.onlineHasPerms(pet.getOwner(), "tppets.tpanywhere") || PermissionChecker.offlineHasPerms(pet.getOwner(), "tppets.tpanywhere", world, this.thisPlugin);
    }

    @EventHandler(priority= EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        // 1) Get any protected region the player is in
        ProtectedRegion pr = this.thisPlugin.getProtectedRegionManager().getProtectedRegionAt(Objects.requireNonNull(event.getTo()));

        // 2) If the player isn't in one, return without doing anything
        if (pr == null || pr.getLfReference() == null || pr.getWorld() == null) {
            return;
        }

        // 3) If they are, get entities near the player
        for (Entity entity : event.getPlayer().getNearbyEntities(10, 10, 10)) {
            // If the entity is near the player and tracked by TPPets
            if (pr.isInRegion(entity.getLocation()) && PetType.isPetTracked(entity) && !canPetBeInPr((Tameable) entity, pr.getWorld()) && pr.tpToLostRegion(entity)) {
                try {
                    this.thisPlugin.getLogWrapper().logUpdatedPet("Pet " + entity.getUniqueId() + " teleported from " + pr.getRegionName() + " to " + pr.getLfReference().getRegionName() + ".");
                    this.thisPlugin.getDatabase().insertOrUpdatePetLocation(entity);
                } catch (SQLException ignored) {}
            }
        }
    }

    @EventHandler (priority=EventPriority.LOW)
    public void entityTeleportIntoPr(EntityTeleportEvent event) {
        if (PetType.isPetTracked(event.getEntity())) {
            ProtectedRegion protectedRegion = this.thisPlugin.getProtectedRegionManager().getProtectedRegionAt(Objects.requireNonNull(event.getTo()));
            Tameable pet = (Tameable) event.getEntity();

            // If pet is teleporting to a protected region
            if ((protectedRegion != null && protectedRegion.getWorld() != null && !canPetBeInPr(pet, protectedRegion.getWorld()))) {
                EntityActions.setSitting(event.getEntity());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler (priority=EventPriority.LOW)
    public void entityTeleportOutLfr(EntityTeleportEvent event) {
        if (PetType.isPetTracked(event.getEntity()) && this.thisPlugin.getLostRegionManager().getLostRegionAt(event.getFrom()) != null) {
            EntityActions.setSitting(event.getEntity());
            event.setCancelled(true);
        }
    }
}
