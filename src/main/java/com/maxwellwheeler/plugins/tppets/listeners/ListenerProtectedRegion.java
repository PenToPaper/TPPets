package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.sql.SQLException;
import java.util.Objects;

/**
 * An event listener that listens for pets interacting with a {@link ProtectedRegion} or {@link com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion}.
 * @author GatheringExp
 */
public class ListenerProtectedRegion implements Listener {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ListenerProtectedRegion(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Determines if the pet is allowed to be within any protected region through tppets.tpanywhere. Uses
     * {@link PermissionChecker#onlineHasPerms(AnimalTamer, String)}, and {@link PermissionChecker#offlineHasPerms(AnimalTamer, String, World, TPPets)}.
     * @param pet The pet that's attempting to be within the protected region.
     * @param world The world context where the permission is being checked. This is necessary for Vault.
     * @return true if the pet can be in any protected region, false if not.
     */
    private boolean canPetBeInPr(Tameable pet, World world) {
        return PermissionChecker.onlineHasPerms(pet.getOwner(), "tppets.tpanywhere") || PermissionChecker.offlineHasPerms(pet.getOwner(), "tppets.tpanywhere", world, this.thisPlugin);
    }

    /**
     * An event listener for the PlayerMoveEvent. It scans a 10 block cube around the player for pets that shouldn't be
     * in a {@link ProtectedRegion}. It's a heuristic to prevent pets from entering protected regions. If it teleports a pet
     * to a {@link com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion}, the pet's position is updated in the database
     * and logged through {@link ListenerProtectedRegion#thisPlugin}'s {@link com.maxwellwheeler.plugins.tppets.helpers.LogWrapper}.
     * @param event The supplied {@link PlayerMoveEvent}.
     */
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

    /**
     * An event listener for the EntityTeleportEvent. It determines if a tracked pet is teleporting into a {@link ProtectedRegion}.
     * If it is, the pet is sat, and the teleport is prevented.
     * @param event The supplied {@link EntityTeleportEvent}.
     */
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

    /**
     * An event listener for the EntityTeleportEvent. It determines if a tracked pet is teleporting out of a {@link com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion}.
     * If it is, the pet is sat, and the teleport is prevented.
     * @param event The supplied {@link EntityTeleportEvent}.
     */
    @EventHandler (priority=EventPriority.LOW)
    public void entityTeleportOutLfr(EntityTeleportEvent event) {
        if (PetType.isPetTracked(event.getEntity()) && this.thisPlugin.getLostRegionManager().getLostRegionAt(event.getFrom()) != null) {
            EntityActions.setSitting(event.getEntity());
            event.setCancelled(true);
        }
    }
}
