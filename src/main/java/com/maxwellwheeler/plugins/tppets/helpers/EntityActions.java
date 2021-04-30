package com.maxwellwheeler.plugins.tppets.helpers;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Sittable;

/**
 * Simple class for general entity actions
 * @author GatheringExp
 */
public class EntityActions {
    /**
     * Sets the ent sitting if applicable
     * @param ent The ent to set sitting
     */
    public static void setSitting(Entity ent) {
        if (ent instanceof Sittable) {
            Sittable sittableTemp = (Sittable) ent;
            sittableTemp.setSitting(true);
        }
    }

    /**
     * Sets the ent standing if applicable
     * @param ent The ent to set standing
     */
    public static void setStanding(Entity ent) {
        if (ent instanceof Sittable) {
            Sittable sittableTemp = (Sittable) ent;
            sittableTemp.setSitting(false);
        }
    }
}
