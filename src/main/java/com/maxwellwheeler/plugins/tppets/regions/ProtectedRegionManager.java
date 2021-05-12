package com.maxwellwheeler.plugins.tppets.regions;

import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Hashtable;

public class ProtectedRegionManager {
    private final TPPets thisPlugin;
    private final Hashtable<String, ProtectedRegion> protectedRegions;

    public ProtectedRegionManager(TPPets thisPlugin) throws SQLException {
        this.thisPlugin = thisPlugin;
        this.protectedRegions = thisPlugin.getDatabase().getProtectedRegions();
    }

    public void addProtectedRegion(@NotNull ProtectedRegion protectedRegion) {
        this.protectedRegions.put(protectedRegion.getRegionName(), protectedRegion);
    }

    public void removeProtectedRegion(@NotNull String protectedRegionName) {
        this.protectedRegions.remove(protectedRegionName);
    }

    public ProtectedRegion getProtectedRegion(@NotNull String protectedRegionName) {
        return this.protectedRegions.get(protectedRegionName);
    }

    public ProtectedRegion getProtectedRegionAt(@NotNull Location location) {
        for (ProtectedRegion protectedRegion : this.protectedRegions.values()) {
            if (protectedRegion.isInRegion(location)) {
                return protectedRegion;
            }
        }
        return null;
    }

    public boolean canTpThere(Player pl, Location location) {
        ProtectedRegion tempPr = getProtectedRegionAt(location);
        if (!pl.hasPermission("tppets.tpanywhere") && tempPr != null) {
            pl.sendMessage(tempPr.getEnterMessage());
            return false;
        }
        return true;
    }

    public void updateLFReferences(String lfRegionName) {
        for (ProtectedRegion protectedRegion : this.protectedRegions.values()) {
            if (protectedRegion.getLfName() != null && protectedRegion.getLfName().equals(lfRegionName)) {
                protectedRegion.updateLFReference(this.thisPlugin);
            }
        }
    }

    public Collection<ProtectedRegion> getProtectedRegions() {
        return this.protectedRegions.values();
    }
}
