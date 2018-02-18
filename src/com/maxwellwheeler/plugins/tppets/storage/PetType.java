package com.maxwellwheeler.plugins.tppets.storage;

import java.util.Arrays;
import java.util.Hashtable;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Wolf;

public class PetType {
    public static enum Pets {
        CAT, DOG, PARROT, UNKNOWN
    }
    
    private static final Hashtable<Pets, Class<?>> classTranslate;
    
    static {
        classTranslate = new Hashtable<Pets, Class<?>>();
        classTranslate.put(Pets.CAT, org.bukkit.entity.Ocelot.class);
        classTranslate.put(Pets.DOG, org.bukkit.entity.Wolf.class);
        classTranslate.put(Pets.PARROT, org.bukkit.entity.Parrot.class);
    }
    
    public static Pets getEnumByEntity(Entity ent) {
        if (ent instanceof Wolf) {
            return Pets.DOG;
        } else if (ent instanceof Ocelot) {
            return Pets.CAT;
        } else if (ent instanceof Parrot) {
            return Pets.PARROT;
        } else {
            return Pets.UNKNOWN;
        }
    }
    
    private static final Pets[] indexTranslation = new Pets[] {Pets.UNKNOWN, Pets.CAT, Pets.DOG, Pets.PARROT};
    
    public static int getIndexFromPet(Pets pt) {
        return Arrays.asList(indexTranslation).indexOf(pt);
    }
    
    public static Pets getPetFromIndex(int i) {
        return indexTranslation[i];
    }
    
    public static Class<?> getClassTranslate(Pets pt) {
        return classTranslate.get(pt);
    }
}
