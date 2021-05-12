package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
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
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TPPCommandStorageListDefaultTest {
    private Player admin;
    private List<World> worldList;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private Command command;
    private CommandTPP commandTPP;
    private List<ServerStorageLocation> storageLocations;

    // TODO: MAKE IT SO THAT MOCKS ARE NOT DESTROYED ON EACH TEST. JUST RESET THEM
    @BeforeEach
    public void beforeEach() {
        // Players
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.storage", "tppets.setdefaultstorage"});
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
        World worldOne = mock(World.class);
        when(worldOne.getName()).thenReturn("MockWorldOne");
        ServerStorageLocation locationOne = MockFactory.getServerStorageLocation("default", 100, 200, 300, worldOne);
        World worldTwo = mock(World.class);
        when(worldTwo.getName()).thenReturn("MockWorldTwo");
        ServerStorageLocation locationTwo = MockFactory.getServerStorageLocation("default", 400, 500, 600, worldTwo);
        this.storageLocations = new ArrayList<>();
        this.storageLocations.add(locationOne);
        this.storageLocations.add(locationTwo);

        // Worlds
        this.worldList = new ArrayList<>();
        this.worldList.add(worldOne);
        this.worldList.add(worldTwo);
    }

    @Test
    @DisplayName("Lists server storage locations from the database")
    void listServerStorageLocations() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            when(this.sqlWrapper.getServerStorageLocations(this.worldList.get(0))).thenReturn(Collections.singletonList(this.storageLocations.get(0)));
            when(this.sqlWrapper.getServerStorageLocations(this.worldList.get(1))).thenReturn(Collections.singletonList(this.storageLocations.get(1)));

            String[] args = {"storage", "list", "default"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(2)).getServerStorageLocations(any(World.class));

            verify(this.admin, times(6)).sendMessage(this.messageCaptor.capture());
            List<String> messages = this.messageCaptor.getAllValues();
            assertEquals(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE +  "Server's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------", messages.get(0));
            assertEquals(ChatColor.BLUE + "name: " + ChatColor.WHITE + "default", messages.get(1));
            assertEquals(ChatColor.BLUE + "    location: " + ChatColor.WHITE + "100, 200, 300, MockWorldOne", messages.get(2));
            assertEquals(ChatColor.BLUE + "name: " + ChatColor.WHITE + "default", messages.get(3));
            assertEquals(ChatColor.BLUE + "    location: " + ChatColor.WHITE + "400, 500, 600, MockWorldTwo", messages.get(4));
            assertEquals(ChatColor.GRAY + "----------------------------------------", messages.get(5));
        }
    }

    @Test
    @DisplayName("Silently fails when sender is invalid type")
    void cannotListWhenSenderIsInvalidType() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.storage")).thenReturn(true);
        when(sender.hasPermission("tppets.setdefaultstorage")).thenReturn(true);

        String[] args = {"storage", "list", "default"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verify(this.sqlWrapper, never()).getServerStorageLocations(any(World.class));

        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Reports general database failure to the user")
    void cantListServerStorageLocationsDatabaseError() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            when(this.sqlWrapper.getServerStorageLocations(this.worldList.get(0))).thenThrow(new SQLException());

            String[] args = {"storage", "list", "default"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getServerStorageLocations(any(World.class));

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not find storage locations", message);        }
    }
}