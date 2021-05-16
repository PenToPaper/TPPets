package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

public class MobDamageManager {
    private final TPPets thisPlugin;
    private boolean preventGuestDamage = false;
    private boolean preventStrangerDamage = false;
    private boolean preventEnvironmentalDamage = false;
    private boolean preventMobDamage = false;
    private boolean preventOwnerDamage = false;

    public MobDamageManager(TPPets thisPlugin, List<String> protectPetsFrom) {
        this.thisPlugin = thisPlugin;
        this.initializeStateFromList(protectPetsFrom);
    }

    private void initializeStateFromList(List<String> protectPetsFrom) {
        if (protectPetsFrom.contains("OwnerDamage")) {
            this.preventOwnerDamage = true;
            this.thisPlugin.getLogWrapper().logSuccessfulAction("Preventing owner damage...");
        }
        if (protectPetsFrom.contains("GuestDamage")) {
            this.preventGuestDamage = true;
            this.thisPlugin.getLogWrapper().logSuccessfulAction("Preventing guest damage...");
        }
        if (protectPetsFrom.contains("StrangerDamage")) {
            this.preventStrangerDamage = true;
            this.thisPlugin.getLogWrapper().logSuccessfulAction("Preventing stranger damage...");
        }
        if (protectPetsFrom.contains("EnvironmentalDamage")) {
            this.preventEnvironmentalDamage = true;
            this.thisPlugin.getLogWrapper().logSuccessfulAction("Preventing environmental damage...");
        }
        if (protectPetsFrom.contains("MobDamage")) {
            this.preventMobDamage = true;
            this.thisPlugin.getLogWrapper().logSuccessfulAction("Preventing mob damage...");
        }
    }

    private boolean entityIsStranger(Entity entity, Tameable pet) {
        return !this.entityIsOwner(entity, pet) && !this.entityIsGuest(entity, pet);
    }

    private boolean entityIsGuest(Entity entity, Tameable pet) {
        return this.thisPlugin.getGuestManager().isGuest(pet.getUniqueId().toString(), entity.getUniqueId().toString());
    }

    private boolean entityIsOwner(Entity entity, Tameable pet) {
        return entity.equals(pet.getOwner());
    }

    private boolean petCanBeDamagedByPlayer(Player player, Tameable pet) {
        return player.hasPermission("tppets.bypassprotection") || (entityIsOwner(player, pet) && !this.preventOwnerDamage) || (entityIsGuest(player, pet) && !this.preventGuestDamage) || (entityIsStranger(player, pet) && !this.preventStrangerDamage);
    }

    private boolean isPreventedDirectPlayerDamage(Entity damager, Tameable pet) {
        return damager instanceof Player && !petCanBeDamagedByPlayer((Player) damager, pet);
    }

    private boolean isPreventedDirectMobDamage(Entity damager) {
        return damager instanceof LivingEntity && !(damager instanceof Player) && this.preventMobDamage;
    }

    private boolean isPreventedIndirectPlayerDamage(Entity damager, Tameable pet) {
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            return projectile.getShooter() instanceof Player && !petCanBeDamagedByPlayer((Player) projectile.getShooter(), pet);
        }
        return false;
    }

    private boolean isPreventedIndirectMobDamage(Entity damager) {
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            return projectile.getShooter() instanceof LivingEntity && !(damager instanceof Player) && isPreventedDirectMobDamage((LivingEntity) projectile.getShooter());
        }
        return false;
    }

    private boolean isPreventedEnvironmentalProjectileDamage(Entity damager) {
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            return !(projectile.getShooter() instanceof LivingEntity);
        }
        return false;
    }

    private boolean isPreventedEnvironmentalEntityDamage(Entity damager) {
        return this.preventEnvironmentalDamage && (!(damager instanceof LivingEntity || damager instanceof Projectile) || isPreventedEnvironmentalProjectileDamage(damager));
    }

    public boolean isPreventedEntityDamage(Entity damager, Tameable pet) {
        return isPreventedDirectPlayerDamage(damager, pet) || isPreventedDirectMobDamage(damager) || isPreventedIndirectPlayerDamage(damager, pet) || isPreventedIndirectMobDamage(damager) || isPreventedEnvironmentalEntityDamage(damager);
    }

    public boolean isPreventedEnvironmentalDamage(EntityDamageEvent.DamageCause damageCause) {
        if (!this.preventEnvironmentalDamage) {
            return false;
        }

        switch(damageCause) {
            case BLOCK_EXPLOSION:
            case CONTACT:
            case CRAMMING:
            case CUSTOM:
            case DRAGON_BREATH:
            case DROWNING:
            case DRYOUT:
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
                return true;
            default:
                return false;
        }
    }

    public boolean getPreventGuestDamage() {
        return this.preventGuestDamage;
    }

    public boolean getPreventStrangerDamage() {
        return this.preventStrangerDamage;
    }

    public boolean getPreventEnvironmentalDamage() {
        return this.preventEnvironmentalDamage;
    }

    public boolean getPreventMobDamage() {
        return this.preventMobDamage;
    }

    public boolean getPreventOwnerDamage() {
        return this.preventOwnerDamage;
    }
}
