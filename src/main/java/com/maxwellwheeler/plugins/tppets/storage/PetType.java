package com.maxwellwheeler.plugins.tppets.storage;

import org.bukkit.entity.*;

import java.util.Arrays;

/**
 * Class that classifies entities by the types we care about, CAT, DOG, and PARROT
 * @author GatheringExp
 *
 */
public class PetType {
    /*
     * Enum representing a pet type.
     * CAT = cat
     * DOG = wolf
     * PARROT = parrot
     * UNKNOWN = a pet not of the above three types
     */
    public enum Pets {
        CAT, DOG, PARROT, HORSE, MULE, LLAMA, DONKEY, UNKNOWN
    }

    /**
     * Gets the enum {@link Pets} based on the entity's type.
     * @param ent The entity to be checked
     * @return The enum value representing the entity's type
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

    public static boolean isPetTypeTracked(Entity entity) {
        return !PetType.getEnumByEntity(entity).equals(PetType.Pets.UNKNOWN);
    }

    public static boolean isPetTracked(Entity entity) {
        if (isPetTypeTracked(entity)) {
            Tameable tameableTemp = (Tameable) entity;
            return tameableTemp.isTamed() && tameableTemp.getOwner() != null;
        }
        return false;
    }

    /**
     * Translates the enum to an integer for the database
     */
    private static final Pets[] indexTranslation = new Pets[] {Pets.UNKNOWN, Pets.CAT, Pets.DOG, Pets.PARROT, Pets.MULE, Pets.LLAMA, Pets.DONKEY, Pets.HORSE};

    /**
     * Gets a numeric index based on the pet type, used in the database storage of the pet. While MySQL supports enums directly, SQLite does not, so this plugin uses integers to store this data.
     * @param pt The type of the pet
     * @return An integer representing the enum passed as pt
     */
    public static int getIndexFromPet(Pets pt) {
        return Arrays.asList(indexTranslation).indexOf(pt);
    }

    /**
     * Gets the {@link Pets} enum value from the integer, used when pulling pets from database storage.
     * @param i The index of the pet, consistent with the getIndexFromPet(Pets pt) method.
     * @return The pet's type
     */
    public static Pets getPetFromIndex(int i) {
        return indexTranslation[i];
    }
}