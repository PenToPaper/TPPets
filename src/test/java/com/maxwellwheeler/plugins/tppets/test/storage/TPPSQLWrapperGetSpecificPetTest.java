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
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperGetSpecificPetTest {
    private TPPets tpPets;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ArgumentCaptor<String> preparedStringCaptor;
    private ArgumentCaptor<Integer> preparedIndexCaptor;
    private ResultSet resultSet;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, this.logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.resultSet = mock(ResultSet.class);
        this.preparedStringCaptor = ArgumentCaptor.forClass(String.class);
        this.preparedIndexCaptor = ArgumentCaptor.forClass(Integer.class);

        when(this.resultSet.next()).thenReturn(true);
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
    @DisplayName("getSpecificPet returns pet from ownerId and petName")
    void getSpecificPetFromOwnerIdPetName() throws SQLException {
        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?")).thenReturn(this.preparedStatement);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        PetStorage petStorage = mockSQLWrapper.getSpecificPet("Mock-Owner-Id", "MockPetName");

        assertEquals("MockPetId", petStorage.petId);
        assertEquals(PetType.Pets.HORSE, petStorage.petType);
        assertEquals(10, petStorage.petX);
        assertEquals(20, petStorage.petY);
        assertEquals(30, petStorage.petZ);
        assertEquals("MockWorldName", petStorage.petWorld);
        assertEquals("MockOwnerId", petStorage.ownerId);
        assertEquals("MockPetName", petStorage.petName);
        assertEquals("mockpetname", petStorage.effectivePetName);

        verify(this.preparedStatement, times(2)).setString(this.preparedIndexCaptor.capture(), this.preparedStringCaptor.capture());
        List<String> preparedStrings = this.preparedStringCaptor.getAllValues();
        List<Integer> preparedStringIndexes = this.preparedIndexCaptor.getAllValues();

        assertEquals(1, preparedStringIndexes.get(0));
        assertEquals("MockOwnerId", preparedStrings.get(0));
        assertEquals(2, preparedStringIndexes.get(1));
        assertEquals("mockpetname", preparedStrings.get(1));

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getSpecificPet returns null from ownerId and petName when pet not found")
    void getSpecificPetFromOwnerIdPetNameReturnsNull() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);
        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?")).thenReturn(this.preparedStatement);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        PetStorage petStorage = mockSQLWrapper.getSpecificPet("Mock-Owner-Id", "MockPetName");

        assertNull(petStorage);

        verify(this.preparedStatement, times(2)).setString(this.preparedIndexCaptor.capture(), this.preparedStringCaptor.capture());
        List<String> preparedStrings = this.preparedStringCaptor.getAllValues();
        List<Integer> preparedStringIndexes = this.preparedIndexCaptor.getAllValues();

        assertEquals(1, preparedStringIndexes.get(0));
        assertEquals("MockOwnerId", preparedStrings.get(0));
        assertEquals(2, preparedStringIndexes.get(1));
        assertEquals("mockpetname", preparedStrings.get(1));

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.logWrapper, never()).logErrors(anyString());
    }

    @Test
    @DisplayName("getSpecificPet rethrows SQLExceptions")
    void getSpecificPetFromOwnerIdPetNameRethrowsSQLExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException("Message"));
        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?")).thenReturn(this.preparedStatement);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> mockSQLWrapper.getSpecificPet("Mock-Owner-Id", "MockPetName"));

        verify(this.preparedStatement, times(2)).setString(this.preparedIndexCaptor.capture(), this.preparedStringCaptor.capture());
        List<String> preparedStrings = this.preparedStringCaptor.getAllValues();
        List<Integer> preparedStringIndexes = this.preparedIndexCaptor.getAllValues();

        assertEquals(1, preparedStringIndexes.get(0));
        assertEquals("MockOwnerId", preparedStrings.get(0));
        assertEquals(2, preparedStringIndexes.get(1));
        assertEquals("mockpetname", preparedStrings.get(1));

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.logWrapper, times(1)).logErrors("Can't execute select statement - Message");
    }

    @Test
    @DisplayName("getSpecificPet returns pet from petId")
    void getSpecificPetFromPetId() throws SQLException {
        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE pet_id = ?")).thenReturn(this.preparedStatement);
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        PetStorage petStorage = mockSQLWrapper.getSpecificPet("Mock-Pet-Id");

        assertEquals("MockPetId", petStorage.petId);
        assertEquals(PetType.Pets.HORSE, petStorage.petType);
        assertEquals(10, petStorage.petX);
        assertEquals(20, petStorage.petY);
        assertEquals(30, petStorage.petZ);
        assertEquals("MockWorldName", petStorage.petWorld);
        assertEquals("MockOwnerId", petStorage.ownerId);
        assertEquals("MockPetName", petStorage.petName);
        assertEquals("mockpetname", petStorage.effectivePetName);

        verify(this.preparedStatement, times(1)).setString(1, "MockPetId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getSpecificPet returns null when petId not found")
    void getSpecificPetFromPetIdReturnsNull() throws SQLException {
        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE pet_id = ?")).thenReturn(this.preparedStatement);
        when(this.resultSet.next()).thenReturn(false);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        PetStorage petStorage = mockSQLWrapper.getSpecificPet("Mock-Pet-Id");

        assertNull(petStorage);

        verify(this.preparedStatement, times(1)).setString(1, "MockPetId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getSpecificPet rethrows SQLExceptions")
    void getSpecificPetFromPetIdRethrowsSQLExceptions() throws SQLException {
        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE pet_id = ?")).thenReturn(this.preparedStatement);
        when(this.resultSet.next()).thenThrow(new SQLException("Message"));

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> mockSQLWrapper.getSpecificPet("Mock-Pet-Id"));

        verify(this.preparedStatement, times(1)).setString(1, "MockPetId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
        verify(this.logWrapper, times(1)).logErrors("Can't execute select statement - Message");
    }
}
