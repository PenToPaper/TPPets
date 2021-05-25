package com.maxwellwheeler.plugins.tppets.test.regions;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.LostRegionManager;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Wolf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPProtectedRegionTest {
    private World world;
    private ProtectedRegion protectedRegion;
    private TPPets tpPets;
    private LostRegionManager lostRegionManager;

    @BeforeEach
    public void beforeEach() {
        this.world = mock(World.class);
        Location min = new Location(this.world, 10, 20, 30);
        Location max = new Location(this.world, 20, 30, 40);

        this.tpPets = mock(TPPets.class);

        this.lostRegionManager = mock(LostRegionManager.class);
        LostAndFoundRegion lostAndFoundRegion = new LostAndFoundRegion("LFRName", "WorldName", this.world, min, max);

        when(this.tpPets.getLostRegionManager()).thenReturn(this.lostRegionManager);
        when(this.lostRegionManager.getLostRegion("LFRName")).thenReturn(lostAndFoundRegion);

        this.protectedRegion = new ProtectedRegion("PRName", "EnterMessage", "WorldName", this.world, min, max, "LFRName", this.tpPets);
    }

    @Test
    @DisplayName("tpToLostRegion teleports valid pets to valid regions")
    void tpToLostRegion() {
        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        Wolf wolf = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Wolf.class);
        when(wolf.teleport(any(Location.class))).thenReturn(true);

        assertTrue(this.protectedRegion.tpToLostRegion(wolf));
        verify(wolf, times(1)).setSitting(true);
        verify(wolf, times(1)).eject();
        verify(wolf, times(1)).teleport(locationCaptor.capture());
        Location lfr = locationCaptor.getValue();
        assertEquals(this.world, lfr.getWorld());
        assertEquals(15, lfr.getBlockX());
        assertEquals(25, lfr.getBlockY());
        assertEquals(35, lfr.getBlockZ());

    }

    @Test
    @DisplayName("tpToLostRegion doesn't teleport pets when lfReference is null")
    void tpToLostRegionLfNull() {
        Wolf wolf = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Wolf.class);
        when(wolf.teleport(any(Location.class))).thenReturn(true);

        when(this.lostRegionManager.getLostRegion("LFRName")).thenReturn(null);
        this.protectedRegion.updateLFReference(this.tpPets);

        assertFalse(this.protectedRegion.tpToLostRegion(wolf));
        verify(wolf, times(1)).setSitting(true);
        verify(wolf, times(1)).eject();
        verify(wolf, never()).teleport(any(Location.class));
    }

    @Test
    @DisplayName("tpToLostRegion doesn't teleport pets when lfReference's center is not in a world")
    void tpToLostRegionLfNoCenter() {
        LostAndFoundRegion noCenterLFR = mock(LostAndFoundRegion.class);
        Location worldLess = new Location(null, 10, 20, 30);
        when(noCenterLFR.getApproxCenter()).thenReturn(worldLess);
        when(this.lostRegionManager.getLostRegion("LFRName")).thenReturn(noCenterLFR);

        this.protectedRegion.updateLFReference(this.tpPets);

        Wolf wolf = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Wolf.class);
        when(wolf.teleport(any(Location.class))).thenReturn(true);

        assertFalse(this.protectedRegion.tpToLostRegion(wolf));
        verify(wolf, times(1)).setSitting(true);
        verify(wolf, times(1)).eject();
        verify(wolf, never()).teleport(any(Location.class));
    }

    @Test
    @DisplayName("isInRegion functions properly")
    void isInRegionTest() {
        // True
        Location isInRegion1 = new Location(this.world, 15, 25, 35);
        Location isInRegion2 = new Location(this.world, 10, 20, 30);
        Location isInRegion3 = new Location(this.world, 20, 30, 40);

        assertTrue(this.protectedRegion.isInRegion(isInRegion1));
        assertTrue(this.protectedRegion.isInRegion(isInRegion2));
        assertTrue(this.protectedRegion.isInRegion(isInRegion3));

        // False, in same world
        Location isNotInRegion1 = new Location(this.world, 9, 25, 35);
        Location isNotInRegion2 = new Location(this.world, 15, 19, 35);
        Location isNotInRegion3 = new Location(this.world, 15, 25, 29);

        assertFalse(this.protectedRegion.isInRegion(isNotInRegion1));
        assertFalse(this.protectedRegion.isInRegion(isNotInRegion2));
        assertFalse(this.protectedRegion.isInRegion(isNotInRegion3));

        // False in different world
        World world = mock(World.class);
        Location isInDifferentWorld = new Location(world, 15, 25, 35);

        assertFalse(this.protectedRegion.isInRegion(isInDifferentWorld));
    }

    @Test
    @DisplayName("Constructor gets the bukkit world if supplied through string")
    void constructorGetsWorld() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            World world = mock(World.class);
            bukkit.when(() -> Bukkit.getWorld("WorldName")).thenReturn(world);

            this.protectedRegion = new ProtectedRegion("PRName", "EnterMessage", "WorldName", 10, 20, 30, 40, 50, 60, "LFRName", this.tpPets);

            assertEquals(world, this.protectedRegion.getWorld());
        }
    }
}
