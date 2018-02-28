package com.maxwellwheeler.plugins.tppets.storage;

import java.util.HashSet;
import java.util.Set;

public class AllPetsList {
    private Set<String> dogs = new HashSet<String>();
    private Set<String> cats = new HashSet<String>();
    private Set<String> birds = new HashSet<String>();
    
    public AllPetsList() {
        
    }
    
    public AllPetsList(Set<String> dogs, Set<String> cats, Set<String> birds) {
        this.dogs = dogs;
        this.cats = cats;
        this.birds = birds;
    }
    
    public void addDog(String entUUID) {
        this.dogs.add(entUUID);
    }
    
    public void addCat(String entUUID) {
        this.cats.add(entUUID);
    }
    
    public void addBird(String entUUID) {
        this.birds.add(entUUID);
    }
    
    public void addPet(String entUUID, PetType.Pets pt) {
        switch (pt) {
            case DOG:
                addDog(entUUID);
                break;
            case CAT:
                addCat(entUUID);
                break;
            case PARROT:
                addBird(entUUID);
                break;
            default:
                break;
        }
    }
    
    public void removeDog(String entUUID) {
        this.dogs.remove(entUUID);
    }
    
    public void removeCat(String entUUID) {
        this.cats.remove(entUUID);
    }
    
    public void removeBird(String entUUID) {
        this.birds.remove(entUUID);
    }
    
    public void removePet(String entUUID, PetType.Pets pt) {
        switch (pt) {
            case DOG:
                removeDog(entUUID);
                break;
            case CAT:
                removeCat(entUUID);
                break;
            case PARROT:
                removeBird(entUUID);
                break;
            default:
                break;
        }
    }
    
    public Set<String> getDogs() {
        return dogs;
    }
    
    public Set<String> getCats() {
        return cats;
    }
    
    public Set<String> getBirds() {
        return birds;
    }
    
    public Set<String> getPets(PetType.Pets pt) {
        switch (pt) {
        case DOG:
            return getDogs();
        case CAT:
            return getCats();
        case PARROT:
            return getBirds();
        default:
            return null;
    }
    }
    
    public int getPetsLength(PetType.Pets pt) {
        switch (pt) {
            case DOG:
                return getDogsLength();
            case CAT:
                return getCatsLength();
            case PARROT:
                return getBirdsLength();
            default:
                return -1;
        }
    }
    
    public int getDogsLength() {
        return dogs.size();
    }
    
    public int getCatsLength() {
        return cats.size();
    }
    
    public int getBirdsLength() {
        return birds.size();
    }
    
    public int getTotalLength() {
        return getDogsLength() + getCatsLength() + getBirdsLength();
    }
}
