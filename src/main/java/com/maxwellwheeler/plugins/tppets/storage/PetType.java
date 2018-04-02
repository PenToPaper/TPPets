package com.maxwellwheeler.plugins.tppets.storage;

import org.bukkit.entity.*;

import java.util.Arrays;
import java.util.Hashtable;

/**
 * Class that classifies entities by the types we care about, CAT, DOG, and PARROT
 * @author GatheringExp
 *
 */
public class PetType {
    /*
     * Enum representing a pet type.
     * CAT = ocelot
     * DOG = wolf
     * PARROT = parrot
     * UNKNOWN = a pet not of the above three types
     */
    public enum Pets {
        CAT, DOG, PARROT, HORSE, MULE, LLAMA, DONKEY, UNKNOWN
    }
    
    private static final Hashtable<Pets, Class<?>[]> classTranslate;
    
    static {
        classTranslate = new Hashtable<>();
        classTranslate.put(Pets.CAT, new Class<?>[]{org.bukkit.entity.Ocelot.class});
        classTranslate.put(Pets.DOG, new Class<?>[]{org.bukkit.entity.Wolf.class});
        classTranslate.put(Pets.PARROT, new Class<?>[]{org.bukkit.entity.Parrot.class});
        classTranslate.put(Pets.MULE, new Class<?>[]{org.bukkit.entity.Mule.class});
        classTranslate.put(Pets.LLAMA, new Class<?>[]{org.bukkit.entity.Llama.class});
        classTranslate.put(Pets.DONKEY, new Class<?>[]{org.bukkit.entity.Donkey.class});
        classTranslate.put(Pets.HORSE, new Class<?>[]{org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class});
    }

    /**
     * Gets the enum {@link Pets} based on the entity's type.
     * @param ent The entity to be checked
     * @return The enum value representing the entity's type
     */
    public static Pets getEnumByEntity(Entity ent) {
        if (ent instanceof Wolf) {
            return Pets.DOG;
        } else if (ent instanceof Ocelot) {
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
    
    /**
     * Returns a reference to the class type of the pet, primarily for the world.getEntitiesByClasses-type methods.
     * @param pt The type of pets to get the class reference of
     * @return A reference to the class
     */
    public static Class<?>[] getClassTranslate(Pets pt) {
        return classTranslate.get(pt);
    }
}
