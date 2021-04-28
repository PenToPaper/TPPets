package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TPPArgValidatorTest {
    @Test
    @DisplayName("validateArgsLength test")
    void validateArgsLength() {
        // False if less than expected
        assertFalse(ArgValidator.validateArgsLength(new String[]{}, 1));
        assertFalse(ArgValidator.validateArgsLength(new String[]{"1"}, 2));
        assertFalse(ArgValidator.validateArgsLength(new String[]{"1", "2"}, 3));

        // True if more than expected
        assertTrue(ArgValidator.validateArgsLength(new String[]{"1"}, 0));
        assertTrue(ArgValidator.validateArgsLength(new String[]{"1", "2"}, 1));
        assertTrue(ArgValidator.validateArgsLength(new String[]{"1", "2", "3"}, 2));

        // True if equal to expected
        assertTrue(ArgValidator.validateArgsLength(new String[]{"1"}, 1));
        assertTrue(ArgValidator.validateArgsLength(new String[]{"1", "2"}, 2));
        assertTrue(ArgValidator.validateArgsLength(new String[]{"1", "2", "3"}, 3));
    }

    @Test
    @DisplayName("softValidatePetName test")
    void softValidatePetName() {
        // False if empty string
        assertFalse(ArgValidator.softValidatePetName(""));

        // True if includes letters + numbers + _ up to 64 in length
        assertTrue(ArgValidator.softValidatePetName("1234567890123456789012345678901234567890123456789012345678901234"));
        assertTrue(ArgValidator.softValidatePetName("abcdefghijklmnopqrstuvwxyz1234567890_"));
        assertTrue(ArgValidator.softValidatePetName("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_"));

        // False if includes an invalid character
        assertFalse(ArgValidator.softValidatePetName("abc;"));
        assertFalse(ArgValidator.softValidatePetName("abc."));
        assertFalse(ArgValidator.softValidatePetName("abc!"));
        assertFalse(ArgValidator.softValidatePetName("abc "));
        assertFalse(ArgValidator.softValidatePetName("ab\nc"));
        assertFalse(ArgValidator.softValidatePetName("abc\n"));
        assertFalse(ArgValidator.softValidatePetName("abc\uD83D\uDE00"));

        // False if over 64 in length
        assertFalse(ArgValidator.softValidatePetName("12345678901234567890123456789012345678901234567890123456789012345"));
    }

    @Test
    @DisplayName("softValidateRegionName test")
    void softValidateRegionName() {
        // False if empty string
        assertFalse(ArgValidator.softValidateRegionName(""));

        // True if includes letters + numbers + _ up to 64 in length
        assertTrue(ArgValidator.softValidateRegionName("1234567890123456789012345678901234567890123456789012345678901234"));
        assertTrue(ArgValidator.softValidateRegionName("abcdefghijklmnopqrstuvwxyz1234567890_ §"));
        assertTrue(ArgValidator.softValidateRegionName("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_ §"));

        // False if includes an invalid character
        assertFalse(ArgValidator.softValidateRegionName("abc;"));
        assertFalse(ArgValidator.softValidateRegionName("abc."));
        assertFalse(ArgValidator.softValidateRegionName("abc!"));
        assertFalse(ArgValidator.softValidateRegionName("abc\uD83D\uDE00"));
        assertFalse(ArgValidator.softValidateRegionName("ab\nc"));
        assertFalse(ArgValidator.softValidateRegionName("abc\n"));

        // False if over 64 in length
        assertFalse(ArgValidator.softValidateRegionName("12345678901234567890123456789012345678901234567890123456789012345"));
    }

    @Test
    @DisplayName("softValidateRegionEnterMessage test")
    void softValidateRegionEnterMessage() {
        // False if empty string
        assertFalse(ArgValidator.softValidateRegionEnterMessage(""));

        // True if includes letters + numbers + _ up to 255 in length
        assertTrue(ArgValidator.softValidateRegionEnterMessage("123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345"));
        assertTrue(ArgValidator.softValidateRegionEnterMessage("abcdefghijklmnopqrstuvwxyz1234567890_ §'.,!"));
        assertTrue(ArgValidator.softValidateRegionEnterMessage("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_ §'.,!"));

        // False if includes an invalid character
        assertFalse(ArgValidator.softValidateRegionEnterMessage("abc;"));
        assertFalse(ArgValidator.softValidateRegionEnterMessage("abc%"));
        assertFalse(ArgValidator.softValidateRegionEnterMessage("abc)"));
        assertFalse(ArgValidator.softValidateRegionEnterMessage("abc\uD83D\uDE00"));
        assertFalse(ArgValidator.softValidateRegionEnterMessage("ab\nc"));
        assertFalse(ArgValidator.softValidateRegionEnterMessage("abc\n"));

        // False if over 255 in length
        assertFalse(ArgValidator.softValidateRegionEnterMessage("1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456"));
    }

    @Test
    @DisplayName("softValidateStorageName test")
    void validateStorageName() {
        // False if empty string
        assertFalse(ArgValidator.softValidateStorageName(""));

        // True if includes letters + numbers + _ up to 64 in length
        assertTrue(ArgValidator.softValidateStorageName("1234567890123456789012345678901234567890123456789012345678901234"));
        assertTrue(ArgValidator.softValidateStorageName("abcdefghijklmnopqrstuvwxyz1234567890_"));
        assertTrue(ArgValidator.softValidateStorageName("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_"));

        // False if includes an invalid character
        assertFalse(ArgValidator.softValidateStorageName("abc;"));
        assertFalse(ArgValidator.softValidateStorageName("abc."));
        assertFalse(ArgValidator.softValidateStorageName("abc!"));
        assertFalse(ArgValidator.softValidateStorageName("abc\uD83D\uDE00"));
        assertFalse(ArgValidator.softValidateStorageName("ab\nc"));
        assertFalse(ArgValidator.softValidateStorageName("abc\n"));

        // False if over 64 in length
        assertFalse(ArgValidator.softValidateStorageName("12345678901234567890123456789012345678901234567890123456789012345"));

        // False if equal to "default"
        assertFalse(ArgValidator.softValidateStorageName("default"));
    }

    @Test
    @DisplayName("softValidateServerStorageName test")
    void softValidateServerStorageName() {
        // False if empty string
        assertFalse(ArgValidator.softValidateServerStorageName(""));

        // True if includes letters + numbers + _ up to 64 in length
        assertTrue(ArgValidator.softValidateServerStorageName("1234567890123456789012345678901234567890123456789012345678901234"));
        assertTrue(ArgValidator.softValidateServerStorageName("abcdefghijklmnopqrstuvwxyz1234567890_"));
        assertTrue(ArgValidator.softValidateServerStorageName("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_"));

        // False if includes an invalid character
        assertFalse(ArgValidator.softValidateServerStorageName("abc;"));
        assertFalse(ArgValidator.softValidateServerStorageName("abc."));
        assertFalse(ArgValidator.softValidateServerStorageName("abc!"));
        assertFalse(ArgValidator.softValidateServerStorageName("abc "));
        assertFalse(ArgValidator.softValidateServerStorageName("ab\nc"));
        assertFalse(ArgValidator.softValidateServerStorageName("abc\n"));
        assertFalse(ArgValidator.softValidateServerStorageName("abc\uD83D\uDE00"));

        // False if over 64 in length
        assertFalse(ArgValidator.softValidateServerStorageName("12345678901234567890123456789012345678901234567890123456789012345"));
    }

    @Test
    @DisplayName("softValidateUsername test")
    void softValidateUsername() {
        // False if string too short
        assertFalse(ArgValidator.softValidateUsername(""));
        assertFalse(ArgValidator.softValidateUsername("ab"));

        // True if includes letters + numbers + _ up to 16 in length
        assertTrue(ArgValidator.softValidateUsername("1234567890123456"));
        assertTrue(ArgValidator.softValidateUsername("abcdefghijklmnop"));
        assertTrue(ArgValidator.softValidateUsername("qrstuvwxyz"));
        assertTrue(ArgValidator.softValidateUsername("ABCDEFGHIJKLMNOP"));
        assertTrue(ArgValidator.softValidateUsername("QRSTUVWXYZ"));
        assertTrue(ArgValidator.softValidateUsername("1234567890_"));

        // False if includes an invalid character
        assertFalse(ArgValidator.softValidateUsername("abc;"));
        assertFalse(ArgValidator.softValidateUsername("abc."));
        assertFalse(ArgValidator.softValidateUsername("abc!"));
        assertFalse(ArgValidator.softValidateUsername("abc "));
        assertFalse(ArgValidator.softValidateUsername("ab\nc"));
        assertFalse(ArgValidator.softValidateUsername("abc\n"));
        assertFalse(ArgValidator.softValidateUsername("abc\uD83D\uDE00"));

        // False if over 16 in length
        assertFalse(ArgValidator.softValidateUsername("12345678901234567"));
    }

    @Test
    @DisplayName("isForSomeoneElse test")
    void isForSomeoneElse() {
        // Null if no f:
        assertNull(ArgValidator.isForSomeoneElse(""));
        assertNull(ArgValidator.isForSomeoneElse("abc"));
        assertNull(ArgValidator.isForSomeoneElse("abc123"));
        assertNull(ArgValidator.isForSomeoneElse("____:"));

        // Empty string if includes only f: or from: (case insensitive)
        assertEquals("", ArgValidator.isForSomeoneElse("f:"));
        assertEquals("", ArgValidator.isForSomeoneElse("from:"));
        assertEquals("", ArgValidator.isForSomeoneElse("F:"));
        assertEquals("", ArgValidator.isForSomeoneElse("FROM:"));

        // Whole name if includes f:
        assertEquals("GatheringExp", ArgValidator.isForSomeoneElse("f:GatheringExp"));
        assertEquals("Username", ArgValidator.isForSomeoneElse("f:Username"));
        assertEquals("Mock:Username", ArgValidator.isForSomeoneElse("f:Mock:Username"));
        assertEquals("Mock-Username", ArgValidator.isForSomeoneElse("f:Mock-Username"));
    }
}
