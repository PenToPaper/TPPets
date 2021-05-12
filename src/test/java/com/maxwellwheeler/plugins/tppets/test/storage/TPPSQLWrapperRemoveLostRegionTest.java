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
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperRemoveLostRegionTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private MockSQLWrapper mockSQLWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);

        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);

        when(this.connection.prepareStatement("DELETE FROM tpp_lost_regions WHERE zone_name = ?")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(0);
    }

    @Test
    @DisplayName("removeLostRegion returns true")
    void removeLostRegionReturnsTrue() throws SQLException {
        assertTrue(this.mockSQLWrapper.removeLostRegion("MockRegionName"));

        verify(this.preparedStatement, times(1)).setString(1, "MockRegionName");
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("removeLostRegion returns false when negative result from update")
    void removeLostRegionReturnsFalse() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(-1);

        assertFalse(this.mockSQLWrapper.removeLostRegion("MockRegionName"));

        verify(this.preparedStatement, times(1)).setString(1, "MockRegionName");
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("removeLostRegion rethrows exceptions")
    void removeLostRegionRethrowsExceptions() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.mockSQLWrapper.removeLostRegion("MockRegionName"));

        verify(this.preparedStatement, times(1)).setString(1, "MockRegionName");
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }
}
