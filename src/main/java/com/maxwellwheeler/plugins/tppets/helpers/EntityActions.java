package com.maxwellwheeler.plugins.tppets.helpers;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Sittable;

public class EntityActions {
    public static void setSitting(Entity ent) {
        if (ent instanceof Sittable) {
            Sittable sittableTemp = (Sittable) ent;
            sittableTemp.setSitting(true);
        }
    }

    public static void setStanding(Entity ent) {
        if (ent instanceof Sittable) {
            Sittable sittableTemp = (Sittable) ent;
            sittableTemp.setSitting(false);
        }
    }

    public static void removePassenger(Entity ent) {
        ent.eject();
    }
}
