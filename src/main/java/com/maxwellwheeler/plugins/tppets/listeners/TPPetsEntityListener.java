package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.spigotmc.event.entity.EntityMountEvent;

/**
 * The event listener that handles entity events
 * @author GatheringExp
 *
 */
public class TPPetsEntityListener implements Listener {
    
    private TPPets thisPlugin;
    
    /**
     * General constructor, saves reference to TPPets plugin
     * @param thisPlugin The TPPets plugin reference
     */
    public TPPetsEntityListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }
    
    /**
     * Event handler for EntityDeathEvent. It removes it from the database if it was a previously tracked pet.
     * @param e The event
     */
    @EventHandler (priority=EventPriority.MONITOR)
    public void onEntityDeathEvent(EntityDeathEvent e) {
        if (e.getEntity() instanceof Tameable && !PetType.getEnumByEntity(e.getEntity()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed() && tameableTemp.getOwner() != null) {
                if (thisPlugin.getDatabase() != null) {
                    thisPlugin.getDatabase().deletePet(e.getEntity());
                }
            }
        }
    }

    /**
     * Event handler for EntityMountEvent. It checks if the player is allowed to mount that pet, using {@link #isAllowedToMount(Player, Entity)}, and cancels it if they aren't
     * @param e The EntityMountEvent object to respond to
     */
    @EventHandler (priority = EventPriority.LOW)
    public void onEntityMountEvent(EntityMountEvent e) {
        // e.getEntity = player
        // e.getMount = mounted mob (horse, etc)
        PetType.Pets pt = PetType.getEnumByEntity(e.getMount());
        if (!e.isCancelled() && e.getEntity() instanceof Player && e.getMount() instanceof LivingEntity && !pt.equals(PetType.Pets.UNKNOWN)) {
            Player playerTemp = (Player) e.getEntity();
            Tameable tameableTemp = (Tameable) e.getMount();
            if (tameableTemp.isTamed()) {
                if (tameableTemp.getOwner() != null) {
                    // Check if that player has permission to ride that pet
                    if (!isAllowedToMount((Player)e.getEntity(), e.getMount())) {
                        e.setCancelled(true);
                        e.getEntity().sendMessage(ChatColor.RED + "You do not have permission to ride this pet.");
                    }
                } else if (e.getMount() instanceof ZombieHorse || e.getMount() instanceof SkeletonHorse) {
                    // Check if the mount is a ZombieHorse or SkeletonHorse, in which case it is set as tamed
                    tameableTemp.setOwner(playerTemp);
                    thisPlugin.getServer().getPluginManager().callEvent(new EntityTameEvent((LivingEntity) e.getMount(), playerTemp));
                }
            }
        }
    }

    /**
     * Checks if a player is allowed to mount a given entity
     * @param pl The player attempting to mount
     * @param ent The entity the player is attempting to mount
     * @return True if they're allowed, false if they're not
     */
    private boolean isAllowedToMount(Player pl, Entity ent) {
        if (ent instanceof Tameable) {
            Tameable tameableTemp = (Tameable) ent;
            return pl.hasPermission("tppets.mountother") || (tameableTemp.isTamed() && tameableTemp.getOwner() != null && tameableTemp.getOwner().equals(pl)) || thisPlugin.isAllowedToPet(ent.getUniqueId().toString(), pl.getUniqueId().toString());
        }
        return false;
    }
}
