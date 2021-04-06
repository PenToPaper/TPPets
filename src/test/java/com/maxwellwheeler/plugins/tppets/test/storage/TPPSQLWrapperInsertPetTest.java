package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Location;
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

public class TPPSQLWrapperInsertPetTest {
    private TPPets tpPets;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private Location horseLocation;
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
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.stringIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        this.intCaptor = ArgumentCaptor.forClass(Integer.class);
        this.intIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        this.horseLocation = MockFactory.getMockLocation(world, 10, 20, 30);
        this.horse = MockFactory.getMockEntity("Mock-Pet-Id", org.bukkit.entity.Horse.class);

        when(world.getName()).thenReturn("MockWorldName");
        when(this.horse.getWorld()).thenReturn(world);
        when(this.horse.getLocation()).thenReturn(this.horseLocation);

        when(this.connection.prepareStatement("INSERT INTO tpp_unloaded_pets(pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id, pet_name, effective_pet_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("insertPet function inserts a new pet from entity data and pet name")
    void insertPetReturnsTrue() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertTrue(mockSQLWrapper.insertPet(this.horse, "Mock-Owner-Id", "MockPetName"));

        verify(this.preparedStatement, times(5)).setString(this.stringIndexCaptor.capture(), this.stringCaptor.capture());
        List<Integer> stringIndexes = this.stringIndexCaptor.getAllValues();
        List<String> preparedStrings = this.stringCaptor.getAllValues();

        verify(this.preparedStatement, times(4)).setInt(this.intIndexCaptor.capture(), this.intCaptor.capture());
        List<Integer> intIndexes = this.intIndexCaptor.getAllValues();
        List<Integer> preparedInts = this.intCaptor.getAllValues();

        assertEquals(1, stringIndexes.get(0));
        assertEquals("MockPetId", preparedStrings.get(0));

        assertEquals(2, intIndexes.get(0));
        assertEquals(7, preparedInts.get(0));

        assertEquals(3, intIndexes.get(1));
        assertEquals(10, preparedInts.get(1));

        assertEquals(4, intIndexes.get(2));
        assertEquals(20, preparedInts.get(2));

        assertEquals(5, intIndexes.get(3));
        assertEquals(30, preparedInts.get(3));

        assertEquals(6, stringIndexes.get(1));
        assertEquals("MockWorldName", preparedStrings.get(1));

        assertEquals(7, stringIndexes.get(2));
        assertEquals("MockOwnerId", preparedStrings.get(2));

        assertEquals(8, stringIndexes.get(3));
        assertEquals("MockPetName", preparedStrings.get(3));

        assertEquals(9, stringIndexes.get(4));
        assertEquals("mockpetname", preparedStrings.get(4));

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }

    @Test
    @DisplayName("insertPet function returns false if supplied entity that isn't tracked")
    void insertPetReturnsFalseOnUntrackedEntity() throws SQLException {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertFalse(mockSQLWrapper.insertPet(villager, "Mock-Owner-Id", "MockPetName"));

        verify(this.preparedStatement, never()).setString(anyInt(), anyString());
        verify(this.preparedStatement, never()).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, never()).executeUpdate();
        verify(this.connection, never()).close();
        verify(this.preparedStatement, never()).close();
    }

    @Test
    @DisplayName("insertPet function returns false if executed statement doesn't return at least 1")
    void insertPetReturnsFalseOnNoAffectedRows() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        when(this.preparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(mockSQLWrapper.insertPet(this.horse, "Mock-Owner-Id", "MockPetName"));

        verify(this.preparedStatement, times(5)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(4)).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }

    @Test
    @DisplayName("insertPet function rethrows SQLExceptions")
    void insertPetRethrowsSQLExceptions() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> mockSQLWrapper.insertPet(this.horse, "Mock-Owner-Id", "MockPetName"));

        verify(this.preparedStatement, times(5)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(4)).setInt(anyInt(), anyInt());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }
}
