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
    private PreparedStatement getAllPetsStatement;
    private ResultSet getAllPetsResultSet;
    private PreparedStatement isNameUniqueStatement;
    private ResultSet isNameUniqueResultSet;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.getAllPetsStatement = mock(PreparedStatement.class);
        this.isNameUniqueStatement = mock(PreparedStatement.class);
        this.getAllPetsResultSet = mock(ResultSet.class);
        this.isNameUniqueResultSet = mock(ResultSet.class);

        // Is name unique
        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?")).thenReturn(this.isNameUniqueStatement);
        when(this.isNameUniqueResultSet.next()).thenReturn(false);
        when(this.isNameUniqueStatement.executeQuery()).thenReturn(this.isNameUniqueResultSet);

        // Getting all pets from pet owner
        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE owner_id = ?")).thenReturn(this.getAllPetsStatement);
        when(this.getAllPetsResultSet.next()).thenReturn(true).thenReturn(false);
        when(this.getAllPetsResultSet.getString("pet_id")).thenReturn("MockPetId");
        when(this.getAllPetsResultSet.getInt("pet_type")).thenReturn(7);
        when(this.getAllPetsResultSet.getInt("pet_x")).thenReturn(10);
        when(this.getAllPetsResultSet.getInt("pet_y")).thenReturn(20);
        when(this.getAllPetsResultSet.getInt("pet_z")).thenReturn(30);
        when(this.getAllPetsResultSet.getString("pet_world")).thenReturn("MockWorldName");
        when(this.getAllPetsResultSet.getString("owner_id")).thenReturn("MockOwnerId");
        when(this.getAllPetsResultSet.getString("pet_name")).thenReturn("MockPetName");
        when(this.getAllPetsResultSet.getString("effective_pet_name")).thenReturn("mockpetname");
        when(this.getAllPetsStatement.executeQuery()).thenReturn(this.getAllPetsResultSet);
    }

    @Test
    @DisplayName("generateUniquePetName returns a unique name based on pet type and numbers of pets")
    void generateUniquePetName() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertEquals("HORSE1", mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(2)).close();

        verify(this.isNameUniqueStatement, times(1)).executeQuery();
        verify(this.isNameUniqueStatement, times(1)).close();
        verify(this.isNameUniqueResultSet, times(1)).close();

        verify(this.getAllPetsStatement, times(1)).executeQuery();
        verify(this.getAllPetsStatement, times(1)).close();
        verify(this.getAllPetsResultSet, times(1)).close();
    }

    @Test
    @DisplayName("generateUniquePetName increments the pet name based on the size of total pets from that owner")
    void generateUniquePetNameWhenHorse1Exists() throws SQLException {
        when(this.isNameUniqueResultSet.next()).thenReturn(true).thenReturn(false);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertEquals("HORSE2", mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(3)).close();

        verify(this.isNameUniqueStatement, times(2)).executeQuery();
        verify(this.isNameUniqueStatement, times(2)).close();
        verify(this.isNameUniqueResultSet, times(2)).close();

        verify(this.getAllPetsStatement, times(1)).executeQuery();
        verify(this.getAllPetsStatement, times(1)).close();
        verify(this.getAllPetsResultSet, times(1)).close();
    }

    @Test
    @DisplayName("generateUniquePetName increments the pet name if the pet name already exists")
    void generateUniquePetNameWith2OwnedPets() throws SQLException {
        when(this.getAllPetsResultSet.next()).thenReturn(true).thenReturn(true).thenReturn(false);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertEquals("HORSE2", mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(2)).close();

        verify(this.isNameUniqueStatement, times(1)).executeQuery();
        verify(this.isNameUniqueStatement, times(1)).close();
        verify(this.isNameUniqueResultSet, times(1)).close();

        verify(this.getAllPetsStatement, times(1)).executeQuery();
        verify(this.getAllPetsStatement, times(1)).close();
        verify(this.getAllPetsResultSet, times(1)).close();
    }

    @Test
    @DisplayName("generateUniquePetName throws SQLExceptions from getAllPetsFromOwner")
    void generateUniquePetNameRethrowsGetException() throws SQLException {
        when(this.getAllPetsResultSet.next()).thenThrow(new SQLException());

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(1)).close();

        verify(this.isNameUniqueStatement, never()).executeQuery();
        verify(this.isNameUniqueStatement, never()).close();
        verify(this.isNameUniqueResultSet, never()).close();

        verify(this.getAllPetsStatement, times(1)).executeQuery();
        verify(this.getAllPetsStatement, times(1)).close();
        verify(this.getAllPetsResultSet, times(1)).close();
    }

    @Test
    @DisplayName("generateUniquePetName throws SQLExceptions from isNameUnique")
    void generateUniquePetNameRethrowsIsUniqueException() throws SQLException {
        when(this.isNameUniqueResultSet.next()).thenThrow(new SQLException());

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> mockSQLWrapper.generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE));

        verify(this.connection, times(2)).close();

        verify(this.isNameUniqueStatement, times(1)).executeQuery();
        verify(this.isNameUniqueStatement, times(1)).close();
        verify(this.isNameUniqueResultSet, times(1)).close();

        verify(this.getAllPetsStatement, times(1)).executeQuery();
        verify(this.getAllPetsStatement, times(1)).close();
        verify(this.getAllPetsResultSet, times(1)).close();
    }
}
