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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperRemoveAllowedPlayerTest {
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

        when(this.connection.prepareStatement("DELETE FROM tpp_allowed_players WHERE pet_id = ? AND user_id = ?")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(0);
    }

    @Test
    @DisplayName("removeAllowedPlayer returns true")
    void removeAllowedPlayerReturnsTrue() throws SQLException {
        assertTrue(this.mockSQLWrapper.removeAllowedPlayer("Mock-Pet-Id", "Mock-Player-Id"));

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
    @DisplayName("removeAllowedPlayer returns false if no rows deleted")
    void removeAllowedPlayerReturnsFalse() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(-1);

        assertFalse(this.mockSQLWrapper.removeAllowedPlayer("Mock-Pet-Id", "Mock-Player-Id"));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("removeAllowedPlayer rethrows exceptions")
    void removeAllowedPlayerRethrowsExceptions() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.removeAllowedPlayer("Mock-Pet-Id", "Mock-Player-Id"));

        verify(this.preparedStatement, times(2)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
