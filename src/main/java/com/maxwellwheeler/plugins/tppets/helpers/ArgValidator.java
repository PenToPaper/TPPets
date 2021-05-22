package com.maxwellwheeler.plugins.tppets.helpers;

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
     * @param expected The expected length.
     * @return True if non-null values occupy the args array up to length, false if otherwise.
     */
    public static boolean validateArgsLength(String[] args, int expected) {
        if (args.length >= expected) {
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
        Matcher nameMatcher = Pattern.compile("\\A\\w{1,64}\\z").matcher(petName);
        return nameMatcher.find();
    }

    public static boolean softValidateRegionName(String regionName) {
        Matcher nameMatcher = Pattern.compile("\\A[\\w§ ]{1,64}\\z").matcher(regionName);
        return nameMatcher.find();
    }

    public static boolean softValidateRegionEnterMessage(String enterMessage) {
        Matcher nameMatcher = Pattern.compile("\\A[\\w§ '.,!]{1,255}\\z").matcher(enterMessage);
        return nameMatcher.find();
    }

    /**
     * Checks if the storage name is valid
     * NOTE: Does not check for uniqueness
     * @param storeName The storage name to check
     * @return If the storage name is valid
     */
    public static boolean softValidateStorageName(String storeName) {
        return softValidateServerStorageName(storeName);
    }

    /**
     * Checks if the server storage name is valid
     * NOTE: Does not check for uniqueness
     * @param serverStoreName The storage name to check
     * @return If the storage name is valid
     */
    public static boolean softValidateServerStorageName(String serverStoreName) {
        Matcher nameMatcher = Pattern.compile("\\A\\w{1,64}\\z").matcher(serverStoreName);
        return nameMatcher.find();
    }

    /**
     * Checks if username COULD be a valid Minecraft name. Does not check if that name is actually in use.
     * @param userName The username to check
     * @return True if possible, false if not
     */
    public static boolean softValidateUsername(String userName) {
        Matcher nameMatcher = Pattern.compile("\\A\\w{3,16}\\z").matcher(userName);
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
            if (toEvaluate.equalsIgnoreCase("f") || toEvaluate.equalsIgnoreCase("from")) {
                return argOne.substring(indexOfColon+1);
            }
        }
        return null;
    }
}
