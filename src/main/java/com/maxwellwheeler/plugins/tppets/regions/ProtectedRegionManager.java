package com.maxwellwheeler.plugins.tppets.regions;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Hashtable;

/**
 * Used to manage a set of {@link ProtectedRegion}s.
 * @author GatheringExp
 */
public class ProtectedRegionManager {
    /** A reference to the active {@link TPPets} instance. */
    private final TPPets thisPlugin;
    /** A hashtable of &lt;Protected Region Name&lt;Protected Region&gt;&gt; */
    private final Hashtable<String, ProtectedRegion> protectedRegions;

    /**
     * Initializes the table of {@link ProtectedRegion}s through provided {@link TPPets}'s {@link SQLWrapper#getProtectedRegions()}.
     * @param thisPlugin A reference to the active {@link TPPets} instance. This is stored within the manager for use
     *                   with relinking protected regions.
     * @throws SQLException If getting all protected regions from the database fails.
     */
    public ProtectedRegionManager(TPPets thisPlugin) throws SQLException {
        this.thisPlugin = thisPlugin;
        this.protectedRegions = thisPlugin.getDatabase().getProtectedRegions();
    }

    /**
     * Adds a {@link ProtectedRegion} to this manager's table. Overwrites any existing {@link ProtectedRegion} with
     * that name. Cannot be null.
     * @param protectedRegion The {@link ProtectedRegion} to add. Cannot be null.
     */
    public void addProtectedRegion(@NotNull ProtectedRegion protectedRegion) {
        this.protectedRegions.put(protectedRegion.getRegionName(), protectedRegion);
    }

    /**
     * Removes a {@link ProtectedRegion} from this manager's table.
     * @param protectedRegionName The {@link ProtectedRegion}'s name to remove. Cannot be null.
     */
    public void removeProtectedRegion(@NotNull String protectedRegionName) {
        this.protectedRegions.remove(protectedRegionName);
    }

    /**
     * Gets a specific {@link ProtectedRegion} from the table, or null if none exists.
     * @param protectedRegionName The {@link ProtectedRegion}'s name to get. Cannot be null.
     * @return The {@link ProtectedRegion} in this manager with specified name, or null if none exists.
     */
    public ProtectedRegion getProtectedRegion(@NotNull String protectedRegionName) {
        return this.protectedRegions.get(protectedRegionName);
    }

    /**
     * Gets the first {@link ProtectedRegion} from the table that the given location is within, or none if the given
     * location is not in any {@link ProtectedRegion}s.
     * @param location The location to find any {@link ProtectedRegion} at.
     * @return A {@link ProtectedRegion} that encompasses the {@link Location}, or null if none exists in this manager.
     */
    public ProtectedRegion getProtectedRegionAt(@NotNull Location location) {
        for (ProtectedRegion protectedRegion : this.protectedRegions.values()) {
            if (protectedRegion.isInRegion(location)) {
                return protectedRegion;
            }
        }
        return null;
    }

    /**
     * Determines if a player can teleport a pet to a given location, based on whether or not the location is in a {@link ProtectedRegion},
     * and if the player has tppets.tpanywhere.
     * @param pl The player attempting to teleport a pet.
     * @param location The location the player is attempting to teleport a pet to.
     * @return true if the player can teleport a pet there, false if not.
     */
    public boolean canTpThere(Player pl, Location location) {
        ProtectedRegion tempPr = getProtectedRegionAt(location);
        if (!pl.hasPermission("tppets.tpanywhere") && tempPr != null) {
            pl.sendMessage(tempPr.getEnterMessage());
            return false;
        }
        return true;
    }

    /**
     * Calls each {@link ProtectedRegion}'s {@link ProtectedRegion#updateLFReference(TPPets)} with the {@link TPPets} instance
     * provided in the constructor.
     * @param lfRegionName The new {@link LostAndFoundRegion} name to update the reference to.
     */
    public void updateLFReferences(String lfRegionName) {
        for (ProtectedRegion protectedRegion : this.protectedRegions.values()) {
            if (protectedRegion.getLfName() != null && protectedRegion.getLfName().equals(lfRegionName)) {
                protectedRegion.updateLFReference(this.thisPlugin);
            }
        }
    }

    /**
     * Gets a collection of all {@link ProtectedRegion} references that this manager has in its table.
     * @return A collection of all {@link ProtectedRegion}s.
     */    public Collection<ProtectedRegion> getProtectedRegions() {
        return this.protectedRegions.values();
    }
}
