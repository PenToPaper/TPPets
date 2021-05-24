package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.List;

/**
 * Used to determine if various mob damage types are permitted under the plugin's settings.
 * @author GatheringExp
 */
public class MobDamageManager {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;
    /** The setting for whether or not stranger damage should be prevented. */
    private boolean preventStrangerDamage = false;
    /** The setting for whether or not guest damage should be prevented. */
    private boolean preventGuestDamage = false;
    /** The setting for whether or not owner damage should be prevented. */
    private boolean preventOwnerDamage = false;
    /** The setting for whether or not environmental damage should be prevented. */
    private boolean preventEnvironmentalDamage = false;
    /** The setting for whether or not mob damage should be prevented. */
    private boolean preventMobDamage = false;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param protectPetsFrom A list of strings, corresponding to each damage type to be prevented:
     *                        OwnerDamage, GuestDamage, StrangerDamage, EnvironmentalDamage, MobDamage
     */
    public MobDamageManager(TPPets thisPlugin, List<String> protectPetsFrom) {
        this.thisPlugin = thisPlugin;
        this.initializeStateFromList(protectPetsFrom);
    }

    /**
     * Initializes damage prevention instance variables from a list of strings.
     * @param protectPetsFrom A list of strings, corresponding to each damage type to be prevented:
     *                        OwnerDamage, GuestDamage, StrangerDamage, EnvironmentalDamage, MobDamage
     */
    private void initializeStateFromList(List<String> protectPetsFrom) {
        if (protectPetsFrom.contains("OwnerDamage")) {
            this.preventOwnerDamage = true;
            this.thisPlugin.getLogWrapper().logPluginInfo("Preventing owner damage...");
        }
        if (protectPetsFrom.contains("GuestDamage")) {
            this.preventGuestDamage = true;
            this.thisPlugin.getLogWrapper().logPluginInfo("Preventing guest damage...");
        }
        if (protectPetsFrom.contains("StrangerDamage")) {
            this.preventStrangerDamage = true;
            this.thisPlugin.getLogWrapper().logPluginInfo("Preventing stranger damage...");
        }
        if (protectPetsFrom.contains("EnvironmentalDamage")) {
            this.preventEnvironmentalDamage = true;
            this.thisPlugin.getLogWrapper().logPluginInfo("Preventing environmental damage...");
        }
        if (protectPetsFrom.contains("MobDamage")) {
            this.preventMobDamage = true;
            this.thisPlugin.getLogWrapper().logPluginInfo("Preventing mob damage...");
        }
    }

    /**
     * Determines if an entity is a stranger to a given pet.
     * @param entity The potential stranger.
     * @param pet The given pet.
     * @return true if the entity is a stranger, false if not.
     */
    private boolean entityIsStranger(Entity entity, Tameable pet) {
        return !this.entityIsOwner(entity, pet) && !this.entityIsGuest(entity, pet);
    }

    /**
     * Determines if an entity is a guest to a given pet.
     * @param entity The potential guest.
     * @param pet The given pet.
     * @return true if the entity is a guest, false if not.
     */
    private boolean entityIsGuest(Entity entity, Tameable pet) {
        return this.thisPlugin.getGuestManager().isGuest(pet.getUniqueId().toString(), entity.getUniqueId().toString());
    }

    /**
     * Determines if an entity is the owner of a given pet.
     * @param entity The potential owner.
     * @param pet The given pet.
     * @return true if the entity is the owner, false if not.
     */
    private boolean entityIsOwner(Entity entity, Tameable pet) {
        return entity.equals(pet.getOwner());
    }

    /**
     * Determines if a pet can be damaged by a player.
     * @param player The player attempting to damage the pet.
     * @param pet The pet that is about to be damaged.
     * @return true if the pet can be damaged by the player, false if not.
     */
    private boolean petCanBeDamagedByPlayer(Player player, Tameable pet) {
        return player.hasPermission("tppets.bypassprotection") || (entityIsOwner(player, pet) && !this.preventOwnerDamage) || (entityIsGuest(player, pet) && !this.preventGuestDamage) || (entityIsStranger(player, pet) && !this.preventStrangerDamage);
    }

    /**
     * Determines if a pet is about to be damaged by prevented direct (non-projectile) player damage.
     * @param damager The damager entity to be evaluated. It does not have to be a player.
     * @param pet The pet that is about to be damaged. This is needed to determine the player's relationship to the pet.
     * @return true if the damager should be prevented from damaging the pet due to a player damage setting, false if not.
     */
    private boolean isPreventedDirectPlayerDamage(Entity damager, Tameable pet) {
        return damager instanceof Player && !petCanBeDamagedByPlayer((Player) damager, pet);
    }

