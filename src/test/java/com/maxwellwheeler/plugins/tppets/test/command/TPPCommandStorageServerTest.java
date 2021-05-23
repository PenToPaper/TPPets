package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TPPCommandStorageServerTest {
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private Command command;
    private CommandTPP commandTPP;
    private List<ServerStorageLocation> serverStorageLocations;
    private World world;

    @BeforeEach
    public void beforeEach(){
        // Players
        this.world = mock(World.class);
        when(this.world.getName()).thenReturn("MockWorld");

        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", this.world, null, new String[]{"tppets.storage"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, null, new String[]{"tppets.storage", "tppets.storageother", "tppets.bypassstoragelimit"});
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
        ServerStorageLocation serverLocationOne = MockFactory.getServerStorageLocation("StorageOne",100, 200, 300, this.world);
        ServerStorageLocation serverLocationTwo = MockFactory.getServerStorageLocation("StorageTwo",400, 500, 600, this.world);
        this.serverStorageLocations = new ArrayList<>();
        this.serverStorageLocations.add(serverLocationOne);
        this.serverStorageLocations.add(serverLocationTwo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"server", "serverlist", "slist"})
    void listAccessibleServerStorageLocations(String alias) throws SQLException {
        when(this.sqlWrapper.getServerStorageLocations(this.world)).thenReturn(this.serverStorageLocations);

        String[] args = {"storage", alias};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getServerStorageLocations(any(World.class));

        verify(this.player, times(6)).sendMessage(this.messageCaptor.capture());
        List<String> messages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "Server's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "---------", messages.get(0));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "StorageOne", messages.get(1));
        assertEquals(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + "100, 200, 300, MockWorld", messages.get(2));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "StorageTwo", messages.get(3));
        assertEquals(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + "400, 500, 600, MockWorld", messages.get(4));
        assertEquals(ChatColor.GRAY + StringUtils.repeat("-", 34), messages.get(5));
    }

    @Test
    @DisplayName("Admins using f:[username] syntax with server storage search only get server storage locations in their world")
    void adminListServerStorageLocations() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.getWorld()).thenReturn(null);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getServerStorageLocations(this.world)).thenReturn(this.serverStorageLocations);

            String[] args = {"storage", "f:MockPlayerName", "server"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getServerStorageLocations(any(World.class));

            verify(this.admin, times(6)).sendMessage(this.messageCaptor.capture());
            List<String> messages = this.messageCaptor.getAllValues();
            assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "Server's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "---------", messages.get(0));
            assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "StorageOne", messages.get(1));
            assertEquals(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + "100, 200, 300, MockWorld", messages.get(2));
            assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "StorageTwo", messages.get(3));
            assertEquals(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + "400, 500, 600, MockWorld", messages.get(4));
            assertEquals(ChatColor.GRAY + StringUtils.repeat("-", 34), messages.get(5));
        }
    }

    @Test
    @DisplayName("Lists empty server storage location list from database")
    void listEmptyServerStorageLocations() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocations(this.world)).thenReturn(new ArrayList<>());

        String[] args = {"storage", "server"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getServerStorageLocations(any(World.class));

        verify(this.player, times(2)).sendMessage(this.messageCaptor.capture());
        List<String> messages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "Server's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "---------", messages.get(0));
        assertEquals(ChatColor.GRAY + StringUtils.repeat("-", 34), messages.get(1));
    }

    @Test
    @DisplayName("Displays inability to find server storage locations to user")
    void cantDisplayServerStorageLocationsDatabaseFailure() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocations(this.world)).thenThrow(new SQLException());

        String[] args = {"storage", "server"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getServerStorageLocations(any(World.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find server storage locations", message);
    }
}
