package com.maxwellwheeler.plugins.tppets.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//TODO rename
public class TimeCalculator {
    private static final int idealTps = 20;
    private static final Pattern findYearRegex = Pattern.compile("\\d*y");
    private static final Pattern findDayRegex = Pattern.compile("\\d*d");
    private static final Pattern findHourRegex = Pattern.compile("\\d*h");
    private static final Pattern findMinuteRegex = Pattern.compile("\\d*m");
    private static final Pattern findSecondsRegex = Pattern.compile("\\d*s");
    
    // Parses strings of the format: 1y1d1h1m1s
    public static int getTimeFromString(String timeStr) {
        int totalTicks = 0;
        totalTicks += getTimeFromYearInt(getIntFromString(findTimeRegex(timeStr, findYearRegex)));
        totalTicks += getTimeFromDayInt(getIntFromString(findTimeRegex(timeStr, findDayRegex)));
        totalTicks += getTimeFromHourInt(getIntFromString(findTimeRegex(timeStr, findHourRegex)));
        totalTicks += getTimeFromMinuteInt(getIntFromString(findTimeRegex(timeStr, findMinuteRegex)));
        totalTicks += getTimeFromSecondInt(getIntFromString(findTimeRegex(timeStr, findSecondsRegex)));
        return totalTicks;
    }
    
    private static int getTimeFromYearInt(int yrInt) {
        return getTimeFromDayInt(yrInt * 365);
    }
    
    private static int getTimeFromDayInt(int dyInt) {
        return getTimeFromHourInt(dyInt * 24);
    }
    
    private static int getTimeFromHourInt(int hrInt) {
        return getTimeFromMinuteInt(hrInt * 60);
    }
    
    private static int getTimeFromMinuteInt(int mnInt) {
        return getTimeFromSecondInt(mnInt * 60);
    }
    
    private static int getTimeFromSecondInt(int scInt) {
        return scInt * idealTps;
    }
    
    private static int getIntFromString(String str) {
        String tempString = str.replaceAll("[^\\d]", "");
        return tempString.length() == 0 ? 0 : Integer.parseInt(tempString);
    }
    
    private static String findTimeRegex(String searchThis, Pattern regexPattern) {
        Matcher m = regexPattern.matcher(searchThis);
        if (m.find()) {
            return m.group(0);
        }
        return "";
    }
    
    public static void main(String[] args) {
        System.out.println(getTimeFromString("1s30d"));
    }
}
