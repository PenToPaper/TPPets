import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TeleportMocksFactory {
    public static Entity getMockEntity(String petID, Class<? extends Entity> entityClass) {
        Entity mockEntity = mock(entityClass);
        UUID entityUUID = mock(UUID.class);
        when(entityUUID.toString()).thenReturn(petID);
        when(mockEntity.getUniqueId()).thenReturn(entityUUID);
        when(mockEntity.getPassengers()).thenReturn(null);

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

    public static TPPets getMockPlugin(DBWrapper dbWrapper, LogWrapper logWrapper, boolean canTpThere, boolean allowTpBetweenWorlds, boolean isAllowedToPet) {
        TPPets tpPets = mock(TPPets.class);
        when(tpPets.getDatabase()).thenReturn(dbWrapper);
        when(tpPets.canTpThere(any())).thenReturn(canTpThere);
        when(tpPets.getAllowTpBetweenWorlds()).thenReturn(allowTpBetweenWorlds);
        when(tpPets.getLogWrapper()).thenReturn(logWrapper);
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
}
