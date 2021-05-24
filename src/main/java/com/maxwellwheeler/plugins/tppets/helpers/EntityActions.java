package com.maxwellwheeler.plugins.tppets.helpers;

import org.bukkit.entity.*;

/**
 * Used to enact common actions on entities
 * @author GatheringExp
 */
public class EntityActions {
    /**
     * Sets the entity to sitting if they're of type {@link Sittable}.
     * @param entity The entity to set sitting.
     */
    public static void setSitting(Entity entity) {
        if (entity instanceof Sittable) {
            Sittable sittableTemp = (Sittable) entity;
            sittableTemp.setSitting(true);
        }
    }

    /**
     * Sets the entity to standing if they're of type {@link Sittable}.
     * @param entity The entity to set standing.
     */
    public static void setStanding(Entity entity) {
        if (entity instanceof Sittable) {
            Sittable sittableTemp = (Sittable) entity;
            sittableTemp.setSitting(false);
        }
    }

    /**
     * Releases the pet, setting the owner to null, tamed to false, and any {@link Sittable} pets to standing. Does not
     * set tamed to false if operating on a {@link SkeletonHorse} or {@link ZombieHorse}, so that they can be tamed later.
     * @param pet The pet to release.
     */
    public static void releasePetEntity(Tameable pet) {
        EntityActions.setStanding(pet);
        pet.setOwner(null);
        if (!(pet instanceof SkeletonHorse || pet instanceof ZombieHorse)) {
            pet.setTamed(false);
        }
    }
}
