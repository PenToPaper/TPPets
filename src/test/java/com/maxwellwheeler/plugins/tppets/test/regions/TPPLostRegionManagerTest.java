package com.maxwellwheeler.plugins.tppets.test.regions;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.LostRegionManager;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TPPLostRegionManagerTest {
    private LostRegionManager lostRegionManager;
    private LostAndFoundRegion lostRegion1;
    private World world;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false, false);
        this.world = mock(World.class);

        Hashtable<String, LostAndFoundRegion> lostRegions = new Hashtable<>();
        this.lostRegion1 = new LostAndFoundRegion("LostRegion1", "MockWorldName", this.world, new Location(this.world, 100, 200, 300), new Location(this.world, 200, 300, 400));
        lostRegions.put("LostRegion1", this.lostRegion1);
        when(sqlWrapper.getLostRegions()).thenReturn(lostRegions);

        this.lostRegionManager = new LostRegionManager(tpPets);
        when(tpPets.getLostRegionManager()).thenReturn(this.lostRegionManager);
    }

    @Test
    @DisplayName("LostRegionManager getProtectedRegion returns null if no region with name exists")
    void protectedRegionManagerGetProtectedRegionReturnsNull() {
        assertNull(this.lostRegionManager.getLostRegion("InvalidName"));
    }

    @Test
    @DisplayName("LostRegionManager initializes based on database")
    void lostRegionManagerInitializes() {
        assertEquals(1, this.lostRegionManager.getLostRegions().size());
        assertEquals(this.lostRegion1, this.lostRegionManager.getLostRegion("LostRegion1"));
    }

    @Test
    @DisplayName("LostRegionManager adds new regions")
    void lostRegionManagerAddsRegions() {
        LostAndFoundRegion lostRegion2 = new LostAndFoundRegion("LostRegion2", "MockWorldName", this.world, new Location(this.world, 100, 200, 300), new Location(this.world, 200, 300, 400));

        this.lostRegionManager.addLostRegion(lostRegion2);

        assertEquals(2, this.lostRegionManager.getLostRegions().size());
        assertEquals(this.lostRegion1, this.lostRegionManager.getLostRegion("LostRegion1"));
        assertEquals(lostRegion2, this.lostRegionManager.getLostRegion("LostRegion2"));
    }

    @Test
    @DisplayName("LostRegionManager removes existing regions")
    void lostRegionManagerRemovesRegions() {
        this.lostRegionManager.removeLostRegion("LostRegion1");

        assertEquals(0, this.lostRegionManager.getLostRegions().size());
        assertNull(this.lostRegionManager.getLostRegion("LostRegion1"));
    }

    @Test
    @DisplayName("LostRegionManager finds protected regions at locations")
    void lostRegionManagerFindsRegionAtLocation() {
        // In protected region
        assertEquals(this.lostRegion1, this.lostRegionManager.getLostRegionAt(new Location(this.world, 101, 201, 301)));
        assertEquals(this.lostRegion1, this.lostRegionManager.getLostRegionAt(new Location(this.world, 199, 299, 399)));

        // On region border
        assertEquals(this.lostRegion1, this.lostRegionManager.getLostRegionAt(new Location(this.world, 100, 200, 300)));
        assertEquals(this.lostRegion1, this.lostRegionManager.getLostRegionAt(new Location(this.world, 200, 300, 400)));

        // Not in region
        assertNull(this.lostRegionManager.getLostRegionAt(new Location(this.world, 99, 200, 300)));
        assertNull(this.lostRegionManager.getLostRegionAt(new Location(this.world, 100, 199, 300)));
        assertNull(this.lostRegionManager.getLostRegionAt(new Location(this.world, 100, 200, 299)));
    }
}
