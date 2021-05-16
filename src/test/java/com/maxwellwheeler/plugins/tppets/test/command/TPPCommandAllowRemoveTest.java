package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.GuestManager;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPCommandAllowRemoveTest {
    private OfflinePlayer guest;
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private PetStorage pet;
    private Command command;
    private CommandTPP commandTPP;
    private GuestManager guestManager;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.guest = MockFactory.getMockOfflinePlayer("MockGuestId", "MockGuestName");
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.allowguests"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.allowguests", "tppets.allowother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, false);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("remove");
        aliases.put("remove", altAlias);
        this.pet = new PetStorage("MockPetId", 7, 100, 200, 300, "MockWorld", "MockPlayerId", "MockPetName", "MockPetName");
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        Hashtable<String, List<String>> allowedPlayers = new Hashtable<>();
        allowedPlayers.put("MockPetId", new ArrayList<>());
        allowedPlayers.get("MockPetId").add("MockGuestId");

        when(this.sqlWrapper.getAllAllowedPlayers()).thenReturn(allowedPlayers);
        this.guestManager = new GuestManager(this.sqlWrapper);
        when(tpPets.getGuestManager()).thenReturn(this.guestManager);
    }

    @Test
    @DisplayName("Removes an existing player from a pet")
    void removesPlayerFromPet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);
            when(this.sqlWrapper.removeAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, times(1)).removeAllowedPlayer(anyString(), anyString());

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockPlayerName removed permission from MockGuestName to use MockPlayerName's pet named MockPetName", capturedLogOutput);

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.BLUE + " is no longer allowed to " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin removes an existing player from a pet")
    void adminRemovesPlayerFromPet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);
            when(this.sqlWrapper.removeAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, times(1)).removeAllowedPlayer(anyString(), anyString());

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockAdminName removed permission from MockGuestName to use MockPlayerName's pet named MockPetName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.BLUE + " is no longer allowed to " + ChatColor.WHITE + "MockPlayerName's " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Silently fails when admin is not a player")
    void cannotAdminRemovePlayerFromPetNotPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.allowguests")).thenReturn(true);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(sender, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Fails to remove player when admin has insufficient permissions")
    void cannotAdminRemovePlayerFromPetInsufficientPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.admin.hasPermission("tppets.allowother")).thenReturn(false);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when f:[username] is not found")
    void cannotAdminRemovePlayerFromPetNoPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.player.hasPlayedBefore()).thenReturn(false);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Admin fails to remove player when no pet name specified")
    void cannotAdminRemovePlayerFromPetNoPetName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp remove [player name] [pet name]", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Silently fails when sender is not a player")
    void cannotRemovePlayerFromPetNotPlayer() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.allowguests")).thenReturn(true);

        String[] args = {"remove", "MockGuestName", "MockPetName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
        assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

        verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(sender, never()).sendMessage(anyString());
    }


    @Test
    @DisplayName("Fails to remove player when no pet name specified")
    void cannotRemovePlayerFromPetNoPet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            String[] args = {"remove", "MockGuestName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp remove [player name] [pet name]", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when no player with name specified")
    void cannotRemovePlayerFromPetNoTargetPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.guest.hasPlayedBefore()).thenReturn(false);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockGuestName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Fails to remove player when no valid pet name specified")
    void cannotRemovePlayerFromPetInvalidPetName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            String[] args = {"remove", "MockGuestName", "MockPetName;"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find pet: " + ChatColor.WHITE + "MockPetName;", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when db fails when finding pet")
    void cannotRemovePlayerFromPetDbSearchFail() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenThrow(new SQLException());

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when db can't find pet")
    void cannotRemovePlayerFromPetDbSearchNoResults() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(null);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find pet: " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when player already isn't allowed to pet")
    void cannotRemovePlayerFromPetAlreadyDone() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            this.guestManager.removeGuest("MockPetId", "MockGuestId");
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.RED + " is already not allowed to " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Admin fails to remove player when player already isn't allowed to pet")
    void adminCannotRemovePlayerFromPetDbSearchAlreadyDone() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            this.guestManager.removeGuest("MockPetId", "MockGuestId");
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.RED + " is already not allowed to " + ChatColor.WHITE + "MockPlayerName's " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Fails to remove player when db cannot remove player from pet")
    void cannotRemovePlayerFromPetDbCannotRemove() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);
            when(this.sqlWrapper.removeAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(false);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, times(1)).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when db fails to remove player from pet")
    void cannotRemovePlayerFromPetDbRemoveFail() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);
            when(this.sqlWrapper.removeAllowedPlayer("MockPetId", "MockGuestId")).thenThrow(new SQLException());

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, times(1)).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }
}