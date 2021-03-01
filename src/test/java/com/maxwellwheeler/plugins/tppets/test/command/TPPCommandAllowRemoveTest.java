package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TPPCommandAllowRemoveTest {
    private OfflinePlayer guest;
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private List<PetStorage> petStorageList;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private Hashtable<String, List<String>> allowedPlayers;

    @BeforeEach
    public void beforeEach() {
        this.guest = MockFactory.getMockOfflinePlayer("MockGuestId", "MockGuestName");
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.allowguests"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.allowguests", "tppets.allowother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("remove");
        aliases.put("remove", altAlias);
        this.petStorageList = Collections.singletonList(new PetStorage("MockPetId", 7, 100, 200, 300, "MockWorld", "MockPlayerId", "MockPetName", "MockPetName"));
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        this.allowedPlayers = new Hashtable<>();
        this.allowedPlayers.put("MockPetId", new ArrayList<>());
        this.allowedPlayers.get("MockPetId").add("MockGuestId");
    }

    @Test
    @DisplayName("Removes an existing player from a pet")
    void removesPlayerFromPet() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.dbWrapper.removeAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(0, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, times(1)).removeAllowedPlayer(anyString(), anyString());

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
    void adminRemovesPlayerFromPet() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.dbWrapper.removeAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(0, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, times(1)).removeAllowedPlayer(anyString(), anyString());

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
    void cannotAdminRemovePlayerFromPetNotPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.allowguests")).thenReturn(true);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(sender, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Fails to remove player when admin has insufficient permissions")
    void cannotAdminRemovePlayerFromPetInsufficientPermissions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.admin.hasPermission("tppets.allowother")).thenReturn(false);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when f:[username] is not found")
    void cannotAdminRemovePlayerFromPetNoPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.player.hasPlayedBefore()).thenReturn(false);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Admin fails to remove player when no pet name specified")
    void cannotAdminRemovePlayerFromPetNoPetName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp remove [player name] [pet name]", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Silently fails when sender is not a player")
    void cannotRemovePlayerFromPetNotPlayer() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.allowguests")).thenReturn(true);

        String[] args = {"remove", "MockGuestName", "MockPetName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        assertEquals(1, this.allowedPlayers.size());
        assertTrue(this.allowedPlayers.containsKey("MockPetId"));
        assertEquals(1, this.allowedPlayers.get("MockPetId").size());

        verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(sender, never()).sendMessage(anyString());
    }


    @Test
    @DisplayName("Fails to remove player when no pet name specified")
    void cannotRemovePlayerFromPetNoPet() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            String[] args = {"remove", "MockGuestName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp remove [player name] [pet name]", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when no player with name specified")
    void cannotRemovePlayerFromPetNoTargetPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.guest.hasPlayedBefore()).thenReturn(false);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockGuestName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Fails to remove player when no valid pet name specified")
    void cannotRemovePlayerFromPetInvalidPetName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            String[] args = {"remove", "MockGuestName", "MockPetName;"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find pet: " + ChatColor.WHITE + "MockPetName;", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when db fails when finding pet")
    void cannotRemovePlayerFromPetDbSearchFail() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(null);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when db can't find pet")
    void cannotRemovePlayerFromPetDbSearchNoResults() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(new ArrayList<>());

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find pet: " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to remove player when player already isn't allowed to pet")
    void cannotRemovePlayerFromPetDbSearchAlreadyDone() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            List<String> allowed = new ArrayList<>();
            allowed.add("MockPlayerId");
            this.allowedPlayers.put("MockPetId", allowed);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.RED + " is already not allowed to " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Admin fails to remove player when player already isn't allowed to pet")
    void adminCannotRemovePlayerFromPetDbSearchAlreadyDone() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            List<String> allowed = new ArrayList<>();
            allowed.add("MockPlayerId");
            this.allowedPlayers.put("MockPetId", allowed);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"remove", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, never()).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.RED + " is already not allowed to " + ChatColor.WHITE + "MockPlayerName's " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Fails to remove player when db fails when removing player from pet")
    void cannotRemovePlayerFromPetDbRemoveFail() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.dbWrapper.removeAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(false);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"remove", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());

            verify(this.dbWrapper, times(1)).removeAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }
}