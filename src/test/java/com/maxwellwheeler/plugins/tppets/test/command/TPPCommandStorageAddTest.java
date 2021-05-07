package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
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

public class TPPCommandStorageAddTest {
    private Location playerLocation;
    private Player player;
    private Location adminLocation;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach(){
        World world = mock(World.class);
        this.playerLocation = MockFactory.getMockLocation(world, 100, 200, 300);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", world, this.playerLocation, new String[]{"tppets.storage"});
        this.adminLocation = MockFactory.getMockLocation(world, 400, 500, 600);
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", world, this.adminLocation, new String[]{"tppets.storage", "tppets.storageother", "tppets.bypassstoragelimit"});
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
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
        when(this.sqlWrapper.insertStorageLocation("MockPlayerId", "StorageName", this.playerLocation)).thenReturn(true);

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).insertStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("Player MockPlayerId has added location StorageName x: 100, y: 200, z: 300 for MockPlayerName", capturedLogOutput);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "You have added storage location " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admin adds storage locations for other people to the database")
    void adminAddStorageLocation() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
            when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
            when(this.sqlWrapper.insertStorageLocation("MockPlayerId", "StorageName", this.adminLocation)).thenReturn(true);

            when(this.tpPets.getStorageLimit()).thenReturn(1);

            String[] args = {"storage", "f:MockPlayerName", "add", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).insertStorageLocation(anyString(), anyString(), any(Location.class));

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("Player MockAdminId has added location StorageName x: 400, y: 500, z: 600 for MockPlayerName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName" + ChatColor.BLUE + " has added storage location " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Can't add storage locations without valid storage name")
    void cannotAddStorageLocationInvalidName() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, never()).insertStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Invalid storage location name: " + ChatColor.WHITE + "StorageName;", capturedMessageOutput);
    }

    @Test
    @DisplayName("Can't add storage locations without any storage name")
    void cannotAddStorageLocationNoName() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, never()).insertStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp storage add [storage name]", capturedMessageOutput);
    }

    @Test
    @DisplayName("Can't add storage locations when you can't teleport pets there")
    void cannotAddStorageLocationCantTpThere() throws SQLException {
        when(this.tpPets.canTpThere(this.player)).thenReturn(false);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, never()).insertStorageLocation(anyString(), anyString(), any(Location.class));
        verify(this.player, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Can't add storage locations when over storage limit")
    void cannotAddStorageLocationOverLimit() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());

        when(this.tpPets.getStorageLimit()).thenReturn(0);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, never()).insertStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "You can't set any more than " + ChatColor.WHITE + "0" + ChatColor.RED + " storage locations", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admins add storage locations over limit for others")
    void adminAddStorageLocationOverLimit() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
            when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
            when(this.sqlWrapper.insertStorageLocation("MockPlayerId", "StorageName", this.adminLocation)).thenReturn(true);

            when(this.tpPets.getStorageLimit()).thenReturn(0);

            String[] args = {"storage", "f:MockPlayerName", "add", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).insertStorageLocation(anyString(), anyString(), any(Location.class));

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("Player MockAdminId has added location StorageName x: 400, y: 500, z: 600 for MockPlayerName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName" + ChatColor.BLUE + " has added storage location " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Does not add a storage location if one with the same name already exists")
    void addStorageLocationAlreadyExists() throws SQLException {
        PlayerStorageLocation existingLocation = mock(PlayerStorageLocation.class);

        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(existingLocation);
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
        when(this.sqlWrapper.insertStorageLocation("MockPlayerId", "StorageName", this.playerLocation)).thenReturn(true);

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, never()).insertStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Storage location " + ChatColor.WHITE + "StorageName" + ChatColor.RED + " already exists", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admin does not add a storage location if one with the same name already exists")
    void adminAddStorageLocationAlreadyExists() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            PlayerStorageLocation existingLocation = mock(PlayerStorageLocation.class);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(existingLocation);
            when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
            when(this.sqlWrapper.insertStorageLocation("MockPlayerId", "StorageName", this.adminLocation)).thenReturn(true);

            when(this.tpPets.getStorageLimit()).thenReturn(1);

            String[] args = {"storage", "f:MockPlayerName", "add", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, never()).insertStorageLocation(anyString(), anyString(), any(Location.class));

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's" + ChatColor.RED + " storage location " + ChatColor.WHITE + "StorageName" + ChatColor.RED + " already exists", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Reports database cannot add storage to user")
    void reportsDatabaseCannotAdd() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
        when(this.sqlWrapper.insertStorageLocation("MockPlayerId", "StorageName", this.playerLocation)).thenReturn(false);

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).insertStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure to get specific storage from user")
    void reportsDatabaseFailureToGetSpecificStorage() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenThrow(new SQLException());
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
        when(this.sqlWrapper.insertStorageLocation("MockPlayerId", "StorageName", this.playerLocation)).thenReturn(true);

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, never()).insertStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure to get all storage from user")
    void reportsDatabaseFailureToGetAllStorage() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenThrow(new SQLException());
        when(this.sqlWrapper.insertStorageLocation("MockPlayerId", "StorageName", this.playerLocation)).thenReturn(true);

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, never()).insertStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add storage location", capturedMessageOutput);
    }

    @Test
    @DisplayName("Reports database failure to add storage from user")
    void reportsDatabaseFailureToAddStorage() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
        when(this.sqlWrapper.insertStorageLocation("MockPlayerId", "StorageName", this.playerLocation)).thenThrow(new SQLException());

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).insertStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add storage location", capturedMessageOutput);
    }
}
