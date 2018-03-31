package com.maxwellwheeler.plugins.tppets.storage;

import java.util.HashSet;
import java.util.Set;

/**
 * A list representing all pets a player is known to have, based on values stored in the database and in loaded chunks.
 * @author GatheringExp
 *
 */
public class AllPetsList {
    private Set<String> dogs = new HashSet<>();
    private Set<String> cats = new HashSet<>();
    private Set<String> birds = new HashSet<>();
    private Set<String> horses = new HashSet<>();
    private Set<String> mules = new HashSet<>();
    private Set<String> llamas = new HashSet<>();
    private Set<String> donkeys = new HashSet<>();

    public AllPetsList() {

    }

    /**
     * Initializes the private variables containing the dogs', cats', and birds' trimmed UUIDs.
     * @param dogs The set of trimmed dog UUIDs.
     * @param cats The set of trimmed cat UUIDs.
     * @param birds The set of trimmed bird UUIDs.
     * @param horses The set of trimmed horse UUIDs.
     * @param mules The set of trimmed mule UUIDs.
     * @param llamas The set of trimmed llama UUIDs.
     * @param donkeys The set of trimmed donkey UUIDs.
     */
    public AllPetsList(Set<String> dogs, Set<String> cats, Set<String> birds, Set<String> horses, Set<String> mules, Set<String> llamas, Set<String> donkeys) {
        this.dogs = dogs;
        this.cats = cats;
        this.birds = birds;
        this.horses = horses;
        this.mules = mules;
        this.llamas = llamas;
        this.donkeys = donkeys;
    }
    
    /**
     * Adds a dog to the internal dog set.
     * @param entUUID A trimmed version of the entity's UUID.
     */
    public void addDog(String entUUID) {
        this.dogs.add(entUUID);
    }
    
    /**
     * Adds a cat to the internal cat set.
     * @param entUUID A trimmed version of the entity's UUID.
     */
    public void addCat(String entUUID) {
        this.cats.add(entUUID);
    }
    
    /**
     * Adds a bird to the internal bird set.
     * @param entUUID A trimmed version of the entity's UUID.
     */
    public void addBird(String entUUID) {
        this.birds.add(entUUID);
    }

    /**
     * Adds a horse to the internal horse set.
     * @param entUUID A trimmed version of the entity's UUID.
     */
    public void addHorse(String entUUID) {
        this.horses.add(entUUID);
    }

    /**
     * Adds a mule to the internal mule set.
     * @param entUUID A trimmed version of the entity's UUID.
     */
    public void addMule(String entUUID) {
        this.mules.add(entUUID);
    }

    /**
     * Adds a llama to the internal llama set.
     * @param entUUID A trimmed version of the entity's UUID.
     */
    public void addLlama(String entUUID) {
        this.llamas.add(entUUID);
    }

    /**
     * Adds a donkey to the internal donkey set.
     * @param entUUID A trimmed version of the entity's UUID.
     */
    public void addDonkey(String entUUID) {
        this.donkeys.add(entUUID);
    }

