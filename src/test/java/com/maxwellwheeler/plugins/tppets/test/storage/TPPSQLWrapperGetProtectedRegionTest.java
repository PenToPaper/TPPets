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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperGetProtectedRegionTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private World world;
    private MockSQLWrapper mockSQLWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.resultSet = mock(ResultSet.class);
        this.world = mock(World.class);

        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        LostRegionManager lostRegionManager = mock(LostRegionManager.class);
        when(tpPets.getLostRegionManager()).thenReturn(lostRegionManager);

        when(this.connection.prepareStatement("SELECT * FROM tpp_protected_regions WHERE zone_name = ?")).thenReturn(this.preparedStatement);
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
    @DisplayName("getProtectedRegion returns region object")
    void getProtectedRegionReturnsRegion() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("WorldName")).thenReturn(this.world);

            ProtectedRegion protectedRegion = this.mockSQLWrapper.getProtectedRegion("ProtectedName");

            assertNotNull(protectedRegion);
            assertEquals("ProtectedName", protectedRegion.getRegionName());
            assertEquals("EnterMessage", protectedRegion.getEnterMessage());
            assertEquals("WorldName", protectedRegion.getWorldName());
            assertEquals(this.world, protectedRegion.getWorld());
            assertEquals(this.world, protectedRegion.getMaxLoc().getWorld());
            assertEquals(this.world, protectedRegion.getMinLoc().getWorld());
            assertEquals(1, protectedRegion.getMinLoc().getBlockX());
            assertEquals(2, protectedRegion.getMinLoc().getBlockY());
            assertEquals(3, protectedRegion.getMinLoc().getBlockZ());
            assertEquals(4, protectedRegion.getMaxLoc().getBlockX());
            assertEquals(5, protectedRegion.getMaxLoc().getBlockY());
            assertEquals(6, protectedRegion.getMaxLoc().getBlockZ());
            assertEquals("LostName", protectedRegion.getLfName());
            assertNull(protectedRegion.getLfReference());

            verify(this.preparedStatement, times(1)).setString(1, "ProtectedName");
            verify(this.preparedStatement, times(1)).executeQuery();
            verify(this.preparedStatement, times(1)).close();
            verify(this.resultSet, times(1)).close();
            verify(this.connection, times(1)).close();
        }
    }

    @Test
    @DisplayName("getProtectedRegion returns null if no protected region present")
    void getProtectedRegionReturnsNull() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);

        ProtectedRegion protectedRegion = this.mockSQLWrapper.getProtectedRegion("ProtectedName");

        assertNull(protectedRegion);

        verify(this.preparedStatement, times(1)).setString(1, "ProtectedName");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("getProtectedRegion rethrows exceptions")
    void getProtectedRegionRethrowsExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.getProtectedRegion("ProtectedName"));

        verify(this.preparedStatement, times(1)).setString(1, "ProtectedName");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
