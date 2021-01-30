import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TPPAllowList {
    private OfflinePlayer guest;
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private DBWrapper dbWrapper;
    private ArgumentCaptor<String> logCaptor;
    private List<PetStorage> petStorageList;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private Hashtable<String, List<String>> allowedPlayers;

    @BeforeEach
    public void beforeEach() {
        this.guest = MockFactory.getMockOfflinePlayer("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "MockGuestName");
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.allowguests"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.allowguests", "tppets.allowother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, null, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("list");
        aliases.put("list", altAlias);
        this.petStorageList = Collections.singletonList(new PetStorage("MockPetId", 7, 100, 200, 300, "MockWorld", "MockPlayerId", "MockPetName", "MockPetName"));
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        this.allowedPlayers = new Hashtable<>();
        this.allowedPlayers.put("MockPetId", new ArrayList<>());
        this.allowedPlayers.get("MockPetId").add("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    }

    @Test
    @DisplayName("Lists all players currently allowed to pet")
    void listsAllowedToPet() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"list", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.dbWrapper, times(1)).getPetByName(anyString(), anyString());

            verify(this.player, times(3)).sendMessage(this.messageCaptor.capture());
            List<String> capturedMessageOutput = this.messageCaptor.getAllValues();
            assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE +  "MockPlayerName's MockPetName" + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------", capturedMessageOutput.get(0));
            assertEquals(ChatColor.WHITE + "MockGuestName", capturedMessageOutput.get(1));
            assertEquals(ChatColor.GRAY + "-------------------------------------------", capturedMessageOutput.get(2));
        }
    }

    @Test
    @DisplayName("Admin lists all players currently allowed to pet")
    void adminListsAllowedToPet() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);
            bukkit.when(() -> Bukkit.getOfflinePlayer(any(UUID.class))).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"list", "f:MockPlayerName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, times(1)).getPetByName(anyString(), anyString());

            verify(this.admin, times(3)).sendMessage(this.messageCaptor.capture());
            List<String> capturedMessageOutput = this.messageCaptor.getAllValues();
            assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE +  "MockPlayerName's MockPetName" + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------", capturedMessageOutput.get(0));
            assertEquals(ChatColor.WHITE + "MockGuestName", capturedMessageOutput.get(1));
            assertEquals(ChatColor.GRAY + "-------------------------------------------", capturedMessageOutput.get(2));
        }
    }

    @Test
    @DisplayName("Silently fails when admin is not a player")
    void cannotAdminListsAllowedToPetNotPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender commandSender = mock(CommandSender.class);
            when(commandSender.hasPermission("tppets.allowguests")).thenReturn(true);

            String[] args = {"list", "f:MockPlayerName", "MockPetName"};
            this.commandTPP.onCommand(commandSender, this.command, "", args);

            verify(this.dbWrapper, never()).getPetByName(anyString(), anyString());
            verify(commandSender, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Fails to list allowed players when admin has insufficient permissions")
    void cannotAdminListsAllowedToPetInsufficientPermissions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.admin.hasPermission("tppets.allowother")).thenReturn(true);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"list", "f:MockPlayerName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, times(1)).getPetByName(anyString(), anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE +  "MockPlayerName's MockPetName" + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Fails to list allowed players when f:[username] is not found")
    void cannotAdminListsAllowedToPetNoPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.player.hasPlayedBefore()).thenReturn(false);

            String[] args = {"list", "f:MockPlayerName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).getPetByName(anyString(), anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin fails to remove player when no pet name specified")
    void cannotAdminListsAllowedToPetNoPetName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"list", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).getPetByName(anyString(), anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp list [pet name]", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Silently fails when sender is not a player")
    void cannotListsAllowedToPetNoPlayer() {
        CommandSender commandSender = mock(CommandSender.class);
        when(commandSender.hasPermission("tppets.allowguests")).thenReturn(true);

        String[] args = {"list", "MockPetName"};
        this.commandTPP.onCommand(commandSender, this.command, "", args);

        verify(this.dbWrapper, never()).getPetByName(anyString(), anyString());
        verify(commandSender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Fails to list players when no pet name specified")
    void cannotListAllowedToPetNoPetName() {
        String[] args = {"list"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getPetByName(anyString(), anyString());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp list [pet name]", capturedMessageOutput);
    }

    @Test
    @DisplayName("Fails to list players when invalid pet name specified")
    void cannotListAllowedToPetInvalidPetName() {
        String[] args = {"list", "MockPetName;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.dbWrapper, never()).getPetByName(anyString(), anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  "MockPetName;", capturedMessageOutput);
    }

    @Test
    @DisplayName("Fails to list players when db fails when finding pet")
    void cannotListAllowedToPetDbSearchFail() {
        when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(null);

        String[] args = {"list", "MockPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.dbWrapper, times(1)).getPetByName(anyString(), anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find allowed users", capturedMessageOutput);
    }

    @Test
    @DisplayName("Fails to list players when db fails to find pet")
    void cannotListAllowedToPetDbSearchNoResults() {
        when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(new ArrayList<>());

        String[] args = {"list", "MockPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.dbWrapper, times(1)).getPetByName(anyString(), anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  "MockPetName", capturedMessageOutput);
    }
}
