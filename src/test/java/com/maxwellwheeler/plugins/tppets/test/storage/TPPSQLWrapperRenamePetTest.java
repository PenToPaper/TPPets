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

public class TPPSQLWrapperRenamePetTest {
    private Connection connection;
    private PreparedStatement preparedStatement;
    private ArgumentCaptor<String> stringCaptor;
    private ArgumentCaptor<Integer> stringIndexCaptor;
    private MockSQLWrapper mockSQLWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false, false);
        this.connection = mock(Connection.class);
        this.preparedStatement = mock(PreparedStatement.class);
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.stringIndexCaptor = ArgumentCaptor.forClass(Integer.class);

        when(this.connection.prepareStatement("UPDATE tpp_unloaded_pets SET pet_name = ?, effective_pet_name = ? WHERE owner_id = ? AND effective_pet_name = ?")).thenReturn(this.preparedStatement);
        when(this.preparedStatement.executeUpdate()).thenReturn(1);

        this.mockSQLWrapper = new MockSQLWrapper(tpPets, this.connection);
    }

    @Test
    @DisplayName("renamePet returns true")
    void renamePetReturnsTrue() throws SQLException {
        assertTrue(this.mockSQLWrapper.renamePet("Mock-Owner-Id", "OldPetName", "NewPetName"));

        verify(this.preparedStatement, times(4)).setString(this.stringIndexCaptor.capture(), this.stringCaptor.capture());
        List<Integer> stringIndexes = this.stringIndexCaptor.getAllValues();
        List<String> preparedStrings = this.stringCaptor.getAllValues();

        assertEquals(1, stringIndexes.get(0));
        assertEquals("NewPetName", preparedStrings.get(0));

        assertEquals(2, stringIndexes.get(1));
        assertEquals("newpetname", preparedStrings.get(1));

        assertEquals(3, stringIndexes.get(2));
        assertEquals("MockOwnerId", preparedStrings.get(2));

        assertEquals(4, stringIndexes.get(3));
        assertEquals("oldpetname", preparedStrings.get(3));

        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("renamePet returns false on no affected rows")
    void renamePetReturnsFalse() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenReturn(-1);

        assertFalse(this.mockSQLWrapper.renamePet("Mock-Owner-Id", "OldPetName", "NewPetName"));

        verify(this.preparedStatement, times(4)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.preparedStatement, times(1)).close();
        verify(this.connection, times(1)).close();
    }

    @Test
    @DisplayName("insertPet function rethrows SQLExceptions")
    void insertPetRethrowsSQLExceptions() throws SQLException {
        when(this.preparedStatement.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> mockSQLWrapper.renamePet("Mock-Owner-Id", "OldPetName", "NewPetName"));

        verify(this.preparedStatement, times(4)).setString(anyInt(), anyString());
        verify(this.preparedStatement, times(1)).executeUpdate();
        verify(this.connection, times(1)).close();
        verify(this.preparedStatement, times(1)).close();
    }
}
