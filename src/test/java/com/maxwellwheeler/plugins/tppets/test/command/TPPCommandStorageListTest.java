package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPCommandStorageListTest {
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private Command command;
    private CommandTPP commandTPP;
    private List<PlayerStorageLocation> playerStorageLocations;

    @BeforeEach
    public void beforeEach(){
        // Players
        World world = mock(World.class);
        when(world.getName()).thenReturn("MockWorld");

        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", world, null, new String[]{"tppets.storage"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", world, null, new String[]{"tppets.storage", "tppets.storageother", "tppets.bypassstoragelimit"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);

        // Plugin
        this.sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, logWrapper, false, true);

        // Command
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("storage");
        aliases.put("storage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        // Database
        PlayerStorageLocation locationOne = MockFactory.getPlayerStorageLocation("StorageOne", "MockPlayerId",100, 200, 300, world);
        PlayerStorageLocation locationTwo = MockFactory.getPlayerStorageLocation("StorageTwo", "MockPlayerId",400, 500, 600, world);
        this.playerStorageLocations = new ArrayList<>();
        this.playerStorageLocations.add(locationOne);
        this.playerStorageLocations.add(locationTwo);
    }

    @Test
    @DisplayName("Lists storage locations from the database")
    void listStorageLocations() throws SQLException {
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(this.playerStorageLocations);

        String[] args = {"storage", "list"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getStorageLocations(anyString());

        verify(this.player, times(6)).sendMessage(this.messageCaptor.capture());
        List<String> messages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockPlayerName's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "---------", messages.get(0));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "StorageOne", messages.get(1));
        assertEquals(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + "100, 200, 300, MockWorld", messages.get(2));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "StorageTwo", messages.get(3));
        assertEquals(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + "400, 500, 600, MockWorld", messages.get(4));
        assertEquals(ChatColor.GRAY + StringUtils.repeat("-", 42), messages.get(5));
    }

    @Test
    @DisplayName("Admins can lists storage locations of other users from the database")
    void adminListStorageLocations() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(this.playerStorageLocations);

            String[] args = {"storage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getStorageLocations(anyString());

            verify(this.admin, times(6)).sendMessage(this.messageCaptor.capture());
            List<String> messages = this.messageCaptor.getAllValues();
            assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockPlayerName's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "---------", messages.get(0));
            assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "StorageOne", messages.get(1));
            assertEquals(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + "100, 200, 300, MockWorld", messages.get(2));
            assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "StorageTwo", messages.get(3));
            assertEquals(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + "400, 500, 600, MockWorld", messages.get(4));
            assertEquals(ChatColor.GRAY + StringUtils.repeat("-", 42), messages.get(5));
        }
    }

    @Test
    @DisplayName("Lists empty storage location list from database")
    void listEmptyStorageLocations() throws SQLException {
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());

        String[] args = {"storage", "list"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getStorageLocations(anyString());

        verify(this.player, times(2)).sendMessage(this.messageCaptor.capture());
        List<String> messages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockPlayerName's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "---------", messages.get(0));
        assertEquals(ChatColor.GRAY + StringUtils.repeat("-", 42), messages.get(1));
    }

    @Test
    @DisplayName("Displays inability to find storage locations to user")
    void cantDisplayStorageLocationsDatabaseFailure() throws SQLException {
        when(this.sqlWrapper.getStorageLocations("MockPlayerId")).thenThrow(new SQLException());

        String[] args = {"storage", "list"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getStorageLocations(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find storage locations", message);
    }
}