    /**
     * Determines if a pet is about to be damaged by prevented indirect (projectile) player damage.
     * @param damager The damager entity to be evaluated. It does not have to be a player.
     * @param pet The pet that is about to be damaged. This is needed to determine the player's relationship to the pet.
     * @return true if the damager should be prevented from damaging the pet due to the a player damage setting, false if not.
     */
    private boolean isPreventedIndirectPlayerDamage(Entity damager, Tameable pet) {
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            return projectile.getShooter() instanceof Player && !petCanBeDamagedByPlayer((Player) projectile.getShooter(), pet);
        }
        return false;
    }

    /**
     * Determines if a pet is about to be damaged by prevented direct (non-projectile) mob damage.
     * @param damager The damager entity to be evaluated. It does not have to be a mob.
     * @return true if the damager should be prevented from damaging the pet due to the the mob damage setting, false if not.
     */
    private boolean isPreventedDirectMobDamage(Entity damager) {
        return damager instanceof LivingEntity && !(damager instanceof Player) && this.preventMobDamage;
    }

    /**
     * Determines if a pet is about to be damaged by prevented indirect (projectile) mob damage.
     * @param damager The damager entity to be evaluated. It does not have to be a mob.
     * @return true if the damager should be prevented from damaging the pet due to the the mob damage setting, false if not.
     */
    private boolean isPreventedIndirectMobDamage(Entity damager) {
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            return projectile.getShooter() instanceof LivingEntity && !(damager instanceof Player) && isPreventedDirectMobDamage((LivingEntity) projectile.getShooter());
        }
        return false;
    }

    /**
     * Determines if a pet is about to be damaged by prevented indirect (projectile) environmental damage. Entity
     * environmental damage is defined as any non-living entity source of damage.
     * @param damager The damager entity to be evaluated. It does not have to be a projectile.
     * @return true if the damager should be prevented from damaging the pet due to the the environmental damage setting,
     * false if not.
     */
    private boolean isPreventedIndirectEnvironmentalEntityDamage(Entity damager) {
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            return !(projectile.getShooter() instanceof LivingEntity);
        }
        return false;
    }

    /**
     * Determines if a pet is about to be damaged by prevented direct (non-projectile) environmental damage. Entity
     * environmental damage is defined as any non-living entity source of damage.
     * @param damager The damager entity to be evaluated. It does not have to be environmental.
     * @return true if the damager should be prevented from damaging the pet due to the the environmental damage setting,
     * false if not.
     */
    private boolean isPreventedDirectEnvironmentalEntityDamage(Entity damager) {
        return !(damager instanceof LivingEntity || damager instanceof Projectile);
    }

    /**
     * Determines if a pet is about to be damaged by prevented indirect (projectile) environmental damage. Entity
     * environmental damage is defined as any non-living entity source of damage.
     * @param damager The damager entity to be evaluated. It does not have to be a projectile.
     * @return true if the damager should be prevented from damaging the pet due to the the environmental damage setting,
     * false if not.
     */
    private boolean isPreventedEnvironmentalEntityDamage(Entity damager) {
        return this.preventEnvironmentalDamage && (isPreventedDirectEnvironmentalEntityDamage(damager) || isPreventedIndirectEnvironmentalEntityDamage(damager));
    }

    /**
     * Determines if a pet is about to be damaged by prevented entity damage. Checks for direct (non-projectile) and
     * indirect (projectile) damage from players, mobs, and the environment, within the contexts of this MobDamageManager's
     * configuration.
     * @param damager The damager entity to be evaluated.
     * @return true if the damager should be prevented from damaging the pet due to the the environmental damage setting,
     * false if not.
     */
    public boolean isPreventedEntityDamage(Entity damager, Tameable pet) {
        return isPreventedDirectPlayerDamage(damager, pet) || isPreventedDirectMobDamage(damager) || isPreventedIndirectPlayerDamage(damager, pet) || isPreventedIndirectMobDamage(damager) || isPreventedEnvironmentalEntityDamage(damager);
    }

    /**
     * Determines if a pet is about to be damaged by prevented environmental damage. Checks damage cause against a list
     * of safe environmental damage to protect pets from. Does not protect from 100% of all environmental damage, so as
     * to prevent pets from being unrecoverable or taxing to the server.
     * @param damageCause The damage cause from an {@link EntityDamageEvent}.
     * @return true if the damage should be prevented due to the the environmental damage setting, false if not.
     */
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
}
