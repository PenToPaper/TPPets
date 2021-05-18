package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPCommandStorageRemoveDefaultTest {
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
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, null, new String[]{"tppets.storage", "tppets.setdefaultstorage"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
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
        assertEquals(expectedPlayerName + " - storage remove default - " + commandStatus.toString(), logCaptor.getValue());
    }

    @Test
    @DisplayName("Removes default storage locations from the database")
    void removeStorageLocation() throws SQLException {
        ServerStorageLocation storageLocation = mock(ServerStorageLocation.class);
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(storageLocation);
        when(this.sqlWrapper.removeServerStorageLocation("default", this.world)).thenReturn(true);

        String[] args = {"storage", "remove", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.sqlWrapper, times(1)).removeServerStorageLocation(anyString(), any(World.class));

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("MockAdminName - storage remove default - removed default from MockWorld", capturedLogOutput);

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "Server storage " + ChatColor.WHITE + "default" + ChatColor.BLUE + " in " + ChatColor.WHITE + "MockWorld" + ChatColor.BLUE + " has been removed", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when database fails when getting existing entry")
    void cannotRemoveStorageLocationDbFailGetting() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenThrow(new SQLException());
        when(this.sqlWrapper.removeServerStorageLocation("default", this.world)).thenReturn(true);

        String[] args = {"storage", "remove", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, never()).removeServerStorageLocation(anyString(), any(World.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not remove sever storage location" + ChatColor.WHITE + "default", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when database cannot remove entry")
    void cannotRemoveStorageLocationDbCannotRemove() throws SQLException {
        ServerStorageLocation storageLocation = mock(ServerStorageLocation.class);
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(storageLocation);
        when(this.sqlWrapper.removeServerStorageLocation("default", this.world)).thenReturn(false);

        String[] args = {"storage", "remove", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(1)).removeServerStorageLocation(anyString(), any(World.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not remove sever storage location" + ChatColor.WHITE + "default", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when database fails when removing entry")
    void cannotRemoveStorageLocationDbFailRemoving() throws SQLException {
        ServerStorageLocation storageLocation = mock(ServerStorageLocation.class);
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(storageLocation);
        when(this.sqlWrapper.removeServerStorageLocation("default", this.world)).thenThrow(new SQLException());

        String[] args = {"storage", "remove", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(1)).removeServerStorageLocation(anyString(), any(World.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not remove sever storage location" + ChatColor.WHITE + "default", capturedMessageOutput);
    }

    @Test
    @DisplayName("Does not add server storage location if it already does not exist")
    void cannotRemoveStorageLocationAlreadyDoesNotExist() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);

        String[] args = {"storage", "remove", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.ALREADY_DONE);

        verify(this.sqlWrapper, never()).removeServerStorageLocation(anyString(), any(World.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Server storage " + ChatColor.WHITE + "default" + ChatColor.BLUE + " in" + ChatColor.WHITE + "MockWorld" + ChatColor.RED + " already does not exist", capturedMessageOutput);
    }
}