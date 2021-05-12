package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Villager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperRemovePetTest {
    private TPPets tpPets;
    private Connection connection;
    private PreparedStatement preparedStatement;
    private Horse horse;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        OfflinePlayer owner = mock(OfflinePlayer.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.horse = MockFactory.getTamedMockEntity("Mock-Pet-Id", org.bukkit.entity.Horse.class, owner);

        when(this.connection.prepareStatement("DELETE FROM tpp_unloaded_pets WHERE pet_id = ?")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(1);
    }

    @Test
    @DisplayName("removePet removes an existing pet from entity data")
    void removePetReturnsTrue() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertTrue(mockSQLWrapper.removePet(this.horse));

        verify(this.preparedStatement, times(1)).setString(1, "MockPetId");
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }

    @Test
    @DisplayName("removePet returns false when pet isn't tamed")
    void removePetReturnsFalseWhenPetNotTamed() throws SQLException {
        this.horse = MockFactory.getMockEntity("Mock-Pet-Id", org.bukkit.entity.Horse.class);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertFalse(mockSQLWrapper.removePet(this.horse));

        verify(this.preparedStatement, never()).setString(anyInt(), anyString());
        verify(this.preparedStatement, never()).executeUpdate();
        verify(this.connection, never()).close();
        verify(this.preparedStatement, never()).close();
    }

    @Test
    @DisplayName("removePet returns false when pet isn't of correct type")
    void removePetReturnsFalseWhenPetWrongType() throws SQLException {
        Villager villager = MockFactory.getMockEntity("Mock-Villager-Id", org.bukkit.entity.Villager.class);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertFalse(mockSQLWrapper.removePet(villager));

        verify(this.preparedStatement, never()).setString(anyInt(), anyString());
        verify(this.preparedStatement, never()).executeUpdate();
        verify(this.connection, never()).close();
        verify(this.preparedStatement, never()).close();
    }

    @Test
    @DisplayName("removePet returns false when execute update returns less than 0")
    void removePetReturnsFalseWhenDeleteReturnsLessThan0() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(-1);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertFalse(mockSQLWrapper.removePet(this.horse));

        verify(this.preparedStatement, times(1)).setString(1, "MockPetId");
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }

    @Test
    @DisplayName("removePet rethrows SQLException")
    void removePetRethrowsSQLException() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertThrows(SQLException.class, () -> mockSQLWrapper.removePet(this.horse));

        verify(this.preparedStatement, times(1)).setString(1, "MockPetId");
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }
}
