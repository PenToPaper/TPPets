package com.maxwellwheeler.plugins.tppets.helpers;

import java.util.UUID;

public class UUIDUtils {
    public static String trimUUID(UUID id) {
        return trimUUID(id.toString());
    }
    
    public static String trimUUID(String idString) {
        return idString.replace("-", "");
    }
}