    /**
     * Adds a pet of any type to the appropriate set, based on the pt argument.
     * @param entUUID A trimmed version of the entity's UUID.
     * @param pt Represents the type of the pet whose UUID is being added to the set.
     */
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
            case HORSE:
                addHorse(entUUID);
                break;
            case MULE:
                addMule(entUUID);
                break;
            case LLAMA:
                addLlama(entUUID);
                break;
            case DONKEY:
                addDonkey(entUUID);
                break;
            default:
                break;
        }
    }
    
    /**
     * Removes a dog UUID from the set.
     * @param entUUID The trimmed UUID to remove.
     */
    public void removeDog(String entUUID) {
        this.dogs.remove(entUUID);
    }
    
    /**
     * Removes a cat UUID from the set.
     * @param entUUID The trimmed UUID to remove.
     */
    public void removeCat(String entUUID) {
        this.cats.remove(entUUID);
    }
    
    /**
     * Removes a bird UUID from the set.
     * @param entUUID The trimmed UUID to remove.
     */
    public void removeBird(String entUUID) {
        this.birds.remove(entUUID);
    }

    /**
     * Removes a horse UUID from the set.
     * @param entUUID The trimmed UUID to remove.
     */
    public void removeHorse(String entUUID) {
        this.horses.remove(entUUID);
    }

    /**
     * Removes a mule UUID from the set.
     * @param entUUID The trimmed UUID to remove.
     */
    public void removeMule(String entUUID) {
        this.mules.remove(entUUID);
    }

    /**
     * Removes a llama UUID from the set.
     * @param entUUID The trimmed UUID to remove.
     */
    public void removeLlama(String entUUID) {
        this.llamas.remove(entUUID);
    }

    /**
     * Removes a donkey UUID from the set.
     * @param entUUID The trimmed UUID to remove.
     */
    public void removeDonkey(String entUUID) {
        this.donkeys.remove(entUUID);
    }
    
    /**
     * Removes a pet of any type from the appropriate set, based on the pt argument.
     * @param entUUID The trimmed UUID to remove.
     * @param pt Represents the type of the pet whose UUID is being added to the set.
     */
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
            case HORSE:
                removeHorse(entUUID);
                break;
            case MULE:
                removeMule(entUUID);
                break;
            case LLAMA:
                removeLlama(entUUID);
                break;
            case DONKEY:
                removeDonkey(entUUID);
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

    public Set<String> getHorses() {
        return birds;
    }

    public Set<String> getMules() {
        return birds;
    }

    public Set<String> getLlamas() {
        return birds;
    }

    public Set<String> getDonkeys() {
        return birds;
    }
    
    /**
     * Gets the appropriate pets UUID set, based on the pt argument.
     * @param pt Represents the type of the pet to get the set for.
     * @return A set representing all trimmed UUIDs of pets that the plugin knows of.
     */
    public Set<String> getPets(PetType.Pets pt) {
        switch (pt) {
            case DOG:
                return getDogs();
            case CAT:
                return getCats();
            case PARROT:
                return getBirds();
            case HORSE:
                return getHorses();
            case MULE:
                return getMules();
            case LLAMA:
                return getLlamas();
            case DONKEY:
                return getDonkeys();
            default:
                return null;
        }
    }
    
    /**
     * Gets the length of the appropriate UUID set.
     * @param pt Represents the type of the pet to get the set for.
     * @return The number of unique pets in the set.
     */
    public int getPetsLength(PetType.Pets pt) {
        switch (pt) {
            case DOG:
                return getDogsLength();
            case CAT:
                return getCatsLength();
            case PARROT:
                return getBirdsLength();
            case HORSE:
                return getHorsesLength();
            case MULE:
                return getMulesLength();
            case LLAMA:
                return getLlamasLength();
            case DONKEY:
                return getDonkeysLength();
            default:
                return -1;
        }
    }
    
    /**
     * Gets the length of the dog set.
     * @return The length of the dog set.
     */
    public int getDogsLength() {
        return dogs.size();
    }
    
    /**
     * Gets the length of the cat set.
     * @return The length of the cat set.
     */
    public int getCatsLength() {
        return cats.size();
    }
    
    /**
     * Gets the length of the bird set.
     * @return The length of the bird set.
     */
    public int getBirdsLength() {
        return birds.size();
    }

    /**
     * Gets the length of the horse set.
     * @return The length of the horse set.
     */
    public int getHorsesLength() {
        return horses.size();
    }

    /**
     * Gets the length of the mule set.
     * @return The length of the mule set.
     */
    public int getMulesLength() {
        return mules.size();
    }

    /**
     * Gets the length of the llama set.
     * @return The length of the llama set.
     */
    public int getLlamasLength() {
        return llamas.size();
    }

    /**
     * Gets the length of the donkey set.
     * @return The length of the donkey set.
     */
    public int getDonkeysLength() {
        return donkeys.size();
    }
    
    /**
     * Gets the length of all sets combined.
     * @return The length of all sets combined.
     */
    public int getTotalLength() {
        return getDogsLength() + getCatsLength() + getBirdsLength() + getHorsesLength() + getMulesLength() + getLlamasLength() + getDonkeysLength();
    }
}
