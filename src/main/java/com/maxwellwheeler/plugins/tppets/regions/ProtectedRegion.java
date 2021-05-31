package com.maxwellwheeler.plugins.tppets.regions;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Represents a protected region, a cube region where pets found inside without permission are teleported to their
 * corresponding {@link LostAndFoundRegion}.
 * @author GatheringExp
 */
public class ProtectedRegion extends Region {
    /** Represents the message sent to players who attempt to teleport a pet in a protected region without permission. */
    private final String enterMessage;

    /** The name of the {@link LostAndFoundRegion} that this Protected Region is linked to. This can't be null, but can
     *  be an empty string */
    private String lfName;

    /** A reference to the {@link LostAndFoundRegion} object that this Protected Region is linked to. This can be null. */
    private LostAndFoundRegion lfReference;
    
    /**
     * Constructor for when the world is only known by its name. Primarily used when reconstructing regions from the db.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param regionName The ProtectedRegion's name.
     * @param enterMessage The message sent to players who attempt to teleport a pet in a protected region without
*                     permission. Supports &#38;[#] color codes.
     * @param lfString A string representing the name of the {@link LostAndFoundRegion} linked to this ProtectedRegion.
*                 This can be an empty string, or point to a {@link LostAndFoundRegion} that doesn't exist.
     * @param worldName The ProtectedRegion's world's name.
     * @param minZ The cube region's minimum z coordinate.
     * @param maxX The cube region's maximum x coordinate.
     * @param maxY The cube region's maximum y coordinate.
     * @param maxZ The cube region's maximum z coordinate.
     * @param minX The cube region's minimum x coordinate.
     * @param minY The cube region's minimum y coordinate.
     */
    public ProtectedRegion(TPPets thisPlugin, String regionName, String enterMessage, String lfString, String worldName, int minZ, int maxX, int maxY, int maxZ, int minX, int minY) {
        this(thisPlugin, regionName, enterMessage, lfString, worldName, Bukkit.getWorld(worldName), new Location(Bukkit.getWorld(worldName), maxX, maxY, maxZ), new Location(Bukkit.getWorld(worldName), minX, minY, minZ));
    }
    
    /**
     * Constructor for when the world object is known.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param regionName The ProtectedRegion's name.
     * @param enterMessage The message sent to players who attempt to teleport a pet in a protected region without
*                     permission. Supports &#38;[#] color codes.
     * @param lfString A string representing the name of the {@link LostAndFoundRegion} linked to this ProtectedRegion.
*                 This can be an empty string, or point to a {@link LostAndFoundRegion} that doesn't exist.
     * @param worldName The ProtectedRegion's world's name.
     * @param world The ProtectedRegion's world
     * @param maxLoc The cube region's maximum point.
     * @param minLoc The cube region's minimum point.
     */
    public ProtectedRegion(TPPets thisPlugin, String regionName, String enterMessage, String lfString, String worldName, World world, Location maxLoc, Location minLoc) {
        super(regionName, worldName, world, minLoc, maxLoc);
        this.lfName = lfString;
        this.lfReference = thisPlugin.getLostRegionManager().getLostRegion(lfString);
        this.enterMessage = ChatColor.translateAlternateColorCodes('&', enterMessage);
    }

    /**
     * Teleports the given entity into the center of the {@link LostAndFoundRegion} linked to this {@link ProtectedRegion}
     * @param entity The entity to teleport.
     * @return true if teleport successful, false if not.
     */
    public boolean tpToLostRegion(Entity entity) {
        EntityActions.setSitting(entity);
        entity.eject();
        if (this.lfReference != null && this.lfReference.getApproxCenter().getWorld() != null) {
            return entity.teleport(this.lfReference.getApproxCenter());
        }
        return false;
    }

    /**
     * Gets the region's enter message - a message sent to players who attempt to teleport a pet in a protected region
     * without permission/
     * @return The region's enter message.
     */
    public String getEnterMessage() {
        return enterMessage;
    }

    /**
     * Gets the region's {@link LostAndFoundRegion} object reference.
     * @return A reference to this {@link ProtectedRegion}'s linked {@link LostAndFoundRegion}.
     */
    public LostAndFoundRegion getLfReference() {
        return lfReference;
    }

    /**
     * Gets the {@link LostAndFoundRegion}'s name that this {@link ProtectedRegion} is to be linked to. This can be
     * empty, but not null.
     * @return The linked {@link LostAndFoundRegion}'s name.
     */
    public String getLfName() {
        return lfName;
    }

    /**
     * Updates this Protected Region's local lfReference property based on its lfName property. Uses {@link LostRegionManager#getLostRegion(String)}.
     * @param thisPlugin A reference to the active {@link TPPets} instance. Uses its {@link LostRegionManager} to get an
     *                  updated {@link LostAndFoundRegion} reference.
     */
    public void updateLFReference(TPPets thisPlugin) {
        this.lfReference = thisPlugin.getLostRegionManager().getLostRegion(this.lfName);
    }

    /**
     * Sets the {@link LostAndFoundRegion}'s name that this {@link ProtectedRegion} is to be linked to. This can be
     * empty, but not null.
     * @param lfName The new {@link LostAndFoundRegion}'s name.
     */
    public void setLfName(String lfName) {
        this.lfName = lfName;
    }
}
