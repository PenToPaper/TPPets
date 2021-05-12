package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
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

public class TPPSQLWrapperGetLostRegionsTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private World world;
    private MockSQLWrapper mockSQLWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.resultSet = mock(ResultSet.class);
        this.world = mock(World.class);

        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        when(this.connection.prepareStatement("SELECT * FROM tpp_lost_regions")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeQuery()).thenReturn(this.resultSet);
        when(this.resultSet.next()).thenReturn(true).thenReturn(false);
        when(this.resultSet.getString("zone_name")).thenReturn("LostName");
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

            Hashtable<String, LostAndFoundRegion> lostAndFoundRegions = this.mockSQLWrapper.getLostRegions();

            assertNotNull(lostAndFoundRegions);
            assertEquals(1, lostAndFoundRegions.size());
            assertNotNull(lostAndFoundRegions.get("LostName"));
            assertEquals("LostName", lostAndFoundRegions.get("LostName").getRegionName());
            assertEquals("WorldName", lostAndFoundRegions.get("LostName").getWorldName());
            assertEquals(this.world, lostAndFoundRegions.get("LostName").getWorld());
            assertEquals(this.world, lostAndFoundRegions.get("LostName").getMaxLoc().getWorld());
            assertEquals(this.world, lostAndFoundRegions.get("LostName").getMinLoc().getWorld());
            assertEquals(1, lostAndFoundRegions.get("LostName").getMinLoc().getBlockX());
            assertEquals(2, lostAndFoundRegions.get("LostName").getMinLoc().getBlockY());
            assertEquals(3, lostAndFoundRegions.get("LostName").getMinLoc().getBlockZ());
            assertEquals(4, lostAndFoundRegions.get("LostName").getMaxLoc().getBlockX());
            assertEquals(5, lostAndFoundRegions.get("LostName").getMaxLoc().getBlockY());
            assertEquals(6, lostAndFoundRegions.get("LostName").getMaxLoc().getBlockZ());

            verify(this.preparedStatement, times(1)).executeQuery();
            verify(this.preparedStatement, times(1)).close();
            verify(this.resultSet, times(1)).close();
            verify(this.connection, times(1)).close();
        }
    }

    @Test
    @DisplayName("getLostRegions returns empty hashtable if no results")
    void getLostRegionsReturnsEmptyHashtable() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);

        Hashtable<String, LostAndFoundRegion> lostAndFoundRegions = this.mockSQLWrapper.getLostRegions();

        assertNotNull(lostAndFoundRegions);
        assertEquals(0, lostAndFoundRegions.size());

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("getLostRegions rethrows exceptions")
    void getLostRegionsRethrowsExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.getLostRegions());

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
