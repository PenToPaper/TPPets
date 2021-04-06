package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
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

public class TPPSQLWrapperIsNameUniqueTest {
    private TPPets tpPets;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ArgumentCaptor<String> stringCaptor;
    private ArgumentCaptor<Integer> intCaptor;
    private ResultSet resultSet;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, this.logWrapper, false, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.resultSet = mock(ResultSet.class);
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.intCaptor = ArgumentCaptor.forClass(Integer.class);

        when(this.connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?")).thenReturn(this.preparedStatement);
        when(this.resultSet.next()).thenReturn(true);
        when(this.preparedStatement.executeQuery()).thenReturn(this.resultSet);
    }

    @Test
    @DisplayName("isNameUnique function returns false when database finds owner has a pet with same name")
    void isNameUniqueReturnsFalse() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertFalse(mockSQLWrapper.isNameUnique("Mock-Owner-Id", "MockPetName"));

        verify(this.preparedStatement, times(2)).setString(this.intCaptor.capture(), this.stringCaptor.capture());
        List<Integer> indexes = this.intCaptor.getAllValues();
        List<String> preparedStrings = this.stringCaptor.getAllValues();
        assertEquals(1, indexes.get(0));
        assertEquals(2, indexes.get(1));
        assertEquals("MockOwnerId", preparedStrings.get(0));
        assertEquals("mockpetname", preparedStrings.get(1));
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("isNameUnique function returns true when database doesn't find owner has a pet with same name")
    void isNameUniqueReturnsTrue() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertTrue(mockSQLWrapper.isNameUnique("Mock-Owner-Id", "MockPetName"));

        verify(this.preparedStatement, times(2)).setString(this.intCaptor.capture(), this.stringCaptor.capture());
        List<Integer> indexes = this.intCaptor.getAllValues();
        List<String> preparedStrings = this.stringCaptor.getAllValues();
        assertEquals(1, indexes.get(0));
        assertEquals(2, indexes.get(1));
        assertEquals("MockOwnerId", preparedStrings.get(0));
        assertEquals("mockpetname", preparedStrings.get(1));
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("isNameUnique function logs SQLExceptions and rethrows them")
    void isNameUniqueRethrowsSQLExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException());

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> {
            mockSQLWrapper.isNameUnique("Mock-Owner-Id", "MockPetName");
        });

        verify(this.logWrapper, times(1)).logErrors("SQL Exception checking if pet name is unique: null");
        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }


}