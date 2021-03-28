package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to validate types of arguments and the length of the arguments array
 * @author GatheringExp
 *
 */
public class ArgValidator {
    /**
     * Used to validate the number of arguments for commands with multiple arguments, and a certain number of them expected.
     * @param args The list of arguments.
     * @param length The expected length.
     * @return True if non-null values occupy the args array up to length, false if otherwise.
     */
    public static boolean validateArgsLength(String[] args, int length) {
        if (args.length >= length) {
            for (String arg : args) {
                if (arg == null) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Checks if the pet name is a possibility through regex matching
     * @param petName The pet name to check
     * @return True if pet name checks out, false otherwise
     */
    public static boolean softValidatePetName(String petName) {
        Matcher nameMatcher = Pattern.compile("^\\w{1,64}$").matcher(petName);
        return nameMatcher.find() && !petName.equalsIgnoreCase("list") && !petName.toLowerCase().equals("all");
    }

    public static boolean softValidateRegionName(String regionName) {
        Matcher nameMatcher = Pattern.compile("^[\\w§ ]{1,64}$").matcher(regionName);
        return nameMatcher.find();
    }

    public static boolean softValidateRegionEnterMessage(String enterMessage) {
        Matcher nameMatcher = Pattern.compile("^[\\w§ '.,!]{1,255}$").matcher(enterMessage);
        return nameMatcher.find();
    }

    /**
     * Checks if the storage name is valid
     * NOTE: Does not check for uniqueness
     * @param storeName The storage name to check
     * @return If the storage name is valid
     */
    public static boolean validateStorageName(String storeName) {
        return validateServerStorageName(storeName) && !storeName.equalsIgnoreCase("default");
    }

    /**
     * Checks if the server storage name is valid
     * NOTE: Does not check for uniqueness
     * @param serverStoreName The storage name to check
     * @return If the storage name is valid
     */
    public static boolean validateServerStorageName(String serverStoreName) {
        Matcher nameMatcher = Pattern.compile("^\\w{1,64}$").matcher(serverStoreName);
        return nameMatcher.find();
    }

    /**
     * Soft validates pet names and checks that pet name is unique.
     * @param ownerUUID The pet owner's UUID, used for checking if pet names are unique to them
     * @param petName The pet name to validate
     * @return True if pet name checks out, false otherwise
     */
    public static boolean validatePetName(SQLWrapper sqlWrapper, String ownerUUID, String petName) throws SQLException {
        return softValidatePetName(petName) && sqlWrapper.isNameUnique(ownerUUID, petName);
    }

    /**
     * Checks if username COULD be a valid Minecraft name. Does not check if that name is actually in use.
     * @param userName The username to check
     * @return True if possible, false if not
     */
    public static boolean validateUsername(String userName) {
        Matcher nameMatcher = Pattern.compile("^\\w{3,16}$").matcher(userName);
        return nameMatcher.find();
    }

    /**
     * Checks if argOne is of the type f:[username] or from:["username]
     * @param argOne The argument to check
     * @return A string representing the username after f:[username], null if argument is not of the type f:[username]/from:[username]
     */
    public static String isForSomeoneElse(String argOne) {
        int indexOfColon = argOne.indexOf(':');
        if (indexOfColon > 0) {
            String toEvaluate = argOne.substring(0,indexOfColon).toLowerCase();
            // f: or from: lead the string
            if (toEvaluate.equals("f") || toEvaluate.equals("from")) {
                return argOne.substring(indexOfColon+1);
            }
        }
        return null;
    }
}
