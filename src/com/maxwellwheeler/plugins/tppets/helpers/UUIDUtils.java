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
        return trimUUID(id.toString());
    }
    
    /**
     * Gets a trimmed version of the string
     * @param idString The UUID string to evaluate
     * @return A trimmed version of the string
     */
    public static String trimUUID(String idString) {
        return idString.replace("-", "");
    }
}
