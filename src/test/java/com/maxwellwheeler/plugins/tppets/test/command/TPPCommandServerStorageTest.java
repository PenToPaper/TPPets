package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation;
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
import static org.mockito.Mockito.*;

public class TPPCommandServerStorageTest {
    private Player admin;
    private Player player;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach(){
        // Players
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.serverstorage", "tppets.storageother", "tppets.bypassstoragelimit"});
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});

        // Plugin
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);

        // Command
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("serverstorage");
        aliases.put("serverstorage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        // Database
        World world = mock(World.class);
        when(world.getName()).thenReturn("MockWorld");
        ServerStorageLocation locationOne = MockFactory.getServerStorageLocation("StorageOne", 100, 200, 300, world);
        ServerStorageLocation locationTwo = MockFactory.getServerStorageLocation("StorageTwo", 400, 500, 600, world);
        List<ServerStorageLocation> storageLocations = new ArrayList<>();
        storageLocations.add(locationOne);
        storageLocations.add(locationTwo);
    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - serverstorage - " + commandStatus.toString(), logCaptor.getValue());
    }

    @Test
    @DisplayName("Non-player sending command denies action silently")
    void nonPlayerSendingCommand() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.serverstorage")).thenReturn(true);

        String[] args = {"serverstorage", "list"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verify(this.sqlWrapper, never()).getServerStorageLocation(anyString(), any(World.class));
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Non-player sending command for another player denies action silently")
    void nonPlayerSendingCommandForAnother() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.serverstorage")).thenReturn(true);

            String[] args = {"serverstorage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            verifyLoggedUnsuccessfulAction("Unknown Sender", CommandStatus.INVALID_SENDER);

            verify(this.sqlWrapper, never()).getServerStorageLocation(anyString(), any(World.class));
            verify(sender, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Admin insufficient permissions username with f:[username] syntax")
    void adminInsufficientForOthersPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);
            when(this.admin.hasPermission("tppets.storageother")).thenReturn(false);

            String[] args = {"serverstorage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.INSUFFICIENT_PERMISSIONS);

            verify(this.sqlWrapper, never()).getStorageLocations(anyString());

            verify(this.admin, times(1)).sendMessage(anyString());
            verify(this.admin, times(1)).sendMessage(ChatColor.RED + "You don't have permission to do that");
        }
    }

    @Test
    @DisplayName("Admin incorrect username with f:[username] syntax")
    void adminIncorrectName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"serverstorage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.sqlWrapper, never()).getServerStorageLocations(any(World.class));

            verify(this.admin, times(1)).sendMessage(anyString());
            verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName");
        }
    }

    @Test
    @DisplayName("Admin invalid username with f:[username] syntax")
    void adminInvalidName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"serverstorage", "f:MockPlayerName;", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.sqlWrapper, never()).getServerStorageLocations(any(World.class));

            verify(this.admin, times(1)).sendMessage(anyString());
            verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName;");
        }
    }

    @Test
    @DisplayName("Admin no action with command using f:[username] syntax")
    void adminNoAction() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"serverstorage", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

            verify(this.sqlWrapper, never()).getServerStorageLocations(any(World.class));

            verify(this.admin, times(1)).sendMessage(anyString());
            verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp serverstorage [add/remove/list]");
        }
    }

    @Test
    @DisplayName("No action with command")
    void playerNoAction() throws SQLException {
        String[] args = {"serverstorage"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

        verify(this.sqlWrapper, never()).getServerStorageLocations(any(World.class));

        verify(this.admin, times(1)).sendMessage(anyString());
        verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp serverstorage [add/remove/list]");
    }

    @Test
    @DisplayName("Invalid action with command")
    void playerInvalidAction() throws SQLException {
        String[] args = {"serverstorage", "invalid"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

        verify(this.sqlWrapper, never()).getServerStorageLocations(any(World.class));

        verify(this.admin, times(1)).sendMessage(anyString());
        verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp serverstorage [add/remove/list]");
    }
}
