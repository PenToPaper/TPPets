package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Using /tpp storage list to test response to syntax errors
public class TPPCommandStorageTest {
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private Command command;
    private CommandTPP commandTPP;
    private List<PlayerStorageLocation> storageLocations;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach(){
        // Players
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.storage"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.storage", "tppets.storageother", "tppets.bypassstoragelimit"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);

        // Plugin
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);

        // Command
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("storage");
        aliases.put("storage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        // Database
        World world = mock(World.class);
        when(world.getName()).thenReturn("MockWorld");
        PlayerStorageLocation locationOne = MockFactory.getPlayerStorageLocation("StorageOne", "MockPlayerId", 100, 200, 300, world);
        PlayerStorageLocation locationTwo = MockFactory.getPlayerStorageLocation("StorageTwo", "MockPlayerId", 400, 500, 600, world);
        this.storageLocations = new ArrayList<>();
        this.storageLocations.add(locationOne);
        this.storageLocations.add(locationTwo);
    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - storage - " + commandStatus.toString(), logCaptor.getValue());
    }

    @Test
    @DisplayName("Non-player sending command denies action silently")
    void nonPlayerSendingCommand() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.storage")).thenReturn(true);

        String[] args = {"storage", "list"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verify(this.sqlWrapper, never()).getStorageLocations(anyString());
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Non-player sending command for another player denies action silently")
    void nonPlayerSendingCommandForAnother() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.storage")).thenReturn(true);

            String[] args = {"storage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            verifyLoggedUnsuccessfulAction("Unknown Sender", CommandStatus.INVALID_SENDER);

            verify(this.sqlWrapper, never()).getStorageLocations(anyString());
            verify(sender, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Admin insufficient permissions username with f:[username] syntax")
    void adminInsufficientForOthersPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);
            when(this.admin.hasPermission("tppets.storageother")).thenReturn(false);

            String[] args = {"storage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.INSUFFICIENT_PERMISSIONS);

            verify(this.sqlWrapper, never()).getStorageLocations(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();

            assertEquals(ChatColor.RED + "You don't have permission to do that", message);
        }
    }

    @Test
    @DisplayName("Admin incorrect username with f:[username] syntax")
    void adminIncorrectName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(this.storageLocations);

            String[] args = {"storage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.sqlWrapper, never()).getStorageLocations(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();

            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", message);
        }
    }

    @Test
    @DisplayName("Admins invalid username with f:[username] syntax")
    void adminInvalidName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocations("MockAdminId")).thenReturn(this.storageLocations);

            String[] args = {"storage", "f:MockPlayerName;", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.sqlWrapper, never()).getStorageLocations(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();

            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName;", message);
        }
    }

    @Test
    @DisplayName("Admins no action with command using f:[username] syntax")
    void adminNoAction() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocations("MockAdminId")).thenReturn(this.storageLocations);

            String[] args = {"storage", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

            verify(this.sqlWrapper, never()).getStorageLocations(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();

            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add/remove/list/server]", message);
        }
    }

    @Test
    @DisplayName("No action with command")
    void playerNoAction() {
        String[] args = {"storage"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.SYNTAX_ERROR);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();

        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add/remove/list/server]", message);
    }

    @Test
    @DisplayName("Invalid action with command")
    void playerInvalidAction() {
        String[] args = {"storage", "invalid"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.SYNTAX_ERROR);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();

        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add/remove/list/server]", message);
    }
}
