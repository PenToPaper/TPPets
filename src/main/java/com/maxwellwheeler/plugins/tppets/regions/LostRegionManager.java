package com.maxwellwheeler.plugins.tppets.regions;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Hashtable;

/**
 * Used to manage a set of {@link LostAndFoundRegion}s.
 * @author GatheringExp
 */
public class LostRegionManager {
    /** A hashtable of &lt;Lost and Found Region Name&lt;Lost and Found Region&gt;&gt; */
    private final Hashtable<String, LostAndFoundRegion> lostRegions;

    /**
     * Initializes the table of {@link LostAndFoundRegion}s through provided {@link TPPets}'s {@link SQLWrapper#getLostRegions()}.
     * @param thisPlugin A reference to the active {@link TPPets} instance. This is not stored within the manager.
     * @throws SQLException If getting all lost regions from the database fails.
     */
    public LostRegionManager(TPPets thisPlugin) throws SQLException {
        this.lostRegions = thisPlugin.getDatabase().getLostRegions();
    }

    /**
     * Adds a {@link LostAndFoundRegion} to this manager's table. Overwrites any existing {@link LostAndFoundRegion} with
     * that name. Cannot be null.
     * @param lostRegion The {@link LostAndFoundRegion} to add. Cannot be null.
     */
    public void addLostRegion(@NotNull LostAndFoundRegion lostRegion) {
        this.lostRegions.put(lostRegion.getRegionName(), lostRegion);
    }

    /**
     * Removes a {@link LostAndFoundRegion} from this manager's table.
     * @param lostRegionName The {@link LostAndFoundRegion}'s name to remove. Cannot be null.
     */
    public void removeLostRegion(@NotNull String lostRegionName) {
        this.lostRegions.remove(lostRegionName);
    }

    /**
     * Gets a specific {@link LostAndFoundRegion} from the table, or null if none exists.
     * @param lostRegionName The {@link LostAndFoundRegion}'s name to get. Cannot be null.
     * @return The {@link LostAndFoundRegion} in this manager with specified name, or null if none exists.
     */
    public LostAndFoundRegion getLostRegion(@NotNull String lostRegionName) {
        return this.lostRegions.get(lostRegionName);
    }

    /**
     * Gets the first {@link LostAndFoundRegion} from the table that the given location is within, or none if the given
     * location is not in any {@link LostAndFoundRegion}s.
     * @param location The location to find any {@link LostAndFoundRegion} at.
     * @return A {@link LostAndFoundRegion} that encompasses the {@link Location}, or null if none exists in this manager.
     */
    public LostAndFoundRegion getLostRegionAt(@NotNull Location location) {
        for (LostAndFoundRegion lostRegion : this.lostRegions.values()) {
            if (lostRegion.isInRegion(location)) {
                return lostRegion;
            }
        }
        return null;
    }

    /**
     * Gets a collection of all {@link LostAndFoundRegion} references that this manager has in its table.
     * @return A collection of all {@link LostAndFoundRegion}s.
     */
    public Collection<LostAndFoundRegion> getLostRegions() {
        return this.lostRegions.values();
    }
}
