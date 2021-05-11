package com.maxwellwheeler.plugins.tppets.test;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.*;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockFactory {
    public static <T extends Entity> T getMockEntity(String petID, Class<T> entityClass) {
        T mockEntity = mock(entityClass);
        UUID entityUUID = mock(UUID.class);
        when(entityUUID.toString()).thenReturn(petID);
        when(mockEntity.getUniqueId()).thenReturn(entityUUID);
        when(mockEntity.getPassengers()).thenReturn(null);

        return mockEntity;
    }

    public static <T extends Tameable> T getTamedMockEntity(String petID, Class<T> entityClass, OfflinePlayer owner) {
        T mockEntity = getMockEntity(petID, entityClass);
        when(mockEntity.isTamed()).thenReturn(true);
        when(mockEntity.getOwner()).thenReturn(owner);

        return mockEntity;
    }

    public static Location getMockLocation(World world, int x, int y, int z) {
        Location loc = mock(Location.class);
        when(loc.getX()).thenReturn((double) x);
        when(loc.getY()).thenReturn((double) y);
        when(loc.getZ()).thenReturn((double) z);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlockX()).thenReturn(x);
        when(loc.getBlockY()).thenReturn(y);
        when(loc.getBlockZ()).thenReturn(z);

        return loc;
    }

    public static TPPets getMockPlugin(SQLWrapper sqlWrapper, LogWrapper logWrapper, boolean canTpThere, boolean allowTpBetweenWorlds, boolean isAllowedToPet) {
        TPPets tpPets = mock(TPPets.class);
        when(tpPets.getDatabase()).thenReturn(sqlWrapper);
        when(tpPets.canTpThere(any(Player.class), any(Location.class))).thenReturn(canTpThere);
        when(tpPets.getAllowTpBetweenWorlds()).thenReturn(allowTpBetweenWorlds);
        if (logWrapper != null) {
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);
        }
        when(tpPets.isAllowedToPet(anyString(), anyString())).thenReturn(isAllowedToPet);

        return tpPets;
    }

    public static Player getMockPlayer(String playerID, String playerName, World playerWorld, Location playerLocation, String[] permissions) {
        Player player = mock(Player.class);
        UUID playerUUID = mock(UUID.class);
        when(playerUUID.toString()).thenReturn(playerID);
        when(player.getUniqueId()).thenReturn(playerUUID);
        when(player.hasPlayedBefore()).thenReturn(true);
        if (playerLocation != null) {
            when(player.getLocation()).thenReturn(playerLocation);
        }
        if (playerWorld != null) {
            when(player.getWorld()).thenReturn(playerWorld);
        }
        when(player.getName()).thenReturn(playerName);
        when(player.hasPermission(anyString())).thenReturn(false);
        for (String permission : permissions) {
            when(player.hasPermission(permission)).thenReturn(true);
        }

        return player;
    }

    public static OfflinePlayer getMockOfflinePlayer(String playerId, String playerName) {
        OfflinePlayer player = mock(OfflinePlayer.class);
        UUID playerUUID = mock(UUID.class);
        when(playerUUID.toString()).thenReturn(playerId);
        when(player.getUniqueId()).thenReturn(playerUUID);
        when(player.hasPlayedBefore()).thenReturn(true);
        when(player.getName()).thenReturn(playerName);

        return player;
    }

    public static PlayerStorageLocation getPlayerStorageLocation(String locationName, String userId, int x, int y, int z, World world) {
        PlayerStorageLocation storage = mock(PlayerStorageLocation.class);
        mockStorageLocation(storage, locationName, x, y, z, world);
        when(storage.getUserId()).thenReturn(userId);
        return storage;
    }

    public static ServerStorageLocation getServerStorageLocation(String locationName, int x, int y, int z, World world) {
        ServerStorageLocation storage = mock(ServerStorageLocation.class);
        mockStorageLocation(storage, locationName, x, y, z, world);
        return storage;
    }

    private static void mockStorageLocation(StorageLocation storageLocation, String locationName, int x, int y, int z, World world) {
        Location location = mock(Location.class);
        when(storageLocation.getStorageName()).thenReturn(locationName);
        when(storageLocation.getEffectiveStorageName()).thenReturn(locationName.toLowerCase());
        when(storageLocation.getLoc()).thenReturn(location);
        when(location.getBlockX()).thenReturn(x);
        when(location.getBlockY()).thenReturn(y);
        when(location.getBlockZ()).thenReturn(z);
        when(location.getWorld()).thenReturn(world);
    }

    public static ProtectedRegion getProtectedRegion(String regionName, String enterMessage, String worldName, World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, String lfString, LostAndFoundRegion lfReference) {
        ProtectedRegion protectedRegion = mock(ProtectedRegion.class);
        when(protectedRegion.getRegionName()).thenReturn(regionName);
        when(protectedRegion.getEnterMessage()).thenReturn(enterMessage);
        when(protectedRegion.getWorldName()).thenReturn(worldName);
        when(protectedRegion.getWorld()).thenReturn(world);
        when(protectedRegion.getLfName()).thenReturn(lfString);
        when(protectedRegion.getLfReference()).thenReturn(lfReference);

        Location minLocMock = MockFactory.getMockLocation(world, minX, minY, minZ);
        Location maxLocMock = MockFactory.getMockLocation(world, maxX, maxY, maxZ);

        when(protectedRegion.getMinLoc()).thenReturn(minLocMock);
        when(protectedRegion.getMaxLoc()).thenReturn(maxLocMock);

        return protectedRegion;
    }
}
