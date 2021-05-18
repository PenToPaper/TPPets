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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperGetLostRegionTest {
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

        when(this.connection.prepareStatement("SELECT * FROM tpp_lost_regions WHERE zone_name = ?")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeQuery()).thenReturn(this.resultSet);
        when(this.resultSet.next()).thenReturn(true);
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
    @DisplayName("getLostRegion returns LostAndFoundRegion object")
    void getLostRegionReturnsObject() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("WorldName")).thenReturn(this.world);

            LostAndFoundRegion lostAndFoundRegion = this.mockSQLWrapper.getLostRegion("LostName");

            assertNotNull(lostAndFoundRegion);
            assertEquals("LostName", lostAndFoundRegion.getRegionName());
            assertEquals("WorldName", lostAndFoundRegion.getWorldName());
            assertEquals(this.world, lostAndFoundRegion.getWorld());
            assertEquals(this.world, lostAndFoundRegion.getMaxLoc().getWorld());
            assertEquals(this.world, lostAndFoundRegion.getMinLoc().getWorld());
            assertEquals(1, lostAndFoundRegion.getMinLoc().getBlockX());
            assertEquals(2, lostAndFoundRegion.getMinLoc().getBlockY());
            assertEquals(3, lostAndFoundRegion.getMinLoc().getBlockZ());
            assertEquals(4, lostAndFoundRegion.getMaxLoc().getBlockX());
            assertEquals(5, lostAndFoundRegion.getMaxLoc().getBlockY());
            assertEquals(6, lostAndFoundRegion.getMaxLoc().getBlockZ());

            verify(this.preparedStatement, times(1)).setString(1, "LostName");
            verify(this.preparedStatement, times(1)).executeQuery();
            verify(this.preparedStatement, times(1)).close();
            verify(this.resultSet, times(1)).close();
            verify(this.connection, times(1)).close();
            verify(this.logWrapper, never()).logErrors(anyString());
        }
    }

    @Test
    @DisplayName("getLostRegion returns null when no lost region found")
    void getLostRegionReturnsNull() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("WorldName")).thenReturn(this.world);

            when(this.resultSet.next()).thenReturn(false);

            LostAndFoundRegion lostAndFoundRegion = this.mockSQLWrapper.getLostRegion("LostName");

            assertNull(lostAndFoundRegion);

            verify(this.preparedStatement, times(1)).setString(1, "LostName");
            verify(this.preparedStatement, times(1)).executeQuery();
            verify(this.preparedStatement, times(1)).close();
            verify(this.resultSet, times(1)).close();
            verify(this.connection, times(1)).close();
            verify(this.logWrapper, never()).logErrors(anyString());
        }
    }

    @Test
    @DisplayName("getLostRegion rethrows exceptions")
    void getLostRegionRethrowsExceptions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("WorldName")).thenReturn(this.world);

            when(this.resultSet.next()).thenThrow(new SQLException("Message"));

            assertThrows(SQLException.class, () -> this.mockSQLWrapper.getLostRegion("LostName"));

            verify(this.preparedStatement, times(1)).setString(1, "LostName");
            verify(this.preparedStatement, times(1)).executeQuery();
            verify(this.preparedStatement, times(1)).close();
            verify(this.resultSet, times(1)).close();
            verify(this.connection, times(1)).close();
            verify(this.logWrapper, times(1)).logErrors("Can't execute select statement - Message");
        }
    }
}
