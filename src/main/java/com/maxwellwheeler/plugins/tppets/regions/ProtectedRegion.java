package com.maxwellwheeler.plugins.tppets.regions;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Represents a protected region, a region where pets can't enter without the permission node "tppets.tpanywhere"
 * @author GatheringExp
 *
 */
public class ProtectedRegion extends Region {
    private String enterMessage;
    /**
     * The name of the {@link LostAndFoundRegion} that this Protected Region is set to link to. This can't be null, but can potentially be an empty string.
     */
    private String lfName;
    /**
     * The reference to the {@link LostAndFoundRegion} object that this Protected Region is linked to. This can be null.
     */
    private LostAndFoundRegion lfReference;
    
    /**
     * Gets an up-to-date reference to the {@link LostAndFoundRegion} linked from the given lfName property.
     * @param lfName The name of the {@link LostAndFoundRegion} to get the reference of.
     * @return A {@link LostAndFoundRegion} reference.
     */
    private LostAndFoundRegion getLfReference (TPPets thisPlugin, String lfName) {
        return thisPlugin.getLostRegion(lfName);
    }
    
    /**
     * Updates this Protected Region's local lfReference property based on its lfName property.
     */
    public void updateLFReference(TPPets thisPlugin) {
        this.lfReference = getLfReference(thisPlugin, lfName);
    }
    
    @Override
    public String toString() {
        return String.format("zoneName = %s; enterMessage = %s; worldName = %s; x1: %d; y1: %d; z1: %d; x2: %d; y2: %d; z2: %d", regionName, enterMessage, worldName, minLoc.getBlockX(), minLoc.getBlockY(), minLoc.getBlockZ(), maxLoc.getBlockX(), maxLoc.getBlockY(), maxLoc.getBlockZ());
    }
    
    /**
     * General constructor, used to recreate regions from database entries.
     * @param regionName The name of the Protected Region.
     * @param enterMessage The message displayed when a player tries to type a /tpp [dogs/cats/birds] command in the region. This supports &#38;[#] color codes.
     * @param worldName The name of the world the Protected Region is in.
     * @param xOne The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point's X location.
     * @param yOne The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point's Y location.
     * @param zOne The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point's Z location.
     * @param xTwo The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point's X location.
     * @param yTwo The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point's Y location.
     * @param zTwo The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point's Z location.
     * @param lfString A string representing the LostAndFound Region to be linked to this ProtectedRegion.
     */
    public ProtectedRegion(String regionName, String enterMessage, String worldName, int xOne, int yOne, int zOne, int xTwo, int yTwo, int zTwo, String lfString, TPPets thisPlugin) {
        this(regionName, enterMessage, worldName, new Location(Bukkit.getWorld(worldName), xOne, yOne, zOne), new Location(Bukkit.getWorld(worldName), xTwo, yTwo, zTwo), lfString, thisPlugin);
    }
    
    /**
     * General constructor, uses Location objects to represent the maximum and minimum of the cube.
     * @param regionName The name of the Protected Region.
     * @param enterMessage The message displayed when a player tries to type a /tpp [dogs/cats/birds] command in the region. This supports &#38;[#] color codes.
     * @param worldName The name of the world the Protected Region is in.
     * @param minLoc The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point.
     * @param maxLoc The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point.
     * @param lfString A string representing the LostAndFound Region to be linked to this ProtectedRegion.
     */
    public ProtectedRegion(String regionName, String enterMessage, String worldName, Location minLoc, Location maxLoc, String lfString, TPPets thisPlugin) {
        this(regionName, enterMessage, worldName, Bukkit.getWorld(worldName), minLoc, maxLoc, lfString, thisPlugin);
    }
    
    /**
     * General constructor, uses Location objects to represent the maximum and minimum of the cube.
     * @param regionName The name of the Protected Region.
     * @param enterMessage The message displayed when a player tries to type a /tpp [dogs/cats/birds] command in the region. This supports &#38;[#] color codes.
     * @param worldName The name of the world the Protected Region is in.
     * @param world A world reference representing the world where the Protected Region is in.
     * @param minLoc The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point.
     * @param maxLoc The Protected Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point.
     * @param lfString A string representing the LostAndFound Region to be linked to this ProtectedRegion.
     */
    public ProtectedRegion(String regionName, String enterMessage, String worldName, World world, Location minLoc, Location maxLoc, String lfString, TPPets thisPlugin) {
        this(regionName, enterMessage, worldName, world, minLoc, maxLoc, lfString);
        this.lfReference = getLfReference(thisPlugin, lfString);
    }

    public ProtectedRegion(String regionName, String enterMessage, String worldName, World world, Location minLoc, Location maxLoc, String lfString) {
        super(regionName, worldName, world, minLoc, maxLoc);
        this.enterMessage = ChatColor.translateAlternateColorCodes('&', enterMessage);
        this.lfName = lfString;
    }
    
    /**
     * Teleports the given entity into the center of the lost region that the Protected Region references.
     * @param ent The entity to teleport.
     */
    public boolean tpToLostRegion(Entity ent) {
        EntityActions.setSitting(ent);
        ent.eject();
        if (lfReference != null && lfReference.getApproxCenter().getWorld() != null) {
            return ent.teleport(lfReference.getApproxCenter());
        }
        return false;
    }

    public String getEnterMessage() {
        return enterMessage;
    }

    public LostAndFoundRegion getLfReference() {
        return lfReference;
    }
    
    public String getLfName() {
        return lfName;
    }
    
    public void setLfName(String lfString) {
        this.lfName = lfString;
    }
    
    /**
     * Directly sets the lfReference of the Protected Region.
     * @param lfString lfReference's name. This can be null.
     */
    public void setLfReference(TPPets thisPlugin, String lfString) {
        this.lfReference = lfString == null ? null : getLfReference(thisPlugin, lfString);
    }
}
