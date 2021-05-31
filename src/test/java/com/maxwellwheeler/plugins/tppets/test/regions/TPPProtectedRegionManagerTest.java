package com.maxwellwheeler.plugins.tppets.test.regions;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.LostRegionManager;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegionManager;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TPPProtectedRegionManagerTest {
    private ProtectedRegionManager protectedRegionManager;
    private ProtectedRegion protectedRegion1;
    private World world;
    private TPPets tpPets;
    private LostRegionManager lostRegionManager;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.world = mock(World.class);

        this.lostRegionManager = mock(LostRegionManager.class);
        LostAndFoundRegion lostRegion = new LostAndFoundRegion("LostRegionName", "MockWorldName", this.world, new Location(this.world, 100, 200, 300), new Location(this.world, 400, 500, 600));
        when(this.tpPets.getLostRegionManager()).thenReturn(this.lostRegionManager);
        when(this.lostRegionManager.getLostRegion("LostRegionName")).thenReturn(lostRegion);

        Hashtable<String, ProtectedRegion> protectedRegions = new Hashtable<>();
        this.protectedRegion1 = new ProtectedRegion(this.tpPets, "ProtectedRegion1", "Enter Message", "LostRegionName", "MockWorldName", this.world, new Location(this.world, 200, 300, 400), new Location(this.world, 100, 200, 300));
        protectedRegions.put("ProtectedRegion1", this.protectedRegion1);
        when(sqlWrapper.getProtectedRegions()).thenReturn(protectedRegions);

        this.protectedRegionManager = new ProtectedRegionManager(this.tpPets);
    }

    @Test
    @DisplayName("ProtectedRegionManager getProtectedRegion returns null if no region with name exists")
    void protectedRegionManagerGetProtectedRegionReturnsNull() {
        assertNull(this.protectedRegionManager.getProtectedRegion("InvalidName"));
    }

    @Test
    @DisplayName("ProtectedRegionManager initializes based on database")
    void protectedRegionManagerInitializes() {
        assertEquals(1, this.protectedRegionManager.getProtectedRegions().size());
        assertEquals(this.protectedRegion1, this.protectedRegionManager.getProtectedRegion("ProtectedRegion1"));
    }

    @Test
    @DisplayName("ProtectedRegionManager adds new regions")
    void protectedRegionManagerAddsRegions() {
        ProtectedRegion protectedRegion2 = new ProtectedRegion(this.tpPets, "ProtectedRegion2", "Enter Message", "LostRegionName", "MockWorldName", this.world, new Location(this.world, 400, 500, 600), new Location(this.world, 100, 200, 300));

        this.protectedRegionManager.addProtectedRegion(protectedRegion2);

        assertEquals(2, this.protectedRegionManager.getProtectedRegions().size());
        assertEquals(this.protectedRegion1, this.protectedRegionManager.getProtectedRegion("ProtectedRegion1"));
        assertEquals(protectedRegion2, this.protectedRegionManager.getProtectedRegion("ProtectedRegion2"));
    }

    @Test
    @DisplayName("ProtectedRegionManager removes existing regions")
    void protectedRegionManagerRemovesRegions() {
        this.protectedRegionManager.removeProtectedRegion("ProtectedRegion1");

        assertEquals(0, this.protectedRegionManager.getProtectedRegions().size());
        assertNull(this.protectedRegionManager.getProtectedRegion("ProtectedRegion1"));
    }

    @Test
    @DisplayName("ProtectedRegionManager finds protected regions at locations")
    void protectedRegionManagerFindsRegionAtLocation() {
        // In protected region
        assertEquals(this.protectedRegion1, this.protectedRegionManager.getProtectedRegionAt(new Location(this.world, 101, 201, 301)));
        assertEquals(this.protectedRegion1, this.protectedRegionManager.getProtectedRegionAt(new Location(this.world, 199, 299, 399)));

        // On region border
        assertEquals(this.protectedRegion1, this.protectedRegionManager.getProtectedRegionAt(new Location(this.world, 100, 200, 300)));
        assertEquals(this.protectedRegion1, this.protectedRegionManager.getProtectedRegionAt(new Location(this.world, 200, 300, 400)));

        // Not in region
        assertNull(this.protectedRegionManager.getProtectedRegionAt(new Location(this.world, 99, 200, 300)));
        assertNull(this.protectedRegionManager.getProtectedRegionAt(new Location(this.world, 100, 199, 300)));
        assertNull(this.protectedRegionManager.getProtectedRegionAt(new Location(this.world, 100, 200, 299)));
    }

    @Test
    @DisplayName("ProtectedRegionManager canTpThere returns correctly and sends enter message to the player")
    void protectedRegionManagerCanTpThere() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        Player admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.tpanywhere"});

        // In protected region
        assertFalse(this.protectedRegionManager.canTpThere(player, new Location(this.world, 101, 201, 301)));
        assertFalse(this.protectedRegionManager.canTpThere(player, new Location(this.world, 199, 299, 399)));

        // On region border
        assertFalse(this.protectedRegionManager.canTpThere(player, new Location(this.world, 100, 200, 300)));
        assertFalse(this.protectedRegionManager.canTpThere(player, new Location(this.world, 200, 300, 400)));

        // Not in region
        assertTrue(this.protectedRegionManager.canTpThere(player, new Location(this.world, 99, 200, 300)));
        assertTrue(this.protectedRegionManager.canTpThere(player, new Location(this.world, 100, 199, 300)));
        assertTrue(this.protectedRegionManager.canTpThere(player, new Location(this.world, 100, 200, 299)));

        // In region, but with permission
        assertTrue(this.protectedRegionManager.canTpThere(admin, new Location(this.world, 101, 201, 301)));
        assertTrue(this.protectedRegionManager.canTpThere(admin, new Location(this.world, 199, 299, 399)));
    }

    @Test
    @DisplayName("ProtectedRegionManager updates lost and found references")
    void protectedRegionManagerUpdatesLfReferences() {
        assertNotNull(this.protectedRegionManager.getProtectedRegion("ProtectedRegion1").getLfReference());

        when(this.lostRegionManager.getLostRegion("LostRegionName")).thenReturn(null);
        this.protectedRegionManager.updateLFReferences("LostRegionName");

        assertNull(this.protectedRegionManager.getProtectedRegion("ProtectedRegion1").getLfReference());
    }
}
