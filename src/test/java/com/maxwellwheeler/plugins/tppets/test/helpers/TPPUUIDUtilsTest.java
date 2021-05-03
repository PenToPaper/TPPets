package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TPPUUIDUtilsTest {
    @Test
    @DisplayName("trimUUID with UUID supplied directly")
    void trimUUIDUDirectUUID() {
        UUID uuid1 = UUID.fromString("12345678-1234-1234-1234-1234567890AB");
        assertEquals("123456781234123412341234567890ab", UUIDUtils.trimUUID(uuid1));

        UUID uuid2 = UUID.fromString("ABCDEFAB-ABCD-ABCD-ABCD-ABCDEFABCDEF");
        assertEquals("abcdefababcdabcdabcdabcdefabcdef", UUIDUtils.trimUUID(uuid2));
    }

    @Test
    @DisplayName("trimUUID with UUID supplied with string")
    void trimUUIDUStringUUID() {
        assertEquals("123456781234123412341234567890AB", UUIDUtils.trimUUID("12345678-1234-1234-1234-1234567890AB"));

        assertEquals("ABCDEFABABCDABCDABCDABCDEFABCDEF", UUIDUtils.trimUUID("ABCDEFAB-ABCD-ABCD-ABCD-ABCDEFABCDEF"));

        assertEquals("1234512345", UUIDUtils.trimUUID("12345-12345"));

        assertEquals("XYZXYZ", UUIDUtils.trimUUID("XYZ-XYZ"));
    }

    @Test
    @DisplayName("unTrimUUID with valid UUID string")
    void unTrimUUIDValidString() {
        assertEquals("12345678-1234-1234-1234-1234567890AB", UUIDUtils.unTrimUUID("123456781234123412341234567890AB"));

        assertEquals("ABCDEFAB-ABCD-ABCD-ABCD-ABCDEFABCDEF", UUIDUtils.unTrimUUID("ABCDEFABABCDABCDABCDABCDEFABCDEF"));

        assertEquals("ABCDEFGH-IJKL-MNOP-QRST-UVWXYZ123456", UUIDUtils.unTrimUUID("ABCDEFGHIJKLMNOPQRSTUVWXYZ123456"));
    }

    @Test
    @DisplayName("unTrimUUID with incorrect length UUID string")
    void unTrimUUIDInvalidLengthString() {
        // Shorter length
        assertNull(UUIDUtils.unTrimUUID("123456781234123412341234567890A"));
        assertNull(UUIDUtils.unTrimUUID(""));

        // Longer length
        assertNull(UUIDUtils.unTrimUUID("ABCDEFABABCDABCDABCDABCDEFABCDEFA"));
    }
}
