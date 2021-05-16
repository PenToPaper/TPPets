package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.helpers.GuestManager;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TPPGuestManagerTest {
    private GuestManager guestManager;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);

        Hashtable<String, List<String>> allowedPlayers = new Hashtable<>();
        List<String> allowedPlayer = new ArrayList<>();
        allowedPlayer.add("MockPlayerId");
        allowedPlayers.put("MockPetId", allowedPlayer);

        when(sqlWrapper.getAllAllowedPlayers()).thenReturn(allowedPlayers);
        this.guestManager = new GuestManager(sqlWrapper);
    }

    @Test
    @DisplayName("GuestManager initializes based on SQLWrapper")
    void guestManagerInitializes() {
        assertEquals(1, this.guestManager.getGuestsToPet("Mock-Pet-Id").size());
        assertTrue(this.guestManager.isGuest("Mock-Pet-Id", "Mock-Player-Id"));
    }

    @Test
    @DisplayName("GuestManager constructor rethrows SQLExceptions from SQLWrapper")
    void guestManagerConstructorRethrows() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        when(sqlWrapper.getAllAllowedPlayers()).thenThrow(new SQLException("Message"));
        SQLException exception = assertThrows(SQLException.class, () -> new GuestManager(sqlWrapper));
        assertEquals("Message", exception.getMessage());
    }

    @Test
    @DisplayName("GuestManager adds guests where pet already exists")
    void guestManagerAddGuestPetExists() {
        this.guestManager.addGuest("MockPetId", "MockPlayerId2");

        assertEquals(2, this.guestManager.getGuestsToPet("Mock-Pet-Id").size());
        assertTrue(this.guestManager.isGuest("Mock-Pet-Id", "Mock-Player-Id"));
        assertTrue(this.guestManager.isGuest("Mock-Pet-Id", "Mock-Player-Id2"));
    }

    @Test
    @DisplayName("GuestManager adds guests where pet doesn't exist")
    void guestManagerAddGuestPetNotExists() {
        this.guestManager.addGuest("Mock-Pet-Id2", "Mock-Player-Id2");

        assertEquals(1, this.guestManager.getGuestsToPet("Mock-Pet-Id").size());
        assertEquals(1, this.guestManager.getGuestsToPet("Mock-Pet-Id2").size());
        assertTrue(this.guestManager.isGuest("Mock-Pet-Id", "Mock-Player-Id"));
        assertTrue(this.guestManager.isGuest("Mock-Pet-Id2", "Mock-Player-Id2"));
    }

    @Test
    @DisplayName("GuestManager removes guests where pet already exists")
    void guestManagerRemoveGuestPetExists() {
        this.guestManager.removeGuest("Mock-Pet-Id", "Mock-Player-Id");

        assertEquals(0, this.guestManager.getGuestsToPet("Mock-Pet-Id").size());
        assertFalse(this.guestManager.isGuest("Mock-Pet-Id", "Mock-Player-Id"));
    }

    @Test
    @DisplayName("GuestManager doesn't throw error when pet doesn't exist")
    void guestManagerRemoveGuestPetNotExists() {
        this.guestManager.removeGuest("Mock-Pet-Id2", "Mock-Player-Id2");

        assertEquals(1, this.guestManager.getGuestsToPet("Mock-Pet-Id").size());
        assertTrue(this.guestManager.isGuest("Mock-Pet-Id", "Mock-Player-Id"));
    }

    @Test
    @DisplayName("GuestManager isGuest returns true if guest exists")
    void guestManagerIsGuestReturnsTrue() {
        assertTrue(this.guestManager.isGuest("Mock-Pet-Id", "Mock-Player-Id"));
    }

    @Test
    @DisplayName("GuestManager isGuest returns false if guest doesn't exist but pet does")
    void guestManagerIsGuestReturnsFalseGuest() {
        assertFalse(this.guestManager.isGuest("Mock-Pet-Id", "Mock-Player-Id2"));
    }

    @Test
    @DisplayName("GuestManager isGuest returns false if pet doesn't exist")
    void guestManagerIsGuestReturnsFalsePet() {
        assertFalse(this.guestManager.isGuest("Mock-Pet-Id2", "Mock-Player-Id"));
    }

    @Test
    @DisplayName("GuestManager isGuest returns accurate trimmed list if pet exists")
    void guestManagerIsGuestReturnsFilledList() {
        assertEquals(1, this.guestManager.getGuestsToPet("Mock-Pet-Id").size());
        assertTrue(this.guestManager.isGuest("Mock-Pet-Id", "Mock-Player-Id"));
    }

    @Test
    @DisplayName("GuestManager isGuest returns empty list if pet doesn't exist")
    void guestManagerIsGuestReturnsEmptyList() {
        assertEquals(0, this.guestManager.getGuestsToPet("Mock-Pet-Id2").size());
    }
}
