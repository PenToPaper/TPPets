package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperGetPlayerStorageLocationsTest {
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

        when(this.connection.prepareStatement("SELECT * FROM tpp_user_storage_locations WHERE user_id = ?")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeQuery()).thenReturn(this.resultSet);
        when(this.resultSet.next()).thenReturn(true).thenReturn(false);
        when(this.resultSet.getString("world_name")).thenReturn("WorldName");
        when(this.resultSet.getInt("loc_x")).thenReturn(1);
        when(this.resultSet.getInt("loc_y")).thenReturn(2);
        when(this.resultSet.getInt("loc_z")).thenReturn(3);
        when(this.resultSet.getString("user_id")).thenReturn("MockOwnerId");
        when(this.resultSet.getString("storage_name")).thenReturn("StorageName");
    }

    @Test
    @DisplayName("getStorageLocations returns accurate list")
    void getStorageLocationsReturnsList() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("WorldName")).thenReturn(this.world);

            List<PlayerStorageLocation> storageLocations = this.mockSQLWrapper.getStorageLocations("Mock-Owner-Id");

            assertNotNull(storageLocations);
            assertEquals(1, storageLocations.size());
            assertNotNull(storageLocations.get(0));
            assertEquals("MockOwnerId", storageLocations.get(0).getUserId());
            assertEquals("StorageName", storageLocations.get(0).getStorageName());
            assertNotNull(storageLocations.get(0).getLoc());
            assertEquals(this.world, storageLocations.get(0).getLoc().getWorld());
            assertEquals(1, storageLocations.get(0).getLoc().getBlockX());
            assertEquals(2, storageLocations.get(0).getLoc().getBlockY());
            assertEquals(3, storageLocations.get(0).getLoc().getBlockZ());

            verify(this.preparedStatement, times(1)).setString(1, "MockOwnerId");
            verify(this.preparedStatement, times(1)).executeQuery();
            verify(this.preparedStatement, times(1)).close();
            verify(this.resultSet, times(1)).close();
            verify(this.connection, times(1)).close();
        }
    }

    @Test
    @DisplayName("getStorageLocations returns empty list when no storage locations found")
    void getStorageLocationsReturnsEmptyList() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);

        List<PlayerStorageLocation> storageLocations = this.mockSQLWrapper.getStorageLocations("Mock-Owner-Id");

        assertNotNull(storageLocations);
        assertEquals(0, storageLocations.size());

        verify(this.preparedStatement, times(1)).setString(1, "MockOwnerId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("getStorageLocations rethrows exceptions")
    void getStorageLocationsRethrowsExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.getStorageLocations("Mock-Owner-Id"));

        verify(this.preparedStatement, times(1)).setString(1, "MockOwnerId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
