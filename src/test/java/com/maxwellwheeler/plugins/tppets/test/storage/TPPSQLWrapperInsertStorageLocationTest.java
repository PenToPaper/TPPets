package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperInsertStorageLocationTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private World world;
    private MockSQLWrapper mockSQLWrapper;
    private ArgumentCaptor<Integer> stringIndexCaptor;
    private ArgumentCaptor<String> stringCaptor;
    private ArgumentCaptor<Integer> intIndexCaptor;
    private ArgumentCaptor<Integer> intCaptor;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.world = mock(World.class);

        this.stringIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.intIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        this.intCaptor = ArgumentCaptor.forClass(Integer.class);

        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        when(this.world.getName()).thenReturn("WorldName");
        when(this.connection.prepareStatement("INSERT INTO tpp_user_storage_locations (user_id, storage_name, effective_storage_name, loc_x, loc_y, loc_z, world_name) VALUES (?, ?, ?, ?, ?, ?, ?)")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("insertStorageLocation returns true")
    void insertStorageLocationReturnsTrue() throws SQLException {
        assertTrue(this.mockSQLWrapper.insertStorageLocation("Mock-Owner-Id", "StorageName", new Location(this.world, 1, 2, 3)));

        verify(this.preparedStatement, times(4)).setString(this.stringIndexCaptor.capture(), this.stringCaptor.capture());
        List<String> strings = this.stringCaptor.getAllValues();
        List<Integer> stringIndexes = this.stringIndexCaptor.getAllValues();

        verify(this.preparedStatement, times(3)).setInt(this.intIndexCaptor.capture(), this.intCaptor.capture());
        List<Integer> ints = this.intCaptor.getAllValues();
        List<Integer> intIndexes = this.intIndexCaptor.getAllValues();

        assertEquals(1, stringIndexes.get(0));
        assertEquals("MockOwnerId", strings.get(0));

        assertEquals(2, stringIndexes.get(1));
        assertEquals("StorageName", strings.get(1));

        assertEquals(3, stringIndexes.get(2));
        assertEquals("storagename", strings.get(2));

        assertEquals(4, intIndexes.get(0));
        assertEquals(1, ints.get(0));

        assertEquals(5, intIndexes.get(1));
        assertEquals(2, ints.get(1));

        assertEquals(6, intIndexes.get(2));
        assertEquals(3, ints.get(2));

        assertEquals(7, stringIndexes.get(3));
        assertEquals("WorldName", strings.get(3));

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("insertStorageLocation returns false when no row inserted")
    void insertStorageLocationReturnsFalse() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(this.mockSQLWrapper.insertStorageLocation("Mock-Owner-Id", "StorageName", new Location(this.world, 1, 2, 3)));

        verify(this.preparedStatement, times(4)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(3)).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("insertLostRegion rethrows exceptions")
    void insertLostRegionRethrowsExceptions() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.insertStorageLocation("Mock-Owner-Id", "StorageName", new Location(this.world, 1, 2, 3)));

        verify(this.preparedStatement, times(4)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(3)).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
