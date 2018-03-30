package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static boolean softValidatePetName(String petName) {
        Matcher nameMatcher = Pattern.compile("^\\w{1,64}$").matcher(petName);
        return nameMatcher.find();
    }

    public static boolean validatePetName(DBWrapper dbw, String ownerUUID, String petName) {
        if (dbw != null) {
            return softValidatePetName(petName) && !petName.equals("list") && dbw.isNameUnique(ownerUUID, petName);
        }
        return false;
    }

    public static boolean validateUsername(String userName) {
        Matcher nameMatcher = Pattern.compile("^\\w{3,16}$").matcher(userName);
        return nameMatcher.find();
    }

    public static String isForSomeoneElse(String argOne) {
        // indexOf == 0 WHEN f: leads the string
        if (argOne.indexOf("f:") == 0) {
            // f: leads the string
            return argOne.substring(2);
        }
        if (argOne.indexOf("from:") == 0) {
            // from: leads the string
            return argOne.substring(5);
        }
        return null;
    }
}
