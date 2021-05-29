package com.maxwellwheeler.plugins.tppets.storage;

import org.bukkit.entity.*;

import java.util.Arrays;

/**
 * Used to represent a {@link com.maxwellwheeler.plugins.tppets.TPPets} pet type.
 * @author GatheringExp
 */
public class PetType {
    /**
     * Representing all pets that {@link com.maxwellwheeler.plugins.tppets.TPPets} tracks, and an {@link #UNKNOWN}
     * value representing a pet that is not tracked on by the plugin.
     */
    public enum Pets {
        CAT, DOG, PARROT, HORSE, MULE, LLAMA, DONKEY, UNKNOWN
    }

    /**
     * Gets the corresponding {@link Pets} value based on an entity's type.
     * @param ent The entity to be checked.
     * @return The {@link Pets} value representing the entity's type, or {@link Pets#UNKNOWN} if not tracked by {@link com.maxwellwheeler.plugins.tppets.TPPets}.
     */
    public static Pets getEnumByEntity(Entity ent) {
        if (ent instanceof Wolf) {
            return Pets.DOG;
        } else if (ent instanceof Cat) {
            return Pets.CAT;
        } else if (ent instanceof Parrot) {
            return Pets.PARROT;
        } else if (ent instanceof Mule) {
            return Pets.MULE;
        } else if (ent instanceof Llama) {
            return Pets.LLAMA;
        } else if (ent instanceof Donkey) {
            return Pets.DONKEY;
        } else if (ent instanceof Horse || ent instanceof ZombieHorse || ent instanceof SkeletonHorse) {
            return Pets.HORSE;
        } else {
            return Pets.UNKNOWN;
        }
    }

    /**
     * Determines if an entity is of a type that is tracked by {@link com.maxwellwheeler.plugins.tppets.TPPets}.
     * @param entity The entity.
     * @return true if the pet type is tracked by {@link com.maxwellwheeler.plugins.tppets.TPPets}, false if not.
     */
    public static boolean isPetTypeTracked(Entity entity) {
        return !PetType.getEnumByEntity(entity).equals(PetType.Pets.UNKNOWN);
    }

    /**
     * Determines if an entity is of a type that is tracked by {@link com.maxwellwheeler.plugins.tppets.TPPets}, and is
     * currently tamed by a real owner.
     * @param entity The entity.
     * @return true if the pet is tracked by {@link com.maxwellwheeler.plugins.tppets.TPPets}, false if not.
     */
    public static boolean isPetTracked(Entity entity) {
        if (isPetTypeTracked(entity)) {
            Tameable tameableTemp = (Tameable) entity;
            return tameableTemp.isTamed() && tameableTemp.getOwner() != null;
        }
        return false;
    }

    /**
     * Translates {@link Pets} to an integer for the database.
     */
    private static final Pets[] indexTranslation = new Pets[] {Pets.UNKNOWN, Pets.CAT, Pets.DOG, Pets.PARROT, Pets.MULE, Pets.LLAMA, Pets.DONKEY, Pets.HORSE};

    /**
     * Translates {@link Pets} to an integer for the database.
     * @param petType The pet's type.
     * @return The integer representing the pet type in the database.
     */
    public static int getIndexFromPet(Pets petType) {
        return Arrays.asList(indexTranslation).indexOf(petType);
    }

    /**
     * Translates a integer to a {@link Pets} type.
     * @param index The integer.
     * @return The pet's type.
     * @see #getIndexFromPet(Pets)
     */
    public static Pets getPetFromIndex(int index) {
        return indexTranslation[index];
    }
}