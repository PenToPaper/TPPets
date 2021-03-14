package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.OfflinePlayer;

import java.sql.SQLException;

/**
 * An index of how many pets each player owns. Used for the limiting of tamed pets.
 * @author GatheringExp
 *
 */
public class PetLimitChecker {
    /**
     * Enum representing the results of when a pet is attempting to be tamed.
     * @author GatheringExp
     *
     */
    public enum RuleRestriction {
        ALLOWED, TOTAL, DOG, CAT, PARROT, HORSE, MULE, LLAMA, DONKEY, UNKNOWN
    }

    private final TPPets thisPlugin;
    private final int totalLimit;
    private final int dogLimit;
    private final int catLimit;
    private final int birdLimit;
    private final int horseLimit;
    private final int muleLimit;
    private final int llamaLimit;
    private final int donkeyLimit;

    
    /**
     * General constructor, referencing the plugin instance, and pet limits from the config file.
     * @param thisPlugin The TPPets instance.
     * @param totalLimit The limit of all pets owned.
     * @param dogLimit The limit of all dogs owned.
     * @param catLimit The limit of all cats owned.
     * @param birdLimit The limit of all birds owned.
     * @param horseLimit The limit of all horses owned.
     * @param muleLimit The limit of all mules owned.
     * @param llamaLimit The limit of all llamas owned.
     * @param donkeyLimit The limit of all donkeys owned.
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
     * Gets the limit specific to a pet of type pt.
     * @param pt The pet type limit to get
     * @return An integer represneting the specific taming limit of pet type pt
     */
    public int getSpecificLimit(PetType.Pets pt) {
        switch (pt) {
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

    public int getTotalLimit() {
        return totalLimit;
    }

    /**
     * Links the enum {@link RuleRestriction} with the enum {@link PetType.Pets}
     * @param pt The {@link PetType.Pets} enum value
     * @return The rule restriction represented by pt
     */
    private RuleRestriction enumLink(PetType.Pets pt) {
        switch (pt) {
            case DOG:
                return RuleRestriction.DOG;
            case CAT:
                return RuleRestriction.CAT;
            case PARROT:
                return RuleRestriction.PARROT;
            case HORSE:
                return RuleRestriction.HORSE;
            case MULE:
                return RuleRestriction.MULE;
            case LLAMA:
                return RuleRestriction.LLAMA;
            case DONKEY:
                return RuleRestriction.DONKEY;
            case UNKNOWN:
            default:
                return RuleRestriction.UNKNOWN;
        }
    }

    public boolean isWithinTotalLimit(OfflinePlayer owner) throws SQLException {
        int numTotalPets = this.thisPlugin.getDatabase().getNumPets(owner.getUniqueId().toString());
        return isWithinLimit(this.totalLimit, numTotalPets);
    }

    public boolean isWithinSpecificLimit(OfflinePlayer owner, PetType.Pets petType) throws SQLException {
        int numSpecificPets = this.thisPlugin.getDatabase().getNumPetsByPT(owner.getUniqueId().toString(), petType);
        return isWithinLimit(this.getSpecificLimit(petType), numSpecificPets);
    }

    /**
     * Helper function that determines if the integer within is under (not inclusive) the limit
     * @param limit The upper bound of the test, non inclusive.
     * @param within The integer being tested that should be under the limit.
     * @return True if within is below limit, false if otherwise
     */
    public boolean isWithinLimit(int limit, int within) {
        return limit < 0 || within < limit;
    }
}
