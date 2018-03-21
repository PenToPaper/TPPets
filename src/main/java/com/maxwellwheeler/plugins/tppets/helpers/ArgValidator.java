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
    public static boolean validateArgs(String[] args, int length) {
        if (args.length >= length) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean validatePetName(DBWrapper dbw, String ownerUUID, String petName) {
        if (dbw != null) {
            Matcher nameMatcher = Pattern.compile("^\\w{1,64}$").matcher(petName);
            return nameMatcher.find() && !petName.equals("list") && dbw.isNameUnique(ownerUUID, petName);
        }
        return false;
    }
}
