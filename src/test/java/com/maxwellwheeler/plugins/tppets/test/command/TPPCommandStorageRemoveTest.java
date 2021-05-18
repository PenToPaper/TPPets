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
import org.bukkit.command.Command;
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

public class TPPCommandStorageRemoveTest {
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private PlayerStorageLocation storageLocation;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach(){
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.storage"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.storage", "tppets.storageother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.storageLocation = mock(PlayerStorageLocation.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("storage");
        aliases.put("storage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - storage remove - " + commandStatus.toString(), logCaptor.getValue());
    }

    @Test
    @DisplayName("Removes storage locations from the database")
    void removeStorageLocation() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
        when(this.sqlWrapper.removeStorageLocation("MockPlayerId", "StorageName")).thenReturn(true);

        String[] args = {"storage", "remove", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).removeStorageLocation(anyString(), anyString());

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("MockPlayerName - storage remove - removed StorageName from MockPlayerName", capturedLogOutput);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "Storage location " + ChatColor.WHITE + "StorageName" + ChatColor.BLUE + " has been removed", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admin removes storage locations for other people from the database")
    void adminRemoveStorageLocation() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
            when(this.sqlWrapper.removeStorageLocation("MockPlayerId", "StorageName")).thenReturn(true);

            String[] args = {"storage", "f:MockPlayerName", "remove", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).removeStorageLocation(anyString(), anyString());

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockAdminName - storage remove - removed StorageName from MockPlayerName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's" + ChatColor.BLUE + " location " + ChatColor.WHITE + "StorageName" + ChatColor.BLUE + " has been removed", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Cannot remove storage locations that do not exist")
    void cannotRemoveNonExistentStorage() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);

        String[] args = {"storage", "remove", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.ALREADY_DONE);

        verify(this.sqlWrapper, never()).removeStorageLocation(anyString(), anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Storage location " + ChatColor.WHITE + "StorageName" + ChatColor.RED + " already does not exist", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot remove storage location without argument provided")
    void cannotRemoveNonProvidedStorage() throws SQLException {
        String[] args = {"storage", "remove"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, never()).removeStorageLocation(anyString(), anyString());

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.SYNTAX_ERROR);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp storage remove [storage name]", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admins cannot remove storage locations for other people that do not exist")
    void adminCannotRemoveNonExistentStorage() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);

            String[] args = {"storage", "f:MockPlayerName", "remove", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.ALREADY_DONE);

            verify(this.sqlWrapper, never()).removeStorageLocation(anyString(), anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's" + ChatColor.RED + " storage location " + ChatColor.WHITE + "StorageName" + ChatColor.RED + " already does not exist", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Reports database cannot to remove storage to user")
    void reportsDbCannotRemove() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
        when(this.sqlWrapper.removeStorageLocation("MockPlayerId", "StorageName")).thenReturn(false);

        String[] args = {"storage", "remove", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(1)).removeStorageLocation(anyString(), anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not remove storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when removing storage to user")
    void reportsDbFailureToRemove() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
        when(this.sqlWrapper.removeStorageLocation("MockPlayerId", "StorageName")).thenThrow(new SQLException());

        String[] args = {"storage", "remove", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(1)).removeStorageLocation(anyString(), anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not remove storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when getting existing storage to user")
    void reportsDbFailureToGet() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenThrow(new SQLException());
        when(this.sqlWrapper.removeStorageLocation("MockPlayerId", "StorageName")).thenReturn(true);

        String[] args = {"storage", "remove", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, never()).removeStorageLocation(anyString(), anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not remove storage location", capturedMessageOutput);
    }
}
