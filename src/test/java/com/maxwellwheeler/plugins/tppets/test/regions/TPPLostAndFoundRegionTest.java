package com.maxwellwheeler.plugins.tppets.test.regions;

import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

public class TPPLostAndFoundRegionTest {
    private World world;
    private LostAndFoundRegion lostAndFoundRegion;

    @BeforeEach
    public void beforeEach() {
        this.world = mock(World.class);
        Location min = new Location(this.world, 10, 20, 30);
        Location max = new Location(this.world, 20, 30, 40);
        this.lostAndFoundRegion = new LostAndFoundRegion("LFRName", "WorldName", this.world, min, max);
    }

    @Test
    @DisplayName("getApproxCenter appropriately finds the center of the min and max locations")
    void getApproxCenterTest() {
        Location center = this.lostAndFoundRegion.getApproxCenter();
        assertEquals(this.world, center.getWorld());
        assertEquals(15, center.getBlockX());
        assertEquals(25, center.getBlockY());
        assertEquals(35, center.getBlockZ());
    }

    @Test
    @DisplayName("toString displays properly")
    void toStringTest() {
        assertEquals("zoneName = LFRName; worldName = WorldName; x1: 10; y1: 20; z1: 30; x2: 20; y2: 30; z2: 40", this.lostAndFoundRegion.toString());
    }

    @Test
    @DisplayName("isInRegion functions properly")
    void isInRegionTest() {
        // True
        Location isInRegion1 = new Location(this.world, 15, 25, 35);
        Location isInRegion2 = new Location(this.world, 10, 20, 30);
        Location isInRegion3 = new Location(this.world, 20, 30, 40);

        assertTrue(this.lostAndFoundRegion.isInRegion(isInRegion1));
        assertTrue(this.lostAndFoundRegion.isInRegion(isInRegion2));
        assertTrue(this.lostAndFoundRegion.isInRegion(isInRegion3));

        // False, in same world
        Location isNotInRegion1 = new Location(this.world, 9, 25, 35);
        Location isNotInRegion2 = new Location(this.world, 15, 19, 35);
        Location isNotInRegion3 = new Location(this.world, 15, 25, 29);

        assertFalse(this.lostAndFoundRegion.isInRegion(isNotInRegion1));
        assertFalse(this.lostAndFoundRegion.isInRegion(isNotInRegion2));
        assertFalse(this.lostAndFoundRegion.isInRegion(isNotInRegion3));

        // False in different world
        World world = mock(World.class);
        Location isInDifferentWorld = new Location(world, 15, 25, 35);

        assertFalse(this.lostAndFoundRegion.isInRegion(isInDifferentWorld));
    }

    @Test
    @DisplayName("Constructor gets the bukkit world if supplied through string")
    void constructorGetsWorld() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            World world = mock(World.class);
            bukkit.when(() -> Bukkit.getWorld("WorldName")).thenReturn(world);

            this.lostAndFoundRegion = new LostAndFoundRegion("LFRName", "WorldName", 10, 20, 30, 40, 50, 60);

            assertEquals(world, this.lostAndFoundRegion.getWorld());
        }
    }
}
