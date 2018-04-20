package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import org.bukkit.Location;
import org.bukkit.entity.AnimalTamer;
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

    private TPPets thisPlugin;
    private int totalLimit;
    private int dogLimit;
    private int catLimit;
    private int birdLimit;
    private int horseLimit;
    private int muleLimit;
    private int llamaLimit;
    private int donkeyLimit;

    
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
    
    /**
     * Helper function that determines if the integer within is under (not inclusive) the limit
     * @param limit The upper bound of the test, not inclusive.
     * @param within The integer being tested that should be under the limit.
     * @return True if within is below limit, false if otherwise
     */
    private boolean isWithinLimit(int limit, int within) {
        return limit < 0 || within < limit;
    }
    
    /**
     * Core functionality of this object. Determines if a player should be able to tame a given pet.
     * @param at The animal tamer that is attempting to tame a pet
     * @param loc The location of the tamer, used in Vault's permission api
     * @param pt The type of the the pet being tamed.
     * @return The ruling. It can be allowed, disallowed because of X, or unknown
     */
    public RuleRestriction allowTame(AnimalTamer at, Location loc, PetType.Pets pt) {
        if (PermissionChecker.onlineHasPerms(at, "tppets.bypasslimit") || PermissionChecker.offlineHasPerms(at, "tppets.bypasslimit", loc.getWorld(), thisPlugin)) {
            return RuleRestriction.ALLOWED;
        }
        int numPetsByType = thisPlugin.getDatabase().getNumPetsByPT(at.getUniqueId().toString(), pt);
        if (numPetsByType == -1) {
            return RuleRestriction.UNKNOWN;
        }
        if (isWithinLimit(getSpecificLimit(pt), numPetsByType)) {
            int numPetsTotal = thisPlugin.getDatabase().getNumPets(at.getUniqueId().toString());
            if (numPetsTotal == -1) {
                return RuleRestriction.UNKNOWN;
            }
            if (isWithinLimit(totalLimit, numPetsTotal)) {
                return RuleRestriction.ALLOWED;
            } else {
                return RuleRestriction.TOTAL;
            }
        } else {
            return enumLink(pt);
        }
    }
}
