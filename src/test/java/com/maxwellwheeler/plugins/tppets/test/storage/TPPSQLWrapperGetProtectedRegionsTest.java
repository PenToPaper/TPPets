package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostRegionManager;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperGetProtectedRegionsTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private World world;
    private MockSQLWrapper mockSQLWrapper;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, this.logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.resultSet = mock(ResultSet.class);
        this.world = mock(World.class);

        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        LostRegionManager lostRegionManager = mock(LostRegionManager.class);
        when(tpPets.getLostRegionManager()).thenReturn(lostRegionManager);

        when(this.connection.prepareStatement("SELECT * FROM tpp_protected_regions")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeQuery()).thenReturn(this.resultSet);
        when(this.resultSet.next()).thenReturn(true).thenReturn(false);
        when(this.resultSet.getString("zone_name")).thenReturn("ProtectedName");
        when(this.resultSet.getString("enter_message")).thenReturn("EnterMessage");
        when(this.resultSet.getString("lf_zone_name")).thenReturn("LostName");
        when(this.resultSet.getString("world_name")).thenReturn("WorldName");
        when(this.resultSet.getInt("min_x")).thenReturn(1);
        when(this.resultSet.getInt("min_y")).thenReturn(2);
        when(this.resultSet.getInt("min_z")).thenReturn(3);
        when(this.resultSet.getInt("max_x")).thenReturn(4);
        when(this.resultSet.getInt("max_y")).thenReturn(5);
        when(this.resultSet.getInt("max_z")).thenReturn(6);
    }

    @Test
    @DisplayName("getLostRegions returns accurate hashtable")
    void getLostRegionsReturnsHashtable() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("WorldName")).thenReturn(this.world);

            Hashtable<String, ProtectedRegion> protectedRegions = this.mockSQLWrapper.getProtectedRegions();

            assertNotNull(protectedRegions);
            assertEquals(1, protectedRegions.size());
            assertNotNull(protectedRegions.get("ProtectedName"));
            assertEquals("ProtectedName", protectedRegions.get("ProtectedName").getRegionName());
            assertEquals("EnterMessage", protectedRegions.get("ProtectedName").getEnterMessage());
            assertEquals("WorldName", protectedRegions.get("ProtectedName").getWorldName());
            assertEquals(this.world, protectedRegions.get("ProtectedName").getWorld());
            assertEquals(this.world, protectedRegions.get("ProtectedName").getMaxLoc().getWorld());
            assertEquals(this.world, protectedRegions.get("ProtectedName").getMinLoc().getWorld());
            assertEquals(1, protectedRegions.get("ProtectedName").getMinLoc().getBlockX());
            assertEquals(2, protectedRegions.get("ProtectedName").getMinLoc().getBlockY());
            assertEquals(3, protectedRegions.get("ProtectedName").getMinLoc().getBlockZ());
            assertEquals(4, protectedRegions.get("ProtectedName").getMaxLoc().getBlockX());
            assertEquals(5, protectedRegions.get("ProtectedName").getMaxLoc().getBlockY());
            assertEquals(6, protectedRegions.get("ProtectedName").getMaxLoc().getBlockZ());
            assertEquals("LostName", protectedRegions.get("ProtectedName").getLfName());
            assertNull(protectedRegions.get("ProtectedName").getLfReference());
            verify(this.logWrapper, never()).logErrors(anyString());
        }
    }

    @Test
    @DisplayName("getLostRegions returns empty hashtable if no results")
    void getLostRegionsReturnsEmptyHashtable() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);

        Hashtable<String, ProtectedRegion> protectedRegions = this.mockSQLWrapper.getProtectedRegions();

        assertNotNull(protectedRegions);
        assertEquals(0, protectedRegions.size());

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
        verify(this.logWrapper, never()).logErrors(anyString());
    }

    @Test
    @DisplayName("getLostRegions rethrows exceptions")
    void getLostRegionsRethrowsExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.getProtectedRegions());

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
        verify(this.logWrapper, times(1)).logErrors("Can't execute select statement - Message");
    }
}
