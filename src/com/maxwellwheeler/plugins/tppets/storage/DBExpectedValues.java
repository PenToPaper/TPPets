package com.maxwellwheeler.plugins.tppets.storage;

public class DBExpectedValues {
    private boolean strict;
    private Object value;
    private int expectedValue;
    
    public DBExpectedValues(Object value, int expectedValue, boolean strict) {
        this.strict = strict;
        this.value = value;
        this.expectedValue = expectedValue;
    }
    
    public boolean meetsExpectedValue() {
        if (strict) {
            if (value instanceof String) {
                return ((String) value).length() == expectedValue;
            } else if (value instanceof Integer) {
                return ((Integer) value) == expectedValue;
            }
        } else {
            if (value instanceof String) {
                return ((String) value).length() <= expectedValue;
            } else if (value instanceof Integer) {
                return ((Integer) value) <= expectedValue;
            }
        }
        return false;
    }
}