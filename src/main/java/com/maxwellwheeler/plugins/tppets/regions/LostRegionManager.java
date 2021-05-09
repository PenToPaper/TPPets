package com.maxwellwheeler.plugins.tppets.regions;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Hashtable;

public class LostRegionManager {
    private final Hashtable<String, LostAndFoundRegion> lostRegions;

    public LostRegionManager(TPPets thisPlugin) throws SQLException {
        this.lostRegions = thisPlugin.getDatabase().getLostRegions();
    }

    public void addLostRegion(@NotNull LostAndFoundRegion lostRegion) {
        this.lostRegions.put(lostRegion.getRegionName(), lostRegion);
    }

    public void removeLostRegion(@NotNull String lostRegionName) {
        this.lostRegions.remove(lostRegionName);
    }

    public LostAndFoundRegion getLostRegion(@NotNull String lostRegionName) {
        return this.lostRegions.get(lostRegionName);
    }

    public LostAndFoundRegion getLostRegionAt(@NotNull Location location) {
        for (LostAndFoundRegion lostRegion : this.lostRegions.values()) {
            if (lostRegion.isInRegion(location)) {
                return lostRegion;
            }
        }
        return null;
    }

    public Collection<LostAndFoundRegion> getLostRegions() {
        return this.lostRegions.values();
    }
}
