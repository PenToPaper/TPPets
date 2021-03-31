package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPCommandStorageAddDefaultTest {
    private Location adminLocation;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private World world;

    @BeforeEach
    public void beforeEach() {
        this.world = mock(World.class);
        this.adminLocation = MockFactory.getMockLocation(this.world, 400, 500, 600);
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, this.adminLocation, new String[]{"tppets.storage", "tppets.setdefaultstorage"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("storage");
        aliases.put("storage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
    }

    @Test
    @DisplayName("Adds storage locations to the database")
    void addStorageLocation() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);
        when(this.sqlWrapper.addServerStorageLocation("default", this.adminLocation)).thenReturn(true);

        String[] args = {"storage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.sqlWrapper, times(1)).addServerStorageLocation(anyString(), any(Location.class));

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("Player MockAdminName has added server location default x: 400, y: 500, z: 600", capturedLogOutput);

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "You have added server storage location " + ChatColor.WHITE + "default", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when database cannot add entry")
    void cannotAddStorageLocationDBCannotAdd() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);
        when(this.sqlWrapper.addServerStorageLocation("default", this.adminLocation)).thenReturn(false);

        String[] args = {"storage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.sqlWrapper, times(1)).addServerStorageLocation(anyString(), any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add sever storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when database fails adding new entry")
    void cannotAddStorageLocationDBFailAdding() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);
        when(this.sqlWrapper.addServerStorageLocation("default", this.adminLocation)).thenThrow(new SQLException());

        String[] args = {"storage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.sqlWrapper, never()).addServerStorageLocation(anyString(), any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure when database fails finding existing entry")
    void cannotAddStorageLocationDBFailGetting() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenThrow(new SQLException());
        when(this.sqlWrapper.addServerStorageLocation("default", this.adminLocation)).thenReturn(true);

        String[] args = {"storage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.sqlWrapper, never()).addServerStorageLocation(anyString(), any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Does not add server storage location if it already exists")
    void cannotAddStorageLocationAlreadyExists() throws SQLException {
        StorageLocation storageLocation = mock(StorageLocation.class);
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(storageLocation);

        String[] args = {"storage", "add", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.sqlWrapper, never()).addServerStorageLocation(anyString(), any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Server storage " + ChatColor.WHITE + "default" + ChatColor.RED + " already exists", capturedMessageOutput);
    }
}