package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Villager;
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

public class TPPSQLWrapperUpdatePetLocationTest {
    private TPPets tpPets;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private Horse horse;
    private ArgumentCaptor<String> stringCaptor;
    private ArgumentCaptor<Integer> stringIndexCaptor;
    private ArgumentCaptor<Integer> intCaptor;
    private ArgumentCaptor<Integer> intIndexCaptor;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        World world = mock(World.class);
        OfflinePlayer owner = MockFactory.getMockOfflinePlayer("Mock-Owner-Id", "MockOwnerName");
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.stringIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        this.intCaptor = ArgumentCaptor.forClass(Integer.class);
        this.intIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        Location horseLocation = MockFactory.getMockLocation(world, 10, 20, 30);
        this.horse = MockFactory.getTamedMockEntity("Mock-Pet-Id", org.bukkit.entity.Horse.class, owner);

        when(world.getName()).thenReturn("MockWorldName");
        when(this.horse.getWorld()).thenReturn(world);
        when(this.horse.getLocation()).thenReturn(horseLocation);

        when(this.connection.prepareStatement("UPDATE tpp_unloaded_pets SET pet_x = ?, pet_y = ?, pet_z = ?, pet_world = ? WHERE pet_id = ?")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("updatePetLocation function returns true based on entity data")
    void updatePetLocationReturnsTrue() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertTrue(mockSQLWrapper.updatePetLocation(this.horse));

        verify(this.preparedStatement, times(2)).setString(this.stringIndexCaptor.capture(), this.stringCaptor.capture());
        List<Integer> stringIndexes = this.stringIndexCaptor.getAllValues();
        List<String> preparedStrings = this.stringCaptor.getAllValues();

        verify(this.preparedStatement, times(3)).setInt(this.intIndexCaptor.capture(), this.intCaptor.capture());
        List<Integer> intIndexes = this.intIndexCaptor.getAllValues();
        List<Integer> preparedInts = this.intCaptor.getAllValues();

        assertEquals(1, intIndexes.get(0));
        assertEquals(10, preparedInts.get(0));

        assertEquals(2, intIndexes.get(1));
        assertEquals(20, preparedInts.get(1));

        assertEquals(3, intIndexes.get(2));
        assertEquals(30, preparedInts.get(2));

        assertEquals(4, stringIndexes.get(0));
        assertEquals("MockWorldName", preparedStrings.get(0));

        assertEquals(5, stringIndexes.get(1));
        assertEquals("MockPetId", preparedStrings.get(1));

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }

    @Test
    @DisplayName("updatePetLocation function returns false when pet isn't owned")
    void updatePetLocationReturnsFalseWhenPetNotOwned() throws SQLException {
        this.horse = MockFactory.getMockEntity("Mock-Pet-Id", org.bukkit.entity.Horse.class);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertFalse(mockSQLWrapper.updatePetLocation(this.horse));

        verify(this.preparedStatement, never()).setString(anyInt(), anyString());
        verify(this.preparedStatement, never()).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, never()).executeUpdate();
        verify(this.connection, never()).close();
        verify(this.preparedStatement, never()).close();
    }

    @Test
    @DisplayName("updatePetLocation function returns false when pet isn't of right type")
    void updatePetLocationReturnsFalseWhenPetWrongType() throws SQLException {
        Villager villager = MockFactory.getMockEntity("Mock-Villager-Id", org.bukkit.entity.Villager.class);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertFalse(mockSQLWrapper.updatePetLocation(villager));

        verify(this.preparedStatement, never()).setString(anyInt(), anyString());
        verify(this.preparedStatement, never()).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, never()).executeUpdate();
        verify(this.connection, never()).close();
        verify(this.preparedStatement, never()).close();
    }

    @Test
    @DisplayName("updatePetLocation function returns false when execute update returns less than 0")
    void updatePetLocationReturnsFalseWhenDeleteReturnsLessThan0() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(-1);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertFalse(mockSQLWrapper.updatePetLocation(this.horse));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(3)).setInt(anyInt(), anyInt());

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }

    @Test
    @DisplayName("updatePetLocation rethrows SQLException")
    void updatePetLocationRethrowsSQLException() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> mockSQLWrapper.updatePetLocation(this.horse));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(3)).setInt(anyInt(), anyInt());

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }
}
