package com.maxwellwheeler.plugins.tppets.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used to validate arguments and the arguments array
 * @author GatheringExp
 */
public class ArgValidator {
    /**
     * Used to validate the number of arguments in an array of arguments.
     * @param args The array of arguments.
     * @param expected The expected array length.
     * @return true if non-null values occupy the args array up to the expected length, false if not.
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
     * Checks if the pet name has any invalid characters through regex matching. A valid string has only word characters
     * and is between 1 and 64 in length.
     * @param petName The pet name to check.
     * @return true if pet name valid, false if not.
     */
    public static boolean softValidatePetName(String petName) {
        Matcher nameMatcher = Pattern.compile("\\A\\w{1,64}\\z").matcher(petName);
        return nameMatcher.find();
    }

    /**
     * Checks if the region name has any invalid characters through regex matching. A valid string has only word characters,
     * spaces, or §, and is between 1 and 64 in length.
     * @param regionName The region name to check.
     * @return true if region name valid, false if not.
     */
    public static boolean softValidateRegionName(String regionName) {
        Matcher nameMatcher = Pattern.compile("\\A[\\w§ ]{1,64}\\z").matcher(regionName);
        return nameMatcher.find();
    }

    /**
     * Checks if the enter message has any invalid characters through regex matching. A valid string has only word characters,
     * spaces, §, ', ., ,, or !, and is between 1 and 255 in length.
     * @param enterMessage The enter message to check.
     * @return true if enter message valid, false if not.
     */
    public static boolean softValidateRegionEnterMessage(String enterMessage) {
        Matcher nameMatcher = Pattern.compile("\\A[\\w§ '.,!]{1,255}\\z").matcher(enterMessage);
        return nameMatcher.find();
    }

    /**
     * Checks if the storage name has any invalid characters through regex matching. A valid string has only word characters
     * and is between 1 and 64 in length.
     * @param storageName The storage name to check.
     * @return true if storage name valid, false if not.
     */
    public static boolean softValidateStorageName(String storageName) {
        return softValidateServerStorageName(storageName);
    }

    /**
     * Checks if the server storage name has any invalid characters through regex matching. A valid string has only word characters
     * and is between 1 and 64 in length.
     * @param serverStorageName The server storage name to check.
     * @return true if server storage name valid, false if not.
     */
    public static boolean softValidateServerStorageName(String serverStorageName) {
        Matcher nameMatcher = Pattern.compile("\\A\\w{1,64}\\z").matcher(serverStorageName);
        return nameMatcher.find();
    }

    /**
     * Checks if the Minecraft username has any invalid characters through regex matching. A valid string has only word characters
     * and is between 3 and 16 in length.
     * @param userName The username to check.
     * @return true if username valid, false if not.
     */
    public static boolean softValidateUsername(String userName) {
        Matcher nameMatcher = Pattern.compile("\\A\\w{3,16}\\z").matcher(userName);
        return nameMatcher.find();
    }

    /**
     * Checks if usernameString is of the type f:[username] or from:[username].
     * @param usernameString The username string to check.
     * @return A string representing the username after f:[username], null if argument is not in the correct format.
     */
    public static String isForSomeoneElse(String usernameString) {
        int indexOfColon = usernameString.indexOf(':');
        if (indexOfColon > 0) {
            String toEvaluate = usernameString.substring(0,indexOfColon).toLowerCase();
            // f: or from: lead the string
            if (toEvaluate.equalsIgnoreCase("f") || toEvaluate.equalsIgnoreCase("from")) {
                return usernameString.substring(indexOfColon+1);
            }
        }
        return null;
    }
}
