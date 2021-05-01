package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TPPPermissionCheckerTest {

    @Test
    @DisplayName("onlineHasPerms test")
    void onlineHasPerms() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"MockPermission"});
        OfflinePlayer offlinePlayer = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");

        assertTrue(PermissionChecker.onlineHasPerms(player, "MockPermission"));
        assertFalse(PermissionChecker.onlineHasPerms(player, "NotMockPermission"));
        assertFalse(PermissionChecker.onlineHasPerms(offlinePlayer, "MockPermission"));
    }

    @Test
    @DisplayName("offlineHasPerms test")
    void offlineHasPerms() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("MockWorld");

        OfflinePlayer offlinePlayer = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
        HumanEntity humanEntity = mock(HumanEntity.class);

        Permission permission = mock(Permission.class);
        when(permission.playerHas("MockWorld", offlinePlayer, "MockPermission")).thenReturn(true);

        TPPets tpPets = mock(TPPets.class);
        when(tpPets.getVaultEnabled()).thenReturn(true);
        when(tpPets.getPerms()).thenReturn(permission);

        assertTrue(PermissionChecker.offlineHasPerms(offlinePlayer, "MockPermission", world, tpPets));

        assertFalse(PermissionChecker.offlineHasPerms(offlinePlayer, "MockPermission", null, tpPets));

        when(tpPets.getVaultEnabled()).thenReturn(false);
        assertFalse(PermissionChecker.offlineHasPerms(offlinePlayer, "MockPermission", world, tpPets));
        when(tpPets.getVaultEnabled()).thenReturn(true);

        assertFalse(PermissionChecker.offlineHasPerms(humanEntity, "MockPermission", world, tpPets));

        when(permission.playerHas("MockWorld", offlinePlayer, "MockPermission")).thenReturn(false);
        assertFalse(PermissionChecker.offlineHasPerms(offlinePlayer, "MockPermission", world, tpPets));
    }

    @Test
    @DisplayName("hasPermissionToTeleportType test")
    void hasPermissionToTeleportType() {
        Player playerHas = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.dogs", "tppets.cats", "tppets.parrots", "tppets.mules", "tppets.llamas", "tppets.donkeys", "tppets.horses", "tppets.unknowns"});
        Player playerLacks = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});

        for (PetType.Pets petType : PetType.Pets.values()) {
            assertTrue(PermissionChecker.hasPermissionToTeleportType(petType, playerHas));
            assertFalse(PermissionChecker.hasPermissionToTeleportType(petType, playerLacks));
        }
    }
}
