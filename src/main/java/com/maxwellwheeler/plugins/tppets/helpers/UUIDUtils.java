package com.maxwellwheeler.plugins.tppets.helpers;

import java.util.UUID;

/**
 * Used to manipulate UUIDs for storage.
 * @author GatheringExp
 */
public class UUIDUtils {
    /**
     * Gets a trimmed, string representation of a given UUID.
     * @param id The UUID to evaluate.
     * @return A trimmed, string version of the UUID.
     */
    public static String trimUUID(UUID id) {
        if (id != null) {
            return trimUUID(id.toString());
        }
        return null;
    }

    /**
     * Trims a string representation of a given UUID.
     * @param idString The UUID string to trim.
     * @return A trimmed, string version of the UUID.
     */
    public static String trimUUID(String idString) {
        return idString.replace("-", "");
    }

    /**
     * Untrims a UUID string. It effectively reverses the operation done in {@see trimUUID}. It adds back the - character,
     * as specified by the UUID spec.
     * @param idString The UUID string to untrim.
     * @return An trimmed, string version of the UUID, or null if the idString cannot properly represent a UUID.
     */
    public static String unTrimUUID(String idString) {
        if (idString.length() == 32) {
            return idString.substring(0, 8) + "-" + idString.substring(8, 12) + "-" + idString.substring(12, 16) + "-" + idString.substring(16, 20) + "-" + idString.substring(20);
        }
        return null;
    }
}
