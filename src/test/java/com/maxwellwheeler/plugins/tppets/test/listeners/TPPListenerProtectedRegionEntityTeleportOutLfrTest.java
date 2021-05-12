package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.LostRegionManager;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegionManager;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class TPPListenerProtectedRegionEntityTeleportOutLfrTest {
    private Wolf wolf;
    private Location teleportingFrom;
    private EntityTeleportEvent entityTeleportEvent;
    private ProtectedRegionManager protectedRegionManager;
    private ListenerProtectedRegion listenerProtectedRegion;
    private LostRegionManager lostRegionManager;

    @BeforeEach
    public void beforeEach() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.wolf = MockFactory.getTamedMockEntity("MockWolfId", org.bukkit.entity.Wolf.class, player);
        this.teleportingFrom = MockFactory.getMockLocation(null, 100, 200, 300);
        this.entityTeleportEvent = mock(EntityTeleportEvent.class);
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.listenerProtectedRegion = new ListenerProtectedRegion(tpPets);
        this.protectedRegionManager = mock(ProtectedRegionManager.class);

        this.lostRegionManager = mock(LostRegionManager.class);
        when(tpPets.getLostRegionManager()).thenReturn(this.lostRegionManager);
        when(tpPets.getProtectedRegionManager()).thenReturn(this.protectedRegionManager);
        when(this.entityTeleportEvent.getFrom()).thenReturn(this.teleportingFrom);
        when(this.entityTeleportEvent.getEntity()).thenReturn(this.wolf);
        when(this.protectedRegionManager.getProtectedRegionAt(this.teleportingFrom)).thenReturn(mock(ProtectedRegion.class));
    }

    @Test
    @DisplayName("Cancels teleport when moving out of LFR")
    void cancelsTeleport() {
        when(this.lostRegionManager.getLostRegionAt(this.entityTeleportEvent.getFrom())).thenReturn(mock(LostAndFoundRegion.class));

        this.listenerProtectedRegion.entityTeleportOutLfr(this.entityTeleportEvent);

        verify(this.wolf, times(1)).setSitting(true);
        verify(this.entityTeleportEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't cancel teleport when pet is not of valid type")
    void cannotCancelTeleportWhenPetInvalidType() {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        when(this.entityTeleportEvent.getEntity()).thenReturn(villager);

        this.listenerProtectedRegion.entityTeleportOutLfr(this.entityTeleportEvent);

        verify(this.entityTeleportEvent, never()).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't cancel teleport when pet is not in lost region")
    void cannotCancelTeleportWhenPetNotInLfr() {
        when(this.protectedRegionManager.getProtectedRegionAt(this.teleportingFrom)).thenReturn(null);

        this.listenerProtectedRegion.entityTeleportOutLfr(this.entityTeleportEvent);

        verify(this.entityTeleportEvent, never()).setCancelled(true);
    }
}
