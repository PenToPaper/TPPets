package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetLimitChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPPetLimitCheckerTest {
    private PetLimitChecker petLimitChecker;
    private OfflinePlayer owner;
    private SQLWrapper sqlWrapper;
    private TPPets tpPets;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, logWrapper, false, false, false);
        this.petLimitChecker = new PetLimitChecker(this.tpPets, 2, 1, 1, 1, 1, 1, 1, 3);
        this.owner = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");

        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(1);
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.DOG)).thenReturn(0);
    }

    @Test
    @DisplayName("getSpecificLimit returns specific limit based on constructor")
    void getSpecificLimit() {
        assertEquals(3, this.petLimitChecker.getSpecificLimit(PetType.Pets.DONKEY));
    }

    @Test
    @DisplayName("getSpecificLimit returns -1 if unknown pet supplied")
    void getSpecificLimitUnknown() {
        assertEquals(-1, this.petLimitChecker.getSpecificLimit(PetType.Pets.UNKNOWN));
    }

    @Test
    @DisplayName("getTotalLimit returns total limit based on constructor")
    void getTotalLimit() {
        assertEquals(2, this.petLimitChecker.getTotalLimit());
    }

    @Test
    @DisplayName("isWithinTotalLimit returns true if less than limit")
    void isWithinTotalLimitReturnsTrue() throws SQLException {
        assertTrue(this.petLimitChecker.isWithinTotalLimit(this.owner));

        verify(this.sqlWrapper, times(1)).getNumPets("MockPlayerId");
    }

    @Test
    @DisplayName("isWithinTotalLimit returns false if equal to limit")
    void isWithinTotalLimitReturnsFalseWhenEqual() throws SQLException {
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(2);

        assertFalse(this.petLimitChecker.isWithinTotalLimit(this.owner));

        verify(this.sqlWrapper, times(1)).getNumPets("MockPlayerId");
    }

    @Test
    @DisplayName("isWithinTotalLimit returns false if over limit")
    void isWithinTotalLimitReturnsFalseWhenOver() throws SQLException {
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(3);

        assertFalse(this.petLimitChecker.isWithinTotalLimit(this.owner));

        verify(this.sqlWrapper, times(1)).getNumPets("MockPlayerId");
    }

    @Test
    @DisplayName("isWithinTotalLimit returns true if no limit")
    void isWithinTotalLimitReturnsTrueWhenNoLimit() throws SQLException {
        this.petLimitChecker = new PetLimitChecker(this.tpPets, -1, 1, 1, 1, 1, 1, 1, 3);

        assertTrue(this.petLimitChecker.isWithinTotalLimit(this.owner));

        verify(this.sqlWrapper, times(1)).getNumPets("MockPlayerId");
    }

    @Test
    @DisplayName("isWithinSpecificLimit returns true if less than limit")
    void isWithinSpecificLimitReturnsTrue() throws SQLException {
        assertTrue(this.petLimitChecker.isWithinSpecificLimit(this.owner, PetType.Pets.DOG));

        verify(this.sqlWrapper, times(1)).getNumPetsByPetType("MockPlayerId", PetType.Pets.DOG);
    }

    @Test
    @DisplayName("isWithinSpecificLimit returns false if equal to limit")
    void isWithinSpecificLimitReturnsFalseWhenEqual() throws SQLException {
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.DOG)).thenReturn(1);

        assertFalse(this.petLimitChecker.isWithinSpecificLimit(this.owner, PetType.Pets.DOG));

        verify(this.sqlWrapper, times(1)).getNumPetsByPetType("MockPlayerId", PetType.Pets.DOG);
    }

    @Test
    @DisplayName("isWithinSpecificLimit returns false if over limit")
    void isWithinSpecificLimitReturnsFalseWhenOver() throws SQLException {
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.DOG)).thenReturn(2);

        assertFalse(this.petLimitChecker.isWithinSpecificLimit(this.owner, PetType.Pets.DOG));

        verify(this.sqlWrapper, times(1)).getNumPetsByPetType("MockPlayerId", PetType.Pets.DOG);
    }

    @Test
    @DisplayName("isWithinSpecificLimit returns true if no limit")
    void isWithinSpecificLimitReturnsTrueWhenNoLimit() throws SQLException {
        this.petLimitChecker = new PetLimitChecker(this.tpPets, 2, -1, 1, 1, 1, 1, 1, 3);

        assertTrue(this.petLimitChecker.isWithinSpecificLimit(this.owner, PetType.Pets.DOG));

        verify(this.sqlWrapper, times(1)).getNumPetsByPetType("MockPlayerId", PetType.Pets.DOG);
    }
}
