import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import org.bukkit.*;
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
import static org.mockito.Mockito.times;

public class TPPStorageRemoveTest {
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private StorageLocation storageLocation;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach(){
        this.player = TeleportMocksFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.storage"});
        this.admin = TeleportMocksFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.storage", "tppets.storageother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.storageLocation = mock(StorageLocation.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        TPPets tpPets = TeleportMocksFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("storage");
        aliases.put("storage", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
    }

    @Test
    @DisplayName("Removes storage locations from the database")
    void removeStorageLocation() {
        when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
        when(this.dbWrapper.removeStorageLocation("MockPlayerId", "StorageName")).thenReturn(true);

        String[] args = {"storage", "remove", "StorageName"};
        this.commandTPP.onCommand(this.player, command, "", args);

        verify(this.dbWrapper, times(1)).removeStorageLocation(anyString(), anyString());

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("Player MockPlayerName has removed location StorageName from MockPlayerName", capturedLogOutput);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "You have removed storage location " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admin removes storage locations for other people from the database")
    void adminRemoveStorageLocation() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
            when(this.dbWrapper.removeStorageLocation("MockPlayerId", "StorageName")).thenReturn(true);

            String[] args = {"storage", "f:MockPlayerName", "remove", "StorageName"};
            this.commandTPP.onCommand(this.admin, command, "", args);

            verify(this.dbWrapper, times(1)).removeStorageLocation(anyString(), anyString());

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("Player MockAdminName has removed location StorageName from MockPlayerName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName" + ChatColor.BLUE + " has removed storage location " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Cannot remove storage locations that do not exist")
    void cannotRemoveNonExistentStorage() {
        when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);

        String[] args = {"storage", "remove", "StorageName"};
        this.commandTPP.onCommand(this.player, command, "", args);

        verify(this.dbWrapper, never()).removeStorageLocation(anyString(), anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.WHITE + "StorageName" + ChatColor.BLUE + " does not exist", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admins cannot remove storage locations for other people that do not exist")
    void adminCannotRemoveNonExistentStorage() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);

            String[] args = {"storage", "f:MockPlayerName", "remove", "StorageName"};
            this.commandTPP.onCommand(this.admin, command, "", args);

            verify(this.dbWrapper, never()).removeStorageLocation(anyString(), anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName" + ChatColor.BLUE + " does not have a storage location named " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Reports database failure to user")
    void reportsDatabaseFailure() {
        when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
        when(this.dbWrapper.removeStorageLocation("MockPlayerId", "StorageName")).thenReturn(false);

        String[] args = {"storage", "remove", "StorageName"};
        this.commandTPP.onCommand(this.player, command, "", args);

        verify(this.dbWrapper, times(1)).removeStorageLocation(anyString(), anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Unable to remove location.", capturedMessageOutput);
    }
}
