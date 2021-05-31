package com.maxwellwheeler.plugins.tppets.test;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.World;

public class ObjectFactory {
    public static LostAndFoundRegion getLostAndFoundRegion(String regionName, String worldName, World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Location minLoc = new Location(world, minX, minY, minZ);
        Location maxLoc = new Location(world, maxX, maxY, maxZ);
        return new LostAndFoundRegion(regionName, worldName, world, minLoc, maxLoc);
    }

    public static ProtectedRegion getProtectedRegion(String regionName, String enterMessage, String worldName, World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String lfString, TPPets tpPets) {
        Location minLoc = new Location(world, minX, minY, minZ);
        Location maxLoc = new Location(world, maxX, maxY, maxZ);
        return new ProtectedRegion(tpPets, regionName, enterMessage, lfString, worldName, world, maxLoc, minLoc);
    }
}
