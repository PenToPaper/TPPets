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
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperGetAllGuestsTest {
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

        when(this.connection.prepareStatement("SELECT * FROM tpp_allowed_players ORDER BY pet_id")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeQuery()).thenReturn(this.resultSet);
        when(this.resultSet.next()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(this.resultSet.getString("pet_id")).thenReturn("MockPetId").thenReturn("MockPetId").thenReturn("MockPetId2");
        when(this.resultSet.getString("user_id")).thenReturn("MockUserId").thenReturn("MockUserId2").thenReturn("MockUserId");
    }

    @Test
    @DisplayName("getAllGuests returns a complete hashtable")
    void getAllGuestsReturnsHashtable() throws SQLException {
        Hashtable<String, List<String>> guests = this.mockSQLWrapper.getAllGuests();

        assertNotNull(guests);
        assertEquals(2, guests.size());
        assertEquals(2, guests.get("MockPetId").size());
        assertEquals("MockUserId", guests.get("MockPetId").get(0));
        assertEquals("MockUserId2", guests.get("MockPetId").get(1));
        assertEquals(1, guests.get("MockPetId2").size());
        assertEquals("MockUserId", guests.get("MockPetId2").get(0));

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getAllGuests returns an empty hashtable on no results")
    void getAllGuestsReturnsEmptyHashtable() throws SQLException {
        when(this.resultSet.next()).thenReturn(false);

        Hashtable<String, List<String>> guests = this.mockSQLWrapper.getAllGuests();

        assertNotNull(guests);
        assertEquals(0, guests.size());

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }

    @Test
    @DisplayName("getAllGuests rethrows SQLExceptions")
    void getAllGuestsRethrowsExceptions() throws SQLException {
        when(this.resultSet.next()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.getAllGuests());

        verify(this.preparedStatement, times(1)).executeQuery();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
        verify(this.resultSet, times(1)).close();
    }
}
