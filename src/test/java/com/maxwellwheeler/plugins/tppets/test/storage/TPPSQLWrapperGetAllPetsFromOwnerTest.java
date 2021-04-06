package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperGetAllPetsFromOwnerTest {
    private TPPets tpPets;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.resultSet = mock(ResultSet.class);

        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE owner_id = ?")).thenReturn(this.preparedStatement);
        when(this.resultSet.next()).thenReturn(true).thenReturn(false);
        when(this.resultSet.getString("pet_id")).thenReturn("MockPetId");
        when(this.resultSet.getInt("pet_type")).thenReturn(7);
        when(this.resultSet.getInt("pet_x")).thenReturn(10);
        when(this.resultSet.getInt("pet_y")).thenReturn(20);
        when(this.resultSet.getInt("pet_z")).thenReturn(30);
        when(this.resultSet.getString("pet_world")).thenReturn("MockWorldName");
        when(this.resultSet.getString("owner_id")).thenReturn("MockOwnerId");
        when(this.resultSet.getString("pet_name")).thenReturn("MockPetName");
        when(this.resultSet.getString("effective_pet_name")).thenReturn("mockpetname");
        when(this.preparedStatement.executeQuery()).thenReturn(this.resultSet);
    }

    @Test
    @DisplayName("getAllPetsFromOwner returns list of all pets")
    void getAllPetsFromOwner() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        List<PetStorage> petStorageList = mockSQLWrapper.getAllPetsFromOwner("Mock-Owner-Id");

        assertEquals(1, petStorageList.size());
        assertEquals("MockPetId", petStorageList.get(0).petId);
        assertEquals(PetType.Pets.HORSE, petStorageList.get(0).petType);
        assertEquals(10, petStorageList.get(0).petX);
        assertEquals(20, petStorageList.get(0).petY);
        assertEquals(30, petStorageList.get(0).petZ);
        assertEquals("MockWorldName", petStorageList.get(0).petWorld);
        assertEquals("MockOwnerId", petStorageList.get(0).ownerId);
        assertEquals("MockPetName", petStorageList.get(0).petName);
        assertEquals("mockpetname", petStorageList.get(0).effectivePetName);

        verify(this.preparedStatement, times(1)).setString(1, "MockOwnerId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getAllPetsFromOwner returns an empty list when no pets are found")
    void getAllPetsFromOwnerEmptyList() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        List<PetStorage> petStorageList = mockSQLWrapper.getAllPetsFromOwner("Mock-Owner-Id");

        assertEquals(0, petStorageList.size());

        verify(this.preparedStatement, times(1)).setString(1, "MockOwnerId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getAllPetsFromOwner rethrows SQLExceptions")
    void getAllPetsFromOwnerEmptyListRethrowsSQLExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException());

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> {
            mockSQLWrapper.getAllPetsFromOwner("Mock-Owner-Id");
        });

        verify(this.preparedStatement, times(1)).setString(1, "MockOwnerId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }
}
