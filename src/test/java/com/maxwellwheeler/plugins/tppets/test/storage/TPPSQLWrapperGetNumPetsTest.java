package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperGetNumPetsTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private MockSQLWrapper mockSQLWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.resultSet = mock(ResultSet.class);

        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        when(this.connection.prepareStatement("SELECT COUNT(pet_id) as count FROM tpp_unloaded_pets WHERE owner_id = ?")).thenReturn(this.preparedStatement);
        when(this.resultSet.next()).thenReturn(true);
        when(this.resultSet.getInt("count")).thenReturn(2);
        when(this.preparedStatement.executeQuery()).thenReturn(this.resultSet);
    }

    @Test
    @DisplayName("getNumPets returns count")
    void getNumPetsReturnsCount() throws SQLException {
        assertEquals(2, this.mockSQLWrapper.getNumPets("Mock-Owner-Id"));

        verify(this.preparedStatement, times(1)).setString(1, "MockOwnerId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getNumPets throws exception if no count found")
    void getNumPetsThrowsExceptionWhenNoCount() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);

        Exception exception = assertThrows(SQLException.class, () -> this.mockSQLWrapper.getNumPets("Mock-Owner-Id"));

        assertEquals("Could not select count", exception.getMessage());

        verify(this.preparedStatement, times(1)).setString(1, "MockOwnerId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getNumPets rethrows normal exceptions")
    void getNumPetsRethrowsExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.getNumPets("Mock-Owner-Id"));

        verify(this.preparedStatement, times(1)).setString(1, "MockOwnerId");
        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }
}
