package com.maxwellwheeler.plugins.tppets.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.PlayerPetIndex;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.List;

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
     * Event handler for EntityTeleportEvent. It checks if the entity is a pet that's teleporting into a {@link ProtectedRegion} or teleporting out of a {@link LostAndFoundRegion} and prevents that.
     * Note that entities can be teleported out of the {@link LostAndFoundRegion} with the command, just not through natural mob behavior.
     * @param e The event
     */
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityTeleportEvent(EntityTeleportEvent e) {
        if (e.getEntity() instanceof Tameable && !PetType.getEnumByEntity(e.getEntity()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed() && !PermissionChecker.onlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere") && (!thisPlugin.getVaultEnabled() || !PermissionChecker.offlineHasPerms(tameableTemp.getOwner(), "tppets.tpanywhere", e.getEntity().getLocation().getWorld(), thisPlugin))) {
                if (thisPlugin.isInProtectedRegion(e.getTo())) {
                    EntityActions.setSitting(e.getEntity());
                    e.setCancelled(true);
                    thisPlugin.getLogger().info("Prevented entity with UUID " + e.getEntity().getUniqueId().toString() +  " from entering protected region.");
                } else if (thisPlugin.isInLostRegion(e.getFrom())) {
                    // direct calls to entity.teleport() methods do not call this event, so cancelling this does not prevent players from taking their pets home
                    EntityActions.setSitting(e.getEntity());
                    e.setCancelled(true);
                }
            }
        }
    }
    
    /**
     * Event handler for EntityDeathEvent. It removes it from the database if it was a previously tracked pet.
     * @param e The event
     */
    @EventHandler (priority=EventPriority.MONITOR)
    public void onEntityDeathEvent(EntityDeathEvent e) {
        if (e.getEntity() instanceof Tameable && !PetType.getEnumByEntity(e.getEntity()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed()) {
                if (thisPlugin.getDatabase() != null) {
                    thisPlugin.getDatabase().deletePet(e.getEntity());
                }
                thisPlugin.getPetIndex().removePetTamed(e.getEntity().getUniqueId().toString(), tameableTemp.getOwner().getUniqueId().toString(), PetType.getEnumByEntity(e.getEntity()));
            }
        }
    }
    
    /**
     * Event handler for EntityDamageByEntityEvent. It prevents player damage and mob damage, even through potions and arrows.
     * @param e The event.
     */
    @SuppressWarnings("unlikely-arg-type")
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent e) {
        // First three lines determine if this is an entity we care about
        if (e.getEntity() instanceof Tameable && !PetType.getEnumByEntity(e.getEntity()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed()) {
                // If we're supposed to prevent player damage, prevent damage directly from players that don't own the pet, and indirectly through projectiles.
                if (thisPlugin.getPreventPlayerDamage()) {
                    // Direct damage
                    if (e.getDamager() instanceof Player && !(e.getDamager().equals(tameableTemp.getOwner())) && !(e.getDamager().hasPermission("tppets.bypassprotection")) && !thisPlugin.isAllowedToPet(e.getEntity().getUniqueId().toString(), e.getDamager().getUniqueId().toString())) {
                        e.setCancelled(true);
                        thisPlugin.getLogger().info("Prevented player damage to pet with UUID " + e.getEntity().getUniqueId().toString() +  ".");
                        return;
                    // Indirect damage
                    } else if (e.getDamager() instanceof Projectile) {
                        Projectile projTemp = (Projectile) e.getDamager();
                        if (projTemp.getShooter() instanceof Player && !projTemp.getShooter().equals(tameableTemp.getOwner()) && !(e.getDamager()).hasPermission("tppets.bypassprotection") && !thisPlugin.isAllowedToPet(e.getEntity().getUniqueId().toString(), ((Player) projTemp.getShooter()).getUniqueId().toString())) {
                            e.setCancelled(true);
                            thisPlugin.getLogger().info("Prevented player damage to pet with UUID " + e.getEntity().getUniqueId().toString() +  ".");
                            return;
                        }
                    }
                }
                // If we're supposed to prevent mob damage, prevent damage directly from mobs and indirectly through any mob-based projectiles
                if (thisPlugin.getPreventMobDamage()) {
                    // Direct damage
                    if (e.getDamager() instanceof LivingEntity && !(e.getDamager() instanceof Player)) {
                        e.setCancelled(true);
                        thisPlugin.getLogger().info("Prevented mob damage to pet with UUID " + e.getEntity().getUniqueId().toString() +  ".");
                        return;
                    // Indirect damage
                    } else if (e.getDamager() instanceof Projectile) {
                        Projectile projTemp = (Projectile) e.getDamager();
                        if (projTemp.getShooter() instanceof LivingEntity && !(projTemp.getShooter() instanceof Player)) {
                            e.setCancelled(true);
                            thisPlugin.getLogger().info("Prevented mob damage to pet with UUID " + e.getEntity().getUniqueId().toString() +  ".");
                            return;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Event handler for EntityDamageEvent. It prevents environmental damage.
     * @param e The event
     */
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityDamageEvent(EntityDamageEvent e) {
        if ((thisPlugin.getPreventEnvironmentalDamage()) && e.getEntity() instanceof Tameable && !PetType.getEnumByEntity(e.getEntity()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) e.getEntity();
            if (tameableTemp.isTamed()) {
                switch (e.getCause()) {
                    case BLOCK_EXPLOSION:
                    case CONTACT:
                    case CRAMMING:
                    case CUSTOM:
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
    
    /**
     * Event handler for EntityTameEvent. It checks if the tamed entity should be allowed to be tamed based on the pet limits. If it can, it adds that to the {@link PlayerPetIndex}
     * @param e The EntityTameEvent object to respond to
     */
    @EventHandler (priority=EventPriority.HIGHEST)
    public void onEntityTameEvent(EntityTameEvent e) {
        if (!e.isCancelled()) {
            PetType.Pets pt = PetType.getEnumByEntity(e.getEntity());
            PlayerPetIndex.RuleRestriction rr = thisPlugin.getPetIndex().allowTame(e.getOwner(), e.getEntity().getLocation(), pt);
            if (!rr.equals(PlayerPetIndex.RuleRestriction.ALLOWED)) {
                e.setCancelled(true);
                if (e.getOwner() instanceof Player) {
                    Player playerTemp = (Player) e.getOwner();
                    playerTemp.sendMessage(ChatColor.BLUE + "You've surpassed the " + ChatColor.WHITE + rr.toString() + ChatColor.BLUE + " taming limit!");
                }
            } else {
                thisPlugin.getDatabase().insertPet(e.getEntity(), e.getOwner().getUniqueId().toString());
                thisPlugin.getPetIndex().newPetTamed(e.getEntity());
            }
        }
    }

    @EventHandler (priority = EventPriority.LOW)
    public void onEntityMountEvent(EntityMountEvent e) {
        // e.getEntity = player
        // e.getMount = mounted mob (horse, etc)
        if (!PetType.getEnumByEntity(e.getMount()).equals(PetType.Pets.UNKNOWN)) {
            Tameable tempTameable = (Tameable) e.getMount();
            if (tempTameable.isTamed() && tempTameable.getOwner() != null && e.getEntity() instanceof Player && !isAllowedToMount((Player)e.getEntity(), e.getMount())) {
                e.setCancelled(true);
                e.getEntity().sendMessage(ChatColor.RED + "You do not have permission to ride this pet.");
            }
        }
    }

    private boolean isAllowedToMount(Player pl, Entity ent) {
        if (ent instanceof Tameable) {
            Tameable tameableTemp = (Tameable) ent;
            return pl.hasPermission("tppets.mountother") || (tameableTemp.isTamed() && tameableTemp.getOwner() != null && tameableTemp.getOwner().equals(pl)) || thisPlugin.isAllowedToPet(ent.getUniqueId().toString(), pl.getUniqueId().toString());
        }
        return false;
    }
}
