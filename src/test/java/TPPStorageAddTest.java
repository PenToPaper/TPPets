import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TPPStorageAddTest {
    private Location playerLocation;
    private Player player;
    private Location adminLocation;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach(){
        World world = mock(World.class);
        this.playerLocation = TeleportMocksFactory.getMockLocation(world, 100, 200, 300);
        this.player = TeleportMocksFactory.getMockPlayer("MockPlayerId", "MockPlayerName", world, this.playerLocation, new String[]{"tppets.storage"});
        this.adminLocation = TeleportMocksFactory.getMockLocation(world, 400, 500, 600);
        this.admin = TeleportMocksFactory.getMockPlayer("MockAdminId", "MockAdminName", world, this.adminLocation, new String[]{"tppets.storage", "tppets.storageother", "tppets.bypassstoragelimit"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.tpPets = TeleportMocksFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("storage");
        aliases.put("storage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
    }

    @Test
    @DisplayName("Adds storage locations to the database")
    void addStorageLocation() {
        when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
        when(this.dbWrapper.addStorageLocation("MockPlayerId", "StorageName", this.playerLocation)).thenReturn(true);

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, command, "", args);

        verify(dbWrapper, times(1)).addStorageLocation(anyString(), anyString(), any(Location.class));

        verify(logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("Player MockPlayerId has added location StorageName x: 100, y: 200, z: 300 for MockPlayerName", capturedLogOutput);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "You have added storage location " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admin adds storage locations for other people to the database")
    void adminAddStorageLocation() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
            when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
            when(this.dbWrapper.addStorageLocation("MockPlayerId", "StorageName", this.adminLocation)).thenReturn(true);

            when(this.tpPets.getStorageLimit()).thenReturn(1);

            String[] args = {"storage", "f:MockPlayerName", "add", "StorageName"};
            this.commandTPP.onCommand(this.admin, command, "", args);

            verify(dbWrapper, times(1)).addStorageLocation(anyString(), anyString(), any(Location.class));

            verify(logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("Player MockAdminId has added location StorageName x: 400, y: 500, z: 600 for MockPlayerName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName" + ChatColor.BLUE + " has added storage location " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Can't add storage locations when over storage limit")
    void cannotAddStorageLocationOverLimit() {
        when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());

        when(this.tpPets.getStorageLimit()).thenReturn(0);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, command, "", args);

        verify(dbWrapper, never()).addStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "You can't set any more than " + ChatColor.WHITE + "0" + ChatColor.RED + " storage locations.", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admins add storage locations over limit for others")
    void adminAddStorageLocationOverLimit() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
            when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
            when(this.dbWrapper.addStorageLocation("MockPlayerId", "StorageName", this.adminLocation)).thenReturn(true);

            when(this.tpPets.getStorageLimit()).thenReturn(0);

            String[] args = {"storage", "f:MockPlayerName", "add", "StorageName"};
            this.commandTPP.onCommand(this.admin, command, "", args);

            verify(dbWrapper, times(1)).addStorageLocation(anyString(), anyString(), any(Location.class));

            verify(logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("Player MockAdminId has added location StorageName x: 400, y: 500, z: 600 for MockPlayerName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName" + ChatColor.BLUE + " has added storage location " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Does not add a storage location if one with the same name already exists")
    void addStorageLocationAlreadyExists() {
        StorageLocation existingLocation = mock(StorageLocation.class);

        when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(existingLocation);
        when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
        when(this.dbWrapper.addStorageLocation("MockPlayerId", "StorageName", this.playerLocation)).thenReturn(true);

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, command, "", args);

        verify(dbWrapper, never()).addStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "You have already set a location named " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admin does not add a storage location if one with the same name already exists")
    void adminAddStorageLocationAlreadyExists() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            StorageLocation existingLocation = mock(StorageLocation.class);

            when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(existingLocation);
            when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
            when(this.dbWrapper.addStorageLocation("MockPlayerId", "StorageName", this.adminLocation)).thenReturn(true);

            when(this.tpPets.getStorageLimit()).thenReturn(1);

            String[] args = {"storage", "f:MockPlayerName", "add", "StorageName"};
            this.commandTPP.onCommand(this.admin, command, "", args);

            verify(dbWrapper, never()).addStorageLocation(anyString(), anyString(), any(Location.class));

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName" + ChatColor.RED + " already has a location named " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Reports database failure to user")
    void reportsDatabaseFailure() {
        when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.dbWrapper.getStorageLocations("MockPlayerId")).thenReturn(new ArrayList<>());
        when(this.dbWrapper.addStorageLocation("MockPlayerId", "StorageName", this.playerLocation)).thenReturn(false);

        when(this.tpPets.getStorageLimit()).thenReturn(1);

        String[] args = {"storage", "add", "StorageName"};
        this.commandTPP.onCommand(this.player, command, "", args);

        verify(dbWrapper, times(1)).addStorageLocation(anyString(), anyString(), any(Location.class));

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Unable to set location.", capturedMessageOutput);
    }
}
