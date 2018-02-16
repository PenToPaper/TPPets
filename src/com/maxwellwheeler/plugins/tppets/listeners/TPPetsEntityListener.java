package com.maxwellwheeler.plugins.tppets.listeners;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class TPPetsEntityListener implements Listener {
    
    private TPPets thisPlugin;
    
    public TPPetsEntityListener(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }
    
    // When an entity tries to teleport, determine if it is a tameable and sittable entity, and if it is, check if it can teleport there
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityTeleportEvent(EntityTeleportEvent e) {
        if (e.getEntity() instanceof Sittable && e.getEntity() instanceof Tameable) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed() && tameableTemp.getOwner() instanceof Player) {
                Player playerTemp = (Player) tameableTemp.getOwner();
                if (thisPlugin.isInProtectedRegion(e.getTo()) && !playerTemp.hasPermission("tppets.tpanywhere")) {
                    Sittable sittableTemp = (Sittable) e.getEntity();
                    sittableTemp.setSitting(true);
                    e.setCancelled(true);
                    thisPlugin.getLogger().info("Prevented entity with UUID " + e.getEntity().getUniqueId().toString() +  " from entering restricted region.");
                }
            }
        }
    }
    
    // Removes entity from database if needed
    @EventHandler (priority=EventPriority.MONITOR)
    public void onEntityDeathEvent(EntityDeathEvent e) {
        if (e.getEntity() instanceof Tameable && e.getEntity() instanceof Sittable) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed()) {
                thisPlugin.getSQLite().deletePet(e.getEntity().getUniqueId(), tameableTemp.getOwner().getUniqueId());
            }
        }
    }
    
    // Deals with player-based and mob-based damage protection
    @SuppressWarnings("unlikely-arg-type")
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {
        // First three lines determine if this is an entity we care about
        if (e.getEntity() instanceof Tameable && e.getEntity() instanceof Sittable) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed()) {
                // If we're supposed to prevent player damage, prevent damage directly from players that don't own the pet, and indirectly through projectiles.
                if (thisPlugin.getPreventPlayerDamage()) {
                    // Direct damage
                    if (e.getDamager() instanceof Player && !(e.getDamager().equals(tameableTemp.getOwner())) && !((Player)e.getDamager()).hasPermission("tppets.bypassprotection")) {
                        e.setCancelled(true);
                        thisPlugin.getLogger().info("Prevented player damage to pet with UUID " + e.getEntity().getUniqueId().toString() +  ".");
                        return;
                    // Indirect damage
                    } else if (e.getDamager() instanceof Projectile) {
                        Projectile projTemp = (Projectile) e.getDamager();
                        if (projTemp.getShooter() instanceof Player && !projTemp.getShooter().equals(tameableTemp.getOwner()) && !((Player)e.getDamager()).hasPermission("tppets.bypassprotection")) {
                            e.setCancelled(true);
                            thisPlugin.getLogger().info("Prevented player damage to pet with UUID " + e.getEntity().getUniqueId().toString() +  ".");
                            return;
                        }
                    }
                }
                // If we're supposed to prevent mob damage, prevent damage directly from mobs and indirectly through any mob-based projectiles
                if (thisPlugin.getPreventMobDamage()) {
                    // Direct damage
                    if (e.getDamager() instanceof LivingEntity) {
                        e.setCancelled(true);
                        thisPlugin.getLogger().info("Prevented mob damage to pet with UUID " + e.getEntity().getUniqueId().toString() +  ".");
                        return;
                    // Indirect damage
                    } else if (e.getDamager() instanceof Projectile) {
                        Projectile projTemp = (Projectile) e.getDamager();
                        if (projTemp.getShooter() instanceof LivingEntity && !projTemp.getShooter().equals(tameableTemp.getOwner())) {
                            e.setCancelled(true);
                            thisPlugin.getLogger().info("Prevented mob damage to pet with UUID " + e.getEntity().getUniqueId().toString() +  ".");
                            return;
                        }
                    }
                }
            }
        }
    }
    
    // Deals with environmental damage
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityDamageEvent(EntityDamageEvent e) {
        if ((thisPlugin.getPreventEnvironmentalDamage()) && e.getEntity() instanceof Tameable && e.getEntity() instanceof Sittable) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed()) {
                switch (e.getCause()) {
                    case BLOCK_EXPLOSION:
                    case CONTACT:
                    case CRAMMING:
                    case DRAGON_BREATH:
                    case DROWNING:
                    case FALL:
                    case FALLING_BLOCK:
                    case FIRE:
                    case FIRE_TICK:
                    case FLY_INTO_WALL:
                    case HOT_FLOOR:
                    case LAVA:
                    case LIGHTNING:
                    case MELTING:
                    case POISON:
                    case STARVATION:
                    case SUFFOCATION:
                    case THORNS:
                    case WITHER:
                        e.setCancelled(true);
                        thisPlugin.getLogger().info("Prevented environmental damage to pet with UUID " + e.getEntity().getUniqueId().toString() +  ".");
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
