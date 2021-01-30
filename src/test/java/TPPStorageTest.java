import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Using /tpp storage list to test response to syntax errors
public class TPPStorageTest {
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
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.storage"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.storage", "tppets.storageother", "tppets.bypassstoragelimit"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);

        // Plugin
        this.dbWrapper = mock(DBWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.dbWrapper, logWrapper, true, false, true);

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
        StorageLocation locationOne = MockFactory.getStorageLocation("StorageOne", 100, 200, 300, world);
        StorageLocation locationTwo = MockFactory.getStorageLocation("StorageTwo", 400, 500, 600, world);
        this.storageLocations = new ArrayList<>();
        this.storageLocations.add(locationOne);
        this.storageLocations.add(locationTwo);
    }

    @Test
    @DisplayName("Non-player sending command denies action silently")
    void nonPlayerSendingCommand() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.storage")).thenReturn(true);

        String[] args = {"storage", "list"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verify(this.dbWrapper, never()).getStorageLocations(anyString());
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Non-player sending command for another player denies action silently")
    void nonPlayerSendingCommandForAnother() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.storage")).thenReturn(true);

            String[] args = {"storage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            verify(this.dbWrapper, never()).getStorageLocations(anyString());
            verify(sender, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Admin insufficient permissions username with f:[username] syntax")
    void adminInsufficientForOthersPermissions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);
            when(this.admin.hasPermission("tppets.storageother")).thenReturn(false);

            String[] args = {"storage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).getStorageLocations(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();

            assertEquals(ChatColor.RED + "You don't have permission to do that", message);
        }
    }

    @Test
    @DisplayName("Admin incorrect username with f:[username] syntax")
    void adminIncorrectName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(this.storageLocations);

            String[] args = {"storage", "f:MockPlayerName", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).getStorageLocations(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();

            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", message);
        }
    }

    @Test
    @DisplayName("Admins invalid username with f:[username] syntax")
    void adminInvalidName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getStorageLocations("MockAdminId")).thenReturn(this.storageLocations);

            String[] args = {"storage", "f:MockPlayerName;", "list"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).getStorageLocations(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();

            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName;", message);
        }
    }

    @Test
    @DisplayName("Admins no action with command using f:[username] syntax")
    void adminNoAction() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getStorageLocations("MockAdminId")).thenReturn(this.storageLocations);

            String[] args = {"storage", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).getStorageLocations(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();

            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add/remove/list]", message);
        }
    }

    @Test
    @DisplayName("No action with command")
    void playerNoAction() {
        String[] args = {"storage"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();

        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add/remove/list]", message);
    }

    @Test
    @DisplayName("Invalid action with command")
    void playerInvalidAction() {
        String[] args = {"storage", "invalid"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();

        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add/remove/list]", message);
    }

    @Test
    @DisplayName("Player insufficient permissions for /tpp list default syntax")
    void adminInsufficientDefaultStoragePermissions() {
        when(this.admin.hasPermission("tppets.setdefaultstorage")).thenReturn(false);

        String[] args = {"storage", "list", "default"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getDefaultServerStorageLocation(any(World.class));

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();

        assertEquals(ChatColor.RED + "You don't have permission to do that", message);
    }
}
