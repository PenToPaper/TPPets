import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TPPCommandStorageListDefaultTest {
    private Player admin;
    private List<World> worldList;
    private ArgumentCaptor<String> messageCaptor;
    private DBWrapper dbWrapper;
    private Command command;
    private CommandTPP commandTPP;
    private List<StorageLocation> storageLocations;
    private TPPets tpPets;

    // TODO: MAKE IT SO THAT MOCKS ARE NOT DESTROYED ON EACH TEST. JUST RESET THEM
    @BeforeEach
    public void beforeEach() {
        // Players
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.storage", "tppets.setdefaultstorage"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);

        // Plugin
        this.dbWrapper = mock(DBWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, logWrapper, true, false, true);

        // Command
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("storage");
        aliases.put("storage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, this.tpPets);

        // Database
        World worldOne = mock(World.class);
        when(worldOne.getName()).thenReturn("MockWorldOne");
        StorageLocation locationOne = MockFactory.getStorageLocation("default", 100, 200, 300, worldOne);
        World worldTwo = mock(World.class);
        when(worldTwo.getName()).thenReturn("MockWorldTwo");
        StorageLocation locationTwo = MockFactory.getStorageLocation("default", 400, 500, 600, worldTwo);
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
    void listServerStorageLocations() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            when(this.dbWrapper.getServerStorageLocations(this.worldList.get(0))).thenReturn(Collections.singletonList(this.storageLocations.get(0)));
            when(this.dbWrapper.getServerStorageLocations(this.worldList.get(1))).thenReturn(Collections.singletonList(this.storageLocations.get(1)));

            String[] args = {"storage", "list", "default"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, times(2)).getServerStorageLocations(any(World.class));

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
    @DisplayName("Reports database not found failure to the user")
    void cantListServerStorageLocationsDatabaseNotFound() {
        when(this.tpPets.getDatabase()).thenReturn(null);

        String[] args = {"storage", "list", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getServerStorageLocations(any(World.class));

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find storage locations", message);
    }

    @Test
    @DisplayName("Reports general database failure to the user")
    void cantListServerStorageLocationsDatabaseError() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            when(this.dbWrapper.getServerStorageLocations(this.worldList.get(0))).thenReturn(null);

            String[] args = {"storage", "list", "default"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, times(1)).getServerStorageLocations(any(World.class));

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not find storage locations", message);        }
    }
}