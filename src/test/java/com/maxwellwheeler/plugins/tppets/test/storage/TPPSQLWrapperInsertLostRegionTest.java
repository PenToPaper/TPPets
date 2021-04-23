package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
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

public class TPPSQLWrapperInsertLostRegionTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private LostAndFoundRegion lostAndFoundRegion;
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

        this.stringIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.intIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        this.intCaptor = ArgumentCaptor.forClass(Integer.class);

        World world = mock(World.class);
        this.lostAndFoundRegion = new LostAndFoundRegion("LostName", "WorldName", world, new Location(world, 1, 2, 3), new Location(world, 4, 5, 6));
        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        when(this.connection.prepareStatement("INSERT INTO tpp_lost_regions(zone_name, min_x, min_y, min_z, max_x, max_y, max_z, world_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("insertLostRegion returns true")
    void insertLostRegionReturnsTrue() throws SQLException {
        assertTrue(this.mockSQLWrapper.insertLostRegion(this.lostAndFoundRegion));

        verify(this.preparedStatement, times(2)).setString(this.stringIndexCaptor.capture(), this.stringCaptor.capture());
        List<String> strings = this.stringCaptor.getAllValues();
        List<Integer> stringIndexes = this.stringIndexCaptor.getAllValues();

        verify(this.preparedStatement, times(6)).setInt(this.intIndexCaptor.capture(), this.intCaptor.capture());
        List<Integer> ints = this.intCaptor.getAllValues();
        List<Integer> intIndexes = this.intIndexCaptor.getAllValues();

        assertEquals(1, stringIndexes.get(0));
        assertEquals("LostName", strings.get(0));

        assertEquals(2, intIndexes.get(0));
        assertEquals(1, ints.get(0));

        assertEquals(3, intIndexes.get(1));
        assertEquals(2, ints.get(1));

        assertEquals(4, intIndexes.get(2));
        assertEquals(3, ints.get(2));

        assertEquals(5, intIndexes.get(3));
        assertEquals(4, ints.get(3));

        assertEquals(6, intIndexes.get(4));
        assertEquals(5, ints.get(4));

        assertEquals(7, intIndexes.get(5));
        assertEquals(6, ints.get(5));

        assertEquals(8, stringIndexes.get(1));
        assertEquals("WorldName", strings.get(1));

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("insertLostRegion returns false when no row inserted")
    void insertLostRegionReturnsFalse() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(this.mockSQLWrapper.insertLostRegion(this.lostAndFoundRegion));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(6)).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("insertLostRegion rethrows exceptions")
    void insertLostRegionRethrowsExceptions() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.insertLostRegion(this.lostAndFoundRegion));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(6)).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
