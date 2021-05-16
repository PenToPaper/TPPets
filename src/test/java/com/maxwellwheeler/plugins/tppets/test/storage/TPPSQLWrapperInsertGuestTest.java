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
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperInsertGuestTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private MockSQLWrapper mockSQLWrapper;
    private ArgumentCaptor<Integer> indexCaptor;
    private ArgumentCaptor<String> stringCaptor;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);

        this.indexCaptor = ArgumentCaptor.forClass(Integer.class);
        this.stringCaptor = ArgumentCaptor.forClass(String.class);

        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        when(this.connection.prepareStatement("INSERT INTO tpp_allowed_players (pet_id, user_id) VALUES (?, ?)")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("insertGuest returns true")
    void insertGuestReturnsTrue() throws SQLException {
        assertTrue(this.mockSQLWrapper.insertGuest("Mock-Pet-Id", "Mock-Player-Id"));

        verify(this.preparedStatement, times(2)).setString(this.indexCaptor.capture(), this.stringCaptor.capture());
        List<String> values = this.stringCaptor.getAllValues();
        List<Integer> indexes = this.indexCaptor.getAllValues();

        assertEquals(1, indexes.get(0));
        assertEquals("MockPetId", values.get(0));

        assertEquals(2, indexes.get(1));
        assertEquals("MockPlayerId", values.get(1));

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("insertGuest returns false if no rows inserted")
    void insertGuestReturnsFalse() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(0);

        assertFalse(this.mockSQLWrapper.insertGuest("Mock-Pet-Id", "Mock-Player-Id"));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("insertGuest rethrows exceptions")
    void insertGuestRethrowsExceptions() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.insertGuest("Mock-Pet-Id", "Mock-Player-Id"));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
