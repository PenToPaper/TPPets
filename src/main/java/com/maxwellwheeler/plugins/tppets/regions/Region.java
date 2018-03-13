package com.maxwellwheeler.plugins.tppets.regions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.TPPets;

/**
 * Abstract class for {@link ProtectedRegion} and {@link LostAndFoundRegion}
 * @author GatheringExp
 *
 */
public abstract class Region {
    protected String zoneName;
    protected String worldName;
    protected World world;
    protected Location minLoc;
    protected Location maxLoc;
    protected TPPets thisPlugin;
    
    /**
     * A general constructor, used primarily for regeneration of the regions from databases.
     * @param zoneName The name of the zone.
     * @param worldName The name of the world the zone is in. If worldName points to a non-existent world, world will be null but worldName will be what the world was.
     * @param xOne The Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point's X location.
     * @param yOne The Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point's Y location.
     * @param zOne The Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point's Z location.
     * @param xTwo The Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point's X location.
     * @param yTwo The Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point's Y location.
     * @param zTwo The Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point's Z location.
     */
    public Region(String zoneName, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this(zoneName, worldName, Bukkit.getServer().getWorld(worldName), new Location(Bukkit.getServer().getWorld(worldName), minX, minY, minZ), new Location(Bukkit.getServer().getWorld(worldName), maxX, maxY, maxZ));
    }
    
    /**
     * The same style of constructor as Region(String zoneName, String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ), but with the coordinates represented by Location objects that include the world.
     * @param zoneName The name of the zone.
     * @param worldName The name of the world the zone is in. If worldName points to a non-existent world, world will be null but worldName will be what the world was.
     * @param minLoc The Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point.
     * @param maxLoc The Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point.
     */
    public Region(String zoneName, String worldName, Location minLoc, Location maxLoc) {
        this(zoneName, worldName, Bukkit.getServer().getWorld(worldName), minLoc, maxLoc);
    }
    
    /**
     * Same style of constructor as Region(String zoneName, String worldName, Location minLoc, Location maxLoc), but with the World reference explicitly made.
     * @param zoneName The name of the zone.
     * @param worldName The name of the world the zone is in. If worldName points to a non-existent world, world will be null but worldName will be what the world was.
     * @param world An explicit reference to the world the region is in.
     * @param minLoc The Region is generated based on two points: the minimum and maximum of the cube. This is the minimum point.
     * @param maxLoc The Region is generated based on two points: the minimum and maximum of the cube. This is the maximum point.
     */
    public Region(String zoneName, String worldName, World world, Location minLoc, Location maxLoc) {
        this.zoneName = zoneName;
        this.worldName = worldName;
        this.world = world;
        this.minLoc = minLoc;
        this.maxLoc = maxLoc;
        this.thisPlugin = (TPPets) Bukkit.getServer().getPluginManager().getPlugin("TPPets");
    }
    
    /**
     * Local utility function to test if argument middle is between minimum and maximum. Includes the endpoints.
     * @param min The minimum value of comparison.
     * @param middle The number being evaluated.
     * @param max The maximum value of comparison.
     * @return True if middle is between or including max and min.
     */
    private boolean isBetween(int min, int middle, int max) {
        return (middle >= min && middle <= max);
    }
    
    /**
     * Checks if a player is within the region instance.
     * @param pl The player to check
     * @return True if player is within the region instance, false otherwise.
     */
    public boolean isInZone(Player pl) {
        return isInZone(pl.getLocation());
    }
    
    /**
     * Checks if a location is within the region instance.
     * @param lc The location to check
     * @return True if location is within the region instance, false otherwise.
     */
    public boolean isInZone(Location lc) {
        return (minLoc.getWorld() != null && maxLoc.getWorld() != null && (lc.getWorld().equals(minLoc.getWorld()) && isBetween(minLoc.getBlockX(), lc.getBlockX(), maxLoc.getBlockX()) && isBetween(minLoc.getBlockY(), lc.getBlockY(), maxLoc.getBlockY()) && isBetween(minLoc.getBlockZ(), lc.getBlockZ(), maxLoc.getBlockZ())));
    }
    
    /**
     * Implementing regions need a way to communicate with the log file.
     */
    public abstract String toString();

    public String getZoneName() {
        return zoneName;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public World getWorld() {
        return world;
    }
    
    public Location getMinLoc() {
        return minLoc;
    }
    
    public Location getMaxLoc() {
        return maxLoc;
    }

    protected TPPets getPlugin() {
        return thisPlugin;
    }
}
