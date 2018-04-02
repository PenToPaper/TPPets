package com.maxwellwheeler.plugins.tppets.helpers;

import java.util.UUID;

/**
 * Utilities to work with UUIDs
 * @author GatheringExp
 *
 */
public class UUIDUtils {
    /**
     * Gets a trimmed version of the UUID's .toString()
     * @param id The UUID to evaluate
     * @return A trimmed version of the UUID's .toString()
     */
    public static String trimUUID(UUID id) {
        if (id != null) {
            return trimUUID(id.toString());
        }
        return null;
    }
    
    /**
     * Gets a trimmed version of the string
     * @param idString The UUID string to evaluate
     * @return A trimmed version of the string
     */
    public static String trimUUID(String idString) {
        return idString.replace("-", "");
    }

    /**
     * Untrims a UUID, effectively reversing the opration done in trimUUID
     * @param idString The UUID string to untrim
     * @return The untrimmed uuid, or null if the UUID is not of length 32
     */
    public static String unTrimUUID(String idString) {
        if (idString.length() == 32) {
            return idString.substring(0, 8) + "-" + idString.substring(8, 12) + "-" + idString.substring(12, 16) + "-" + idString.substring(16, 20) + "-" + idString.substring(20);
        }
        return null;
    }
}
