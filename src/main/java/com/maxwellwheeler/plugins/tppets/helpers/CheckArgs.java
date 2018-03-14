package com.maxwellwheeler.plugins.tppets.helpers;

public class CheckArgs {
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
}
