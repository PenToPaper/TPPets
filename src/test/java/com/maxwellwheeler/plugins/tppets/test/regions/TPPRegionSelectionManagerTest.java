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

public class TPPRegionSelectionManagerTest {
    private RegionSelectionManager regionSelectionManager;
    private Player player;

    @BeforeEach
    public void beforeEach() {
        this.regionSelectionManager = new RegionSelectionManager();
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
    }

    @Test
    @DisplayName("RegionSelectionManager begins selection with start location")
    void regionSelectionManagerBeginsSelectionStart() {
        assertNull(this.regionSelectionManager.getSelectionSession(this.player));

        World world = mock(World.class);
        Location startLocation = new Location(world, 10, 20, 30);

        this.regionSelectionManager.setStartLocation(this.player, startLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertEquals(world, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
    }

    @Test
    @DisplayName("RegionSelectionManager begins selection with end location")
    void regionSelectionManagerBeginsSelectionEnd() {
        assertNull(this.regionSelectionManager.getSelectionSession(this.player));

        World world = mock(World.class);
        Location endLocation = new Location(world, 10, 20, 30);

        this.regionSelectionManager.setEndLocation(this.player, endLocation);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertEquals(world, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
    }

    @Test
    @DisplayName("RegionSelectionManager finishes selection with end location")
    void regionSelectionManagerFinishesSelectionWithEnd() {
        assertNull(this.regionSelectionManager.getSelectionSession(this.player));

        World world = mock(World.class);
        Location location = new Location(world, 10, 20, 30);

        this.regionSelectionManager.setStartLocation(this.player, location);
        this.regionSelectionManager.setEndLocation(this.player, location);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertEquals(world, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertTrue(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());
    }

    @Test
    @DisplayName("RegionSelectionManager finishes selection with start location")
    void regionSelectionManagerFinishesSelectionWithStart() {
        assertNull(this.regionSelectionManager.getSelectionSession(this.player));

        World world = mock(World.class);
        Location location = new Location(world, 10, 20, 30);

        this.regionSelectionManager.setEndLocation(this.player, location);
        this.regionSelectionManager.setStartLocation(this.player, location);
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.player));
        assertEquals(world, this.regionSelectionManager.getSelectionSession(this.player).getWorld());
        assertTrue(this.regionSelectionManager.getSelectionSession(this.player).isCompleteSelection());
    }

    @Test
    @DisplayName("RegionSelectionManager clears selections")
    void regionSelectionManagerClearsSelections() {
        World world = mock(World.class);
        Location endLocation = new Location(world, 10, 20, 30);

        this.regionSelectionManager.setEndLocation(this.player, endLocation);
        this.regionSelectionManager.clearPlayerSession(this.player);

        assertNull(this.regionSelectionManager.getSelectionSession(this.player));
    }
}
