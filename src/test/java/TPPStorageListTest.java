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
import java.util.Hashtable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TPPStorageListTest {
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private DBWrapper dbWrapper;
    private Command command;
    private CommandTPP commandTPP;
    private List<StorageLocation> storageLocations;

    @BeforeEach
    public void beforeEach(){
        // Players
        this.player = TeleportMocksFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.storage"});
        this.admin = TeleportMocksFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.storage", "tppets.storageother", "tppets.bypassstoragelimit"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);

        // Plugin
        this.dbWrapper = mock(DBWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = TeleportMocksFactory.getMockPlugin(this.dbWrapper, logWrapper, true, false, true);

        // Command
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("storage");
        aliases.put("storage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        // Database
        World world = mock(World.class);
        when(world.getName()).thenReturn("MockWorld");
        StorageLocation locationOne = TeleportMocksFactory.getStorageLocation("StorageOne", 100, 200, 300, world);
        StorageLocation locationTwo = TeleportMocksFactory.getStorageLocation("StorageTwo", 400, 500, 600, world);
        this.storageLocations = new ArrayList<>();
        this.storageLocations.add(locationOne);
        this.storageLocations.add(locationTwo);
    }

    @Test
    @DisplayName("Lists storage locations from the database")
    void listStorageLocations() {
        when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(this.storageLocations);

        String[] args = {"storage", "list"};
        this.commandTPP.onCommand(this.player, command, "", args);

        verify(this.dbWrapper, times(1)).getStorageLocations(anyString());

        verify(this.player, times(6)).sendMessage(this.messageCaptor.capture());
        List<String> messages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockPlayerName's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------", messages.get(0));
        assertEquals(ChatColor.BLUE + "name: " + ChatColor.WHITE + "StorageOne", messages.get(1));
        assertEquals(ChatColor.BLUE + "    location: " + ChatColor.WHITE + "100, 200, 300, MockWorld", messages.get(2));
        assertEquals(ChatColor.BLUE + "name: " + ChatColor.WHITE + "StorageTwo", messages.get(3));
        assertEquals(ChatColor.BLUE + "    location: " + ChatColor.WHITE + "400, 500, 600, MockWorld", messages.get(4));
        assertEquals(ChatColor.GRAY + "----------------------------------------", messages.get(5));
    }

    @Test
    @DisplayName("Admins can lists storage locations of other users from the database")
    void adminListStorageLocations() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(this.storageLocations);

            String[] args = {"storage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(this.admin, command, "", args);

            verify(this.dbWrapper, times(1)).getStorageLocations(anyString());

            verify(this.admin, times(6)).sendMessage(this.messageCaptor.capture());
            List<String> messages = this.messageCaptor.getAllValues();
            assertEquals(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockPlayerName's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------", messages.get(0));
            assertEquals(ChatColor.BLUE + "name: " + ChatColor.WHITE + "StorageOne", messages.get(1));
            assertEquals(ChatColor.BLUE + "    location: " + ChatColor.WHITE + "100, 200, 300, MockWorld", messages.get(2));
            assertEquals(ChatColor.BLUE + "name: " + ChatColor.WHITE + "StorageTwo", messages.get(3));
            assertEquals(ChatColor.BLUE + "    location: " + ChatColor.WHITE + "400, 500, 600, MockWorld", messages.get(4));
            assertEquals(ChatColor.GRAY + "----------------------------------------", messages.get(5));
        }
    }

    @Test
    @DisplayName("Lists empty storage location list from database")
    void listEmptyStorageLocations() {
        when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());

        String[] args = {"storage", "list"};
        this.commandTPP.onCommand(this.player, command, "", args);

        verify(this.dbWrapper, times(1)).getStorageLocations(anyString());

        verify(this.player, times(2)).sendMessage(this.messageCaptor.capture());
        List<String> messages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockPlayerName's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------", messages.get(0));
        assertEquals(ChatColor.GRAY + "----------------------------------------", messages.get(1));
    }
}
