package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPCommandServerStorageAddTest {
    private Location adminLocation;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private Command command;
    private CommandTPP commandTPP;
    private World world;

    @BeforeEach
    public void beforeEach() {
        this.world = mock(World.class);
        when(this.world.getName()).thenReturn("MockWorld");
        this.adminLocation = MockFactory.getMockLocation(this.world, 400, 500, 600);
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, this.adminLocation, new String[]{"tppets.storage", "tppets.serverstorage", "tppets.storageother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("serverstorage");
        aliases.put("serverstorage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - serverstorage add - " + commandStatus.toString(), logCaptor.getValue());
    }

    @Test
    @DisplayName("Adds storage locations to the database")
    void addStorageLocation() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);
        when(this.sqlWrapper.insertServerStorageLocation("default", this.adminLocation)).thenReturn(true);

        String[] args = {"serverstorage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.sqlWrapper, times(1)).insertServerStorageLocation(anyString(), any(Location.class));

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("MockAdminName - serverstorage add - added default in MockWorld", capturedLogOutput);

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "You have added server storage location " + ChatColor.WHITE + "default", capturedMessageOutput);
    }

    @Test
    @DisplayName("Adds storage location based on sender, even if f:[username] syntax used")
    void addStorageLocationBasedOnSender() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            OfflinePlayer player = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(player);

            when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);
            when(this.sqlWrapper.insertServerStorageLocation("default", this.adminLocation)).thenReturn(true);

            String[] args = {"serverstorage", "f:MockPlayerName", "add", "default"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).insertServerStorageLocation(anyString(), any(Location.class));

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockAdminName - serverstorage add - added default in MockWorld", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "You have added server storage location " + ChatColor.WHITE + "default", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Adds non-default locations to the database")
    void addNonDefaultStorageLocation() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("notdefault", this.world)).thenReturn(null);
        when(this.sqlWrapper.insertServerStorageLocation("notdefault", this.adminLocation)).thenReturn(true);

        String[] args = {"serverstorage", "add", "notdefault"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.sqlWrapper, times(1)).insertServerStorageLocation(anyString(), any(Location.class));

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("MockAdminName - serverstorage add - added notdefault in MockWorld", capturedLogOutput);

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "You have added server storage location " + ChatColor.WHITE + "notdefault", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when database cannot add entry")
    void cannotAddStorageLocationDBCannotAdd() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);
        when(this.sqlWrapper.insertServerStorageLocation("default", this.adminLocation)).thenReturn(false);

        String[] args = {"serverstorage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(1)).insertServerStorageLocation(anyString(), any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add server storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when database fails adding new entry")
    void cannotAddStorageLocationDBFailAdding() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);
        when(this.sqlWrapper.insertServerStorageLocation("default", this.adminLocation)).thenThrow(new SQLException());

        String[] args = {"serverstorage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(1)).insertServerStorageLocation(anyString(), any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add server storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when database fails finding existing entry")
    void cannotAddStorageLocationDBFailGetting() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenThrow(new SQLException());
        when(this.sqlWrapper.insertServerStorageLocation("default", this.adminLocation)).thenReturn(true);

        String[] args = {"serverstorage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, never()).insertServerStorageLocation(anyString(), any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add server storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Does not add server storage location if it already exists")
    void cannotAddStorageLocationAlreadyExists() throws SQLException {
        ServerStorageLocation storageLocation = mock(ServerStorageLocation.class);
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(storageLocation);

        String[] args = {"serverstorage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.ALREADY_DONE);

        verify(this.sqlWrapper, never()).insertServerStorageLocation(anyString(), any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Server storage " + ChatColor.WHITE + "default" + ChatColor.RED + " already exists", capturedMessageOutput);
    }
}