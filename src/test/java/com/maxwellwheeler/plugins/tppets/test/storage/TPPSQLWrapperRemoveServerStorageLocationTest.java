package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
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

public class TPPSQLWrapperRemoveServerStorageLocationTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private MockSQLWrapper mockSQLWrapper;
    private World world;
    private ArgumentCaptor<Integer> stringIndexCaptor;
    private ArgumentCaptor<String> stringCaptor;

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

        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        when(this.world.getName()).thenReturn("WorldName");
        when(this.connection.prepareStatement("DELETE FROM tpp_server_storage_locations WHERE effective_storage_name = ? AND world_name = ?")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(0);
    }

    @Test
    @DisplayName("removeStorageLocation returns true")
    void removeStorageLocationReturnsTrue() throws SQLException {
        assertTrue(this.mockSQLWrapper.removeServerStorageLocation("StorageName", this.world));

        verify(this.preparedStatement, times(2)).setString(this.stringIndexCaptor.capture(), this.stringCaptor.capture());
        List<Integer> stringIndexes = this.stringIndexCaptor.getAllValues();
        List<String> strings = this.stringCaptor.getAllValues();

        assertEquals(1, stringIndexes.get(0));
        assertEquals("storagename", strings.get(0));

        assertEquals(2, stringIndexes.get(1));
        assertEquals("WorldName", strings.get(1));

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("removeStorageLocation returns false when negative result from update")
    void removeStorageLocationReturnsFalse() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(-1);

        assertFalse(this.mockSQLWrapper.removeServerStorageLocation("StorageName", this.world));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("removeLostRegion rethrows exceptions")
    void removeLostRegionRethrowsExceptions() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.removeServerStorageLocation("StorageName", this.world));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
