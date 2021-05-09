package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostRegionManager;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegionManager;
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

public class TPPSQLWrapperInsertProtectedRegionTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ProtectedRegion protectedRegion;
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

        LostRegionManager lostRegionManager = mock(LostRegionManager.class);
        when(tpPets.getLostRegionManager()).thenReturn(lostRegionManager);

        World world = mock(World.class);
        this.protectedRegion = new ProtectedRegion("ProtectedRegion", "EnterMessage", "WorldName", world, new Location(world, 1, 2, 3), new Location(world, 4, 5, 6), "ProtectedRegion", tpPets);
        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        ProtectedRegionManager protectedRegionManager = mock(ProtectedRegionManager.class);
        when(tpPets.getProtectedRegionManager()).thenReturn(protectedRegionManager);
        when(protectedRegionManager.getProtectedRegion("ProtectedRegion")).thenReturn(null);
        when(this.connection.prepareStatement("INSERT INTO tpp_protected_regions(zone_name, enter_message, min_x, min_y, min_z, max_x, max_y, max_z, world_name, lf_zone_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("insertProtectedRegion returns true")
    void insertProtectedRegionReturnsTrue() throws SQLException {
        assertTrue(this.mockSQLWrapper.insertProtectedRegion(this.protectedRegion));

        verify(this.preparedStatement, times(4)).setString(this.stringIndexCaptor.capture(), this.stringCaptor.capture());
        List<String> strings = this.stringCaptor.getAllValues();
        List<Integer> stringIndexes = this.stringIndexCaptor.getAllValues();

        verify(this.preparedStatement, times(6)).setInt(this.intIndexCaptor.capture(), this.intCaptor.capture());
        List<Integer> ints = this.intCaptor.getAllValues();
        List<Integer> intIndexes = this.intIndexCaptor.getAllValues();

        assertEquals(1, stringIndexes.get(0));
        assertEquals("ProtectedRegion", strings.get(0));

        assertEquals(2, stringIndexes.get(1));
        assertEquals("EnterMessage", strings.get(1));

        assertEquals(3, intIndexes.get(0));
        assertEquals(1, ints.get(0));

        assertEquals(4, intIndexes.get(1));
        assertEquals(2, ints.get(1));

        assertEquals(5, intIndexes.get(2));
        assertEquals(3, ints.get(2));

        assertEquals(6, intIndexes.get(3));
        assertEquals(4, ints.get(3));

        assertEquals(7, intIndexes.get(4));
        assertEquals(5, ints.get(4));

        assertEquals(8, intIndexes.get(5));
        assertEquals(6, ints.get(5));

        assertEquals(9, stringIndexes.get(2));
        assertEquals("WorldName", strings.get(2));

        assertEquals(10, stringIndexes.get(3));
        assertEquals("ProtectedRegion", strings.get(3));

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("insertProtectedRegion returns false when no row inserted")
    void insertProtectedRegionReturnsFalse() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(this.mockSQLWrapper.insertProtectedRegion(this.protectedRegion));

        verify(this.preparedStatement, times(4)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(6)).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("insertProtectedRegion rethrows exceptions")
    void insertProtectedRegionRethrowsExceptions() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.insertProtectedRegion(this.protectedRegion));

        verify(this.preparedStatement, times(4)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(6)).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
