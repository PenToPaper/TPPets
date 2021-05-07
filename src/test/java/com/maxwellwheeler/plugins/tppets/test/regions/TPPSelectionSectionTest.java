package com.maxwellwheeler.plugins.tppets.test.regions;

import com.maxwellwheeler.plugins.tppets.regions.RegionSelectionManager;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class TPPSelectionSectionTest {
    private RegionSelectionManager regionSelectionManager;
    private Player player;

    @BeforeEach
    public void beforeEach() {
        this.regionSelectionManager = new RegionSelectionManager();
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
    }

    @Test
    @DisplayName("SelectionSession begins incomplete selection with only start location")
    void selectionSessionShowsIncompleteWithOnlySectionStart() {
        World world = mock(World.class);
        Location startLocation = new Location(world, 10, 20, 30);

        this.regionSelectionManager.setStartLocation(this.player, startLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertEquals(world, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertFalse(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());
    }

    @Test
    @DisplayName("SelectionSession begins incomplete selection with only end location")
    void selectionSessionShowsIncompleteWithOnlySectionEnd() {
        World world = mock(World.class);
        Location endLocation = new Location(world, 10, 20, 30);

        this.regionSelectionManager.setEndLocation(this.player, endLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertEquals(world, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertFalse(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());
    }

    @Test
    @DisplayName("SelectionSession shows complete selection")
    void selectionSessionShowsComplete() {
        World world = mock(World.class);
        Location startLocation = new Location(world, 10, 20, 30);
        Location endLocation = new Location(world, 20, 30, 40);

        this.regionSelectionManager.setEndLocation(this.player, startLocation);
        this.regionSelectionManager.setStartLocation(this.player, endLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertEquals(world, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertTrue(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());
    }

    @Test
    @DisplayName("SelectionSession computes minimum and maximum locations when start location higher than end")
    void selectionSessionMinMaxStartHigherThanEnd() {
        World world = mock(World.class);
        Location startLocation = new Location(world, 20, 30, 40);
        Location endLocation = new Location(world, 10, 20, 30);

        this.regionSelectionManager.setEndLocation(this.player, startLocation);
        this.regionSelectionManager.setStartLocation(this.player, endLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));

        Location min = this.regionSelectionManager.getSelectionSession(this.player).getMinimumLocation();
        assertEquals(world, min.getWorld());
        assertEquals(10, min.getBlockX());
        assertEquals(20, min.getBlockY());
        assertEquals(30, min.getBlockZ());

        Location max = this.regionSelectionManager.getSelectionSession(this.player).getMaximumLocation();
        assertEquals(world, max.getWorld());
        assertEquals(20, max.getBlockX());
        assertEquals(30, max.getBlockY());
        assertEquals(40, max.getBlockZ());
    }

    @Test
    @DisplayName("SelectionSession computes minimum and maximum locations when start location lower than end")
    void selectionSessionMinMaxStartLowerThanEnd() {
        World world = mock(World.class);
        Location startLocation = new Location(world, 10, 20, 30);
        Location endLocation = new Location(world, 20, 30, 40);

        this.regionSelectionManager.setEndLocation(this.player, startLocation);
        this.regionSelectionManager.setStartLocation(this.player, endLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));

        Location min = this.regionSelectionManager.getSelectionSession(this.player).getMinimumLocation();
        assertEquals(world, min.getWorld());
        assertEquals(10, min.getBlockX());
        assertEquals(20, min.getBlockY());
        assertEquals(30, min.getBlockZ());

        Location max = this.regionSelectionManager.getSelectionSession(this.player).getMaximumLocation();
        assertEquals(world, max.getWorld());
        assertEquals(20, max.getBlockX());
        assertEquals(30, max.getBlockY());
        assertEquals(40, max.getBlockZ());
    }

    @Test
    @DisplayName("SelectionSession computes minimum and maximum locations when start location mixed compared to end")
    void selectionSessionMinMaxStartMixedToEnd() {
        World world = mock(World.class);
        Location startLocation = new Location(world, 10, 30, 30);
        Location endLocation = new Location(world, 20, 20, 40);

        this.regionSelectionManager.setEndLocation(this.player, endLocation);
        this.regionSelectionManager.setStartLocation(this.player, startLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));

        Location min = this.regionSelectionManager.getSelectionSession(this.player).getMinimumLocation();
        assertEquals(world, min.getWorld());
        assertEquals(10, min.getBlockX());
        assertEquals(20, min.getBlockY());
        assertEquals(30, min.getBlockZ());

        Location max = this.regionSelectionManager.getSelectionSession(this.player).getMaximumLocation();
        assertEquals(world, max.getWorld());
        assertEquals(20, max.getBlockX());
        assertEquals(30, max.getBlockY());
        assertEquals(40, max.getBlockZ());
    }

    @Test
    @DisplayName("SelectionSession min and max location return null if selection not complete")
    void selectionSessionMinMaxNullIncompleteSelection() {
        World world = mock(World.class);
        Location startLocation = new Location(world, 10, 30, 30);

        this.regionSelectionManager.setStartLocation(this.player, startLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertNull(this.regionSelectionManager.getSelectionSession(this.player).getMaximumLocation());
        assertNull(this.regionSelectionManager.getSelectionSession(this.player).getMinimumLocation());
    }

    @Test
    @DisplayName("SelectionSession refreshes start location and world if end location has different world")
    void selectionSessionRefreshesWhenStartDifferentWorld() {
        World startWorld = mock(World.class);
        Location startLocation = new Location(startWorld, 10, 30, 30);
        World endWorld = mock(World.class);
        Location endLocation = new Location(endWorld, 20, 20, 40);

        this.regionSelectionManager.setEndLocation(this.player, endLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertEquals(endWorld, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertNotEquals(startWorld, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertFalse(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());

        this.regionSelectionManager.setStartLocation(this.player, startLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertNotEquals(endWorld, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertEquals(startWorld, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertFalse(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());
    }

    @Test
    @DisplayName("SelectionSession refreshes end location and world if start location has different world")
    void selectionSessionRefreshesWhenEndDifferentWorld() {
        World startWorld = mock(World.class);
        Location startLocation = new Location(startWorld, 10, 30, 30);
        World endWorld = mock(World.class);
        Location endLocation = new Location(endWorld, 20, 20, 40);

        this.regionSelectionManager.setStartLocation(this.player, startLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertEquals(startWorld, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertNotEquals(endWorld, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertFalse(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());

        this.regionSelectionManager.setEndLocation(this.player, endLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertNotEquals(startWorld, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertEquals(endWorld, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertFalse(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());
    }

    @Test
    @DisplayName("SelectionSession refreshes locations if setWorld is different world")
    void selectionSessionRefreshesWhenSettingDifferentWorld() {
        World world = mock(World.class);
        Location startLocation = new Location(world, 10, 30, 30);
        Location endLocation = new Location(world, 20, 20, 40);

        this.regionSelectionManager.setStartLocation(this.player, startLocation);
        this.regionSelectionManager.setEndLocation(this.player, endLocation);
        assertTrue(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());

        World setWorld = mock(World.class);
        this.regionSelectionManager.getSelectionSession(this.player).setWorld(setWorld);
        assertFalse(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());
        assertEquals(setWorld, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
    }

    @Test
    @DisplayName("SelectionSession doesn't set locations without a world")
    void selectionSessionDoesNotSetLocationsWithoutWorld() {
        // End no world
        World world = mock(World.class);
        Location startLocation = new Location(world, 10, 30, 30);
        Location endLocation = new Location(null, 20, 20, 40);

        this.regionSelectionManager.setStartLocation(this.player, startLocation);
        this.regionSelectionManager.setEndLocation(this.player, endLocation);
        assertFalse(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());

        this.regionSelectionManager.clearPlayerSession(this.player);

        // Start no world

        Location startLocation1 = new Location(null, 10, 30, 30);
        Location endLocation1 = new Location(world, 20, 20, 40);

        this.regionSelectionManager.setEndLocation(this.player, endLocation1);
        this.regionSelectionManager.setStartLocation(this.player, startLocation1);
        assertFalse(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());

        this.regionSelectionManager.clearPlayerSession(this.player);
    }
}
