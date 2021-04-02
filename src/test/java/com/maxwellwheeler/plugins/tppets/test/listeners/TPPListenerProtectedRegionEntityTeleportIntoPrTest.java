package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class TPPListenerProtectedRegionEntityTeleportIntoPrTest {
    private ProtectedRegion protectedRegion;
    private Player player;
    private Wolf wolf;
    private Location teleportingTo;
    private TPPets tpPets;
    private EntityTeleportEvent entityTeleportEvent;
    private ListenerProtectedRegion listenerProtectedRegion;

    @BeforeEach
    public void beforeEach() {
        World world = mock(World.class);
        this.protectedRegion = MockFactory.getProtectedRegion("ProtectedRegionName", "Enter Message", "MockWorldName", world, 100, 200, 300, 400, 500, 600, "LostAndFoundRegion", null);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", world, null, new String[]{});
        this.wolf = MockFactory.getTamedMockEntity("MockWolfId", org.bukkit.entity.Wolf.class, this.player);
        this.teleportingTo = MockFactory.getMockLocation(world, 100, 200, 300);
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false, false);
        this.entityTeleportEvent = mock(EntityTeleportEvent.class);
        this.listenerProtectedRegion = new ListenerProtectedRegion(this.tpPets);

        when(this.entityTeleportEvent.getEntity()).thenReturn(this.wolf);
        when(this.entityTeleportEvent.getTo()).thenReturn(this.teleportingTo);
        when(world.getName()).thenReturn("MockWorldName");
        when(this.tpPets.getProtectedRegionWithin(this.teleportingTo)).thenReturn(this.protectedRegion);
        when(this.tpPets.getVaultEnabled()).thenReturn(false);
    }

    @Test
    @DisplayName("Cancels teleport when moving into PR and owner doesn't have permission")
    void cancelsTeleport() {
        this.listenerProtectedRegion.entityTeleportIntoPr(this.entityTeleportEvent);

        verify(this.wolf, times(1)).setSitting(true);
        verify(this.entityTeleportEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't cancel teleport when pet isn't of valid type")
    void cannotCancelTeleportWhenPetInvalidType() {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        when(this.entityTeleportEvent.getEntity()).thenReturn(villager);

        this.listenerProtectedRegion.entityTeleportIntoPr(this.entityTeleportEvent);

        verify(this.entityTeleportEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't cancel teleport when pet isn't teleporting to protected region")
    void cannotCancelTeleportWhenPetNotGoingToPr() {
        when(this.tpPets.getProtectedRegionWithin(this.teleportingTo)).thenReturn(null);

        this.listenerProtectedRegion.entityTeleportIntoPr(this.entityTeleportEvent);

        verify(this.wolf, never()).setSitting(anyBoolean());
        verify(this.entityTeleportEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't cancel teleport when pet isn't teleporting to protected region with world")
    void cannotCancelTeleportWhenPetNotGoingToValidPr() {
        when(this.protectedRegion.getWorld()).thenReturn(null);

        this.listenerProtectedRegion.entityTeleportIntoPr(this.entityTeleportEvent);

        verify(this.wolf, never()).setSitting(anyBoolean());
        verify(this.entityTeleportEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't cancel teleport when pet owner is online and has permission")
    void cannotCancelTeleportWhenOnlineOwnerHasPermission() {
        when(this.player.hasPermission("tppets.tpanywhere")).thenReturn(true);

        this.listenerProtectedRegion.entityTeleportIntoPr(this.entityTeleportEvent);

        verify(this.wolf, never()).setSitting(anyBoolean());
        verify(this.entityTeleportEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't cancel teleport when pet owner is offline and plugin has vault and has permission")
    void cannotCancelTeleportWhenOfflineOwnerHasPermission() {
        OfflinePlayer offlinePlayer = MockFactory.getMockOfflinePlayer("MockOfflinePlayerId", "MockOfflinePlayerName");
        Permission permission = mock(Permission.class);
        when(this.wolf.getOwner()).thenReturn(offlinePlayer);
        when(this.tpPets.getVaultEnabled()).thenReturn(true);
        when(this.tpPets.getPerms()).thenReturn(permission);
        when(permission.playerHas("MockWorldName", offlinePlayer, "tppets.tpanywhere")).thenReturn(true);

        this.listenerProtectedRegion.entityTeleportIntoPr(this.entityTeleportEvent);

        verify(this.wolf, never()).setSitting(anyBoolean());
        verify(this.entityTeleportEvent, never()).setCancelled(anyBoolean());
    }
}
