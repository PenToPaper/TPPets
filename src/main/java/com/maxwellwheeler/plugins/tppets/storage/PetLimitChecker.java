package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;

/**
 * Used to determine if a player can tame a new pet, based on config settings.
 * @author GatheringExp
 */
public class PetLimitChecker {
    /** A reference to the active TPPets instance. */
    private final TPPets thisPlugin;
    /** The configured limit to the total number of pets a player can own. */
    private final int totalLimit;
    /** The configured limit to the number of dogs a player can own. */
    private final int dogLimit;
    /** The configured limit to the number of cats a player can own. */
    private final int catLimit;
    /** The configured limit to the number of birds a player can own. */
    private final int birdLimit;
    /** The configured limit to the number of horses a player can own. */
    private final int horseLimit;
    /** The configured limit to the number of mules a player can own. */
    private final int muleLimit;
    /** The configured limit to the number of llamas a player can own. */
    private final int llamaLimit;
    /** The configured limit to the number of donkeys a player can own. */
    private final int donkeyLimit;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param totalLimit The configured limit to the total number of pets owned.
     * @param dogLimit The configured limit to the number of dogs owned.
     * @param catLimit The configured limit to the number of cats owned.
     * @param birdLimit The configured limit to the number of birds owned.
     * @param horseLimit The configured limit to the number of horses owned.
     * @param muleLimit The configured limit to the number of mules owned.
     * @param llamaLimit The configured limit to the number of llamas owned.
     * @param donkeyLimit The configured limit to the number of donkeys owned.
     */
    public PetLimitChecker(TPPets thisPlugin, int totalLimit, int dogLimit, int catLimit, int birdLimit, int horseLimit, int muleLimit, int llamaLimit, int donkeyLimit) {
        this.thisPlugin = thisPlugin;
        this.totalLimit = totalLimit;
        this.dogLimit = dogLimit;
        this.catLimit = catLimit;
        this.birdLimit = birdLimit;
        this.horseLimit = horseLimit;
        this.muleLimit = muleLimit;
        this.llamaLimit = llamaLimit;
        this.donkeyLimit = donkeyLimit;
    }
    
    /**
     * Gets the specific limit for a pet type.
     * @param petType The pet type to get the limit of.
     * @return The configured limit to the number of that pet type a player can own.
     */
    public int getSpecificLimit(PetType.Pets petType) {
        switch (petType) {
            case DOG:
                return dogLimit;
            case CAT:
                return catLimit;
            case PARROT:
                return birdLimit;
            case HORSE:
                return horseLimit;
            case MULE:
                return muleLimit;
            case LLAMA:
                return llamaLimit;
            case DONKEY:
                return donkeyLimit;
            default:
                return -1;
        }
    }

    /**
     * Gets the total pet limit.
     * @return The configured limit to the total number of pets a player can own.
     */
    public int getTotalLimit() {
        return totalLimit;
    }

    /**
     * Determines if the player has room in their total pet limit to tame a new pet.
     * @param owner The owner of the pet.
     * @return true if the player can tame an additional pet and be within the total limit, false if not.
     * @throws SQLException If getting the total number of owned pets from the database fails.
     */
    public boolean isWithinTotalLimit(OfflinePlayer owner) throws SQLException {
        int numTotalPets = this.thisPlugin.getDatabase().getNumPets(owner.getUniqueId().toString());
        return isWithinLimit(this.totalLimit, numTotalPets);
    }

    /**
     * Determines if the player has room in their specific pet type limit to tame a new pet.
     * @param owner The owner of the pet.
     * @param petType The specific pet type to get the limit of.
     * @return true if the player can tame an additional pet and be within the pet type's specific limit, false if not.
     * @throws SQLException If getting the total number of owned pets of the pet type from the database fails.
     */
    public boolean isWithinSpecificLimit(OfflinePlayer owner, PetType.Pets petType) throws SQLException {
        int numSpecificPets = this.thisPlugin.getDatabase().getNumPetsByPetType(owner.getUniqueId().toString(), petType);
        return isWithinLimit(this.getSpecificLimit(petType), numSpecificPets);
    }

    /**
     * Determines if the integer within is under (not inclusive) the limit, or if the limit is negative.
     * @param limit The upper limit, non inclusive.
     * @param within The integer being tested that should be under the limit.
     * @return true if within is below limit or if limit is negative, false if otherwise.
     */
    private boolean isWithinLimit(int limit, int within) {
        return limit < 0 || within < limit;
    }
}
