package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TPPSQLWrapperGenerateUniquePetNameTest {
    private TPPets tpPets;
    private Connection connection;
    private PreparedStatement getNumPetsStatement;
    private ResultSet getNumPetsResultSet;
    private PreparedStatement getSpecificPetStatement;
    private ResultSet getSpecificPetResultSet;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.getNumPetsStatement = mock(PreparedStatement.class);
        this.getSpecificPetStatement = mock(PreparedStatement.class);
        this.getNumPetsResultSet = mock(ResultSet.class);
        this.getSpecificPetResultSet = mock(ResultSet.class);

        // Get specific pet
        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?")).thenReturn(this.getSpecificPetStatement);
        when(this.getSpecificPetResultSet.next()).thenReturn(false);
        when(this.getSpecificPetStatement.executeQuery()).thenReturn(this.getSpecificPetResultSet);

        // Getting all pets from pet owner
        when(this.connection.prepareStatement("SELECT COUNT(pet_id) as count FROM tpp_unloaded_pets WHERE owner_id = ?")).thenReturn(this.getNumPetsStatement);
        when(this.getNumPetsResultSet.next()).thenReturn(true);
        when(this.getNumPetsResultSet.getInt("count")).thenReturn(1);
        when(this.getNumPetsStatement.executeQuery()).thenReturn(this.getNumPetsResultSet);
    }

    @Test
    @DisplayName("generateUniquePetName returns a unique name based on pet type and numbers of pets")
    void generateUniquePetName() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertEquals("HORSE1", mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(2)).close();

        verify(this.getSpecificPetStatement, times(1)).executeQuery();
        verify(this.getSpecificPetStatement, times(1)).close();
        verify(this.getSpecificPetResultSet, times(1)).close();

        verify(this.getNumPetsStatement, times(1)).executeQuery();
        verify(this.getNumPetsStatement, times(1)).close();
        verify(this.getNumPetsResultSet, times(1)).close();
    }

    @Test
    @DisplayName("generateUniquePetName increments the pet name based on the size of total pets from that owner")
    void generateUniquePetNameWhenHorse1Exists() throws SQLException {
        when(this.getSpecificPetResultSet.next()).thenReturn(true).thenReturn(false);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertEquals("HORSE2", mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(3)).close();

        verify(this.getSpecificPetStatement, times(2)).executeQuery();
        verify(this.getSpecificPetStatement, times(2)).close();
        verify(this.getSpecificPetResultSet, times(2)).close();

        verify(this.getNumPetsStatement, times(1)).executeQuery();
        verify(this.getNumPetsStatement, times(1)).close();
        verify(this.getNumPetsResultSet, times(1)).close();
    }

    @Test
    @DisplayName("generateUniquePetName increments the pet name if the pet name already exists")
    void generateUniquePetNameWith2OwnedPets() throws SQLException {
        when(this.getNumPetsResultSet.getInt("count")).thenReturn(2);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertEquals("HORSE2", mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(2)).close();

        verify(this.getSpecificPetStatement, times(1)).executeQuery();
        verify(this.getSpecificPetStatement, times(1)).close();
        verify(this.getSpecificPetResultSet, times(1)).close();

        verify(this.getNumPetsStatement, times(1)).executeQuery();
        verify(this.getNumPetsStatement, times(1)).close();
        verify(this.getNumPetsResultSet, times(1)).close();
    }

    @Test
    @DisplayName("generateUniquePetName throws SQLExceptions from getAllPetsFromOwner")
    void generateUniquePetNameRethrowsGetException() throws SQLException {
        when(this.getNumPetsResultSet.next()).thenThrow(new SQLException());

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(1)).close();

        verify(this.getSpecificPetStatement, never()).executeQuery();
        verify(this.getSpecificPetStatement, never()).close();
        verify(this.getSpecificPetResultSet, never()).close();

        verify(this.getNumPetsStatement, times(1)).executeQuery();
        verify(this.getNumPetsStatement, times(1)).close();
        verify(this.getNumPetsResultSet, times(1)).close();
    }

    @Test
    @DisplayName("generateUniquePetName throws SQLExceptions from getSpecificPet")
    void generateUniquePetNameRethrowsGetSpecificPetException() throws SQLException {
        when(this.getSpecificPetResultSet.next()).thenThrow(new SQLException());

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(2)).close();

        verify(this.getSpecificPetStatement, times(1)).executeQuery();
        verify(this.getSpecificPetStatement, times(1)).close();
        verify(this.getSpecificPetResultSet, times(1)).close();

        verify(this.getNumPetsStatement, times(1)).executeQuery();
        verify(this.getNumPetsStatement, times(1)).close();
        verify(this.getNumPetsResultSet, times(1)).close();
    }
}
