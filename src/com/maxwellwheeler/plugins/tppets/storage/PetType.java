package com.maxwellwheeler.plugins.tppets.storage;

import java.util.Arrays;

public class PetType {
    public static enum Pets {
        CAT, DOG, PARROT, UNKNOWN
    }
    
    private static Pets[] indexTranslation = new Pets[] {Pets.UNKNOWN, Pets.CAT, Pets.DOG, Pets.PARROT};
    
    public static int getIndexFromPet(Pets pt) {
        return Arrays.asList(indexTranslation).indexOf(pt);
    }
    
    public static Pets getPetFromIndex(int i) {
        return indexTranslation[i];
    }
}
