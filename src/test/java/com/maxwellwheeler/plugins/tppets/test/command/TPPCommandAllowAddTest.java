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

public class TPPCommandAllowAddTest {
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
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("allow");
        aliases.put("allow", altAlias);
        this.pet = new PetStorage("MockPetId", 7, 100, 200, 300, "MockWorld", "MockPlayerId", "MockPetName", "MockPetName");
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        when(this.sqlWrapper.getAllAllowedPlayers()).thenReturn(new Hashtable<>());
        this.guestManager = new GuestManager(this.sqlWrapper);
        when(tpPets.getGuestManager()).thenReturn(this.guestManager);
    }

    @Test
    @DisplayName("Adds a new player to a pet")
    void addsPlayerToPet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);
            when(this.sqlWrapper.insertAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, times(1)).insertAllowedPlayer(anyString(), anyString());

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockPlayerName allowed MockGuestName to use MockPlayerName's pet named MockPetName", capturedLogOutput);

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.BLUE + " is now allowed to " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin adds new players to pet")
    void adminAddPlayerToPet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);
            when(this.sqlWrapper.insertAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.guestManager.getGuestsToPet("MockPetId").size());
            assertTrue(this.guestManager.isGuest("MockPetId", "MockGuestId"));

            verify(this.sqlWrapper, times(1)).insertAllowedPlayer(anyString(), anyString());

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockAdminName allowed MockGuestName to use MockPlayerName's pet named MockPetName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.BLUE + " is now allowed to " + ChatColor.WHITE + "MockPlayerName's " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Silently fails when admin is not user")
    void cannotAdminAddPlayerToPetNotPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.allowguests")).thenReturn(true);
            when(sender.hasPermission("tppets.allowother")).thenReturn(true);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());
        }
    }


    @Test
    @DisplayName("Admin insufficient permissions username with f:[username] syntax")
    void cannotAdminAddPlayerToPetInsufficientPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.admin.hasPermission("tppets.allowother")).thenReturn(false);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin cannot add player to pet without valid user")
    void cannotAdminAddPlayerToPetNoPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.hasPlayedBefore()).thenReturn(false);

            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.admin.hasPermission("tppets.allowother")).thenReturn(false);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin cannot add player to pet without target pet")
    void cannotAdminAddPlayerToPetNoTargetPet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp allow [player name] [pet name]", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Silently fails when player is not user")
    void cannotAddPlayerToPetNotPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.allowguests")).thenReturn(true);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());
        }
    }

    @Test
    @DisplayName("Cannot add player to pet without target pet")
    void cannotAddPlayerToPetNoTargetPet() throws SQLException {
        String[] args = {"allow", "MockGuestName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

        verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp allow [player name] [pet name]", capturedMessageOutput);
    }


    @Test
    @DisplayName("Cannot add player to pet without target player")
    void cannotAddPlayerToPetNoTargetPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.guest.hasPlayedBefore()).thenReturn(false);

            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockGuestName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Cannot add player to pet with invalid pet name")
    void cannotAddPlayerToPetInvalidPetName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            String[] args = {"allow", "MockGuestName", "MockPetName;"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  "MockPetName;", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Cannot add player to pet when database fails to find pet")
    void cannotAddPlayerToPetDbFailPet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenThrow(new SQLException());

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Cannot add player to pet when no pet in database with name")
    void cannotAddPlayerToPetNoPetWithName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(null);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Cannot add player to pet when player is already added to pet")
    void cannotAddPlayerToPetAlreadyDone() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            this.guestManager.addGuest("MockPetId", "MockGuestId");

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.RED + " is already allowed to " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Admin cannot add player to pet when player is already added to pet")
    void cannotAdminAddPlayerToPetAlreadyDone() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            this.guestManager.addGuest("MockPetId", "MockGuestId");

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.RED + " is already allowed to " + ChatColor.WHITE + "MockPlayerName's " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Cannot add player to pet when database can't to add player to pet")
    void cannotAddPlayerToPetDbCannotAdd() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);
            when(this.sqlWrapper.insertAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(false);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, times(1)).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Cannot add player to pet when database fails when adding player to pet")
    void cannotAddPlayerToPetDbFailAdd() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MockPetName")).thenReturn(this.pet);
            when(this.sqlWrapper.insertAllowedPlayer("MockPetId", "MockGuestId")).thenThrow(new SQLException());

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.guestManager.getGuestsToPet("MockPetId").size());

            verify(this.sqlWrapper, times(1)).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }
}
