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
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperGetStorageLocationTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private World world;
    private ResultSet resultSet;
    private MockSQLWrapper mockSQLWrapper;
    private ArgumentCaptor<String> preparedStringCaptor;
    private ArgumentCaptor<Integer> preparedIndexCaptor;

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

        this.preparedStringCaptor = ArgumentCaptor.forClass(String.class);
        this.preparedIndexCaptor = ArgumentCaptor.forClass(Integer.class);

        when(this.connection.prepareStatement("SELECT * FROM tpp_user_storage_locations WHERE user_id = ? AND effective_storage_name = ? LIMIT 1")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeQuery()).thenReturn(this.resultSet);
        when(this.resultSet.next()).thenReturn(true);
        when(this.resultSet.getString("world_name")).thenReturn("WorldName");
        when(this.resultSet.getInt("loc_x")).thenReturn(1);
        when(this.resultSet.getInt("loc_y")).thenReturn(2);
        when(this.resultSet.getInt("loc_z")).thenReturn(3);
        when(this.resultSet.getString("user_id")).thenReturn("MockOwnerId");
        when(this.resultSet.getString("storage_name")).thenReturn("StorageName");
        when(this.resultSet.getString("effective_storage_name")).thenReturn("storagename");
    }

    @Test
    @DisplayName("getStorageLocation returns storage location object")
    void getStorageLocationReturnsStorage() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("WorldName")).thenReturn(this.world);

            PlayerStorageLocation storageLocation = this.mockSQLWrapper.getStorageLocation("Mock-Owner-Id", "StorageName");

            assertNotNull(storageLocation);
            assertEquals("StorageName", storageLocation.getStorageName());
            assertEquals("storagename", storageLocation.getEffectiveStorageName());
            assertNotNull(storageLocation.getLoc());
            assertEquals(this.world, storageLocation.getLoc().getWorld());
            assertEquals(1, storageLocation.getLoc().getBlockX());
            assertEquals(2, storageLocation.getLoc().getBlockY());
            assertEquals(3, storageLocation.getLoc().getBlockZ());

            verify(this.preparedStatement, times(2)).setString(this.preparedIndexCaptor.capture(), this.preparedStringCaptor.capture());
            List<String> preparedStrings = this.preparedStringCaptor.getAllValues();
            List<Integer> preparedStringIndexes = this.preparedIndexCaptor.getAllValues();

            assertEquals(1, preparedStringIndexes.get(0));
            assertEquals("MockOwnerId", preparedStrings.get(0));
            assertEquals(2, preparedStringIndexes.get(1));
            assertEquals("storagename", preparedStrings.get(1));

            verify(this.preparedStatement, times(1)).executeQuery();
            verify(this.preparedStatement, times(1)).close();
            verify(this.resultSet, times(1)).close();
            verify(this.connection, times(1)).close();
        }
    }

    @Test
    @DisplayName("getStorageLocation returns null if no storage location present")
    void getStorageLocationReturnsNull() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);

        PlayerStorageLocation storageLocation = this.mockSQLWrapper.getStorageLocation("Mock-Owner-Id", "StorageName");

        assertNull(storageLocation);

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("getStorageLocation rethrows exceptions")
    void getStorageLocationRethrowsExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.getStorageLocation("Mock-Owner-Id", "StorageName"));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
