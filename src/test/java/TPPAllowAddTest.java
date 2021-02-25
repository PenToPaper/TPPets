import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TPPAllowAddTest {
    private OfflinePlayer guest;
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private List<PetStorage> petStorageList;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private Hashtable<String, List<String>> allowedPlayers;

    @BeforeEach
    public void beforeEach(){
        this.guest = MockFactory.getMockOfflinePlayer("MockGuestId", "MockGuestName");
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.allowguests"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.allowguests", "tppets.allowother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("allow");
        aliases.put("allow", altAlias);
        this.petStorageList = Collections.singletonList(new PetStorage("MockPetId", 7, 100, 200, 300, "MockWorld", "MockPlayerId", "MockPetName", "MockPetName"));
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        this.allowedPlayers = new Hashtable<>();
    }

    @Test
    @DisplayName("Adds a new player to a pet")
    void addsPlayerToPet() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.dbWrapper.insertAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());
            assertEquals("MockGuestId", this.allowedPlayers.get("MockPetId").get(0));

            verify(this.dbWrapper, times(1)).insertAllowedPlayer(anyString(), anyString());

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockPlayerName allowed MockGuestName to use MockPlayerName's pet named MockPetName", capturedLogOutput);

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.BLUE + " is now allowed to " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin adds new players to pet")
    void adminAddPlayerToPet() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.dbWrapper.insertAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(1, this.allowedPlayers.size());
            assertTrue(this.allowedPlayers.containsKey("MockPetId"));
            assertEquals(1, this.allowedPlayers.get("MockPetId").size());
            assertEquals("MockGuestId", this.allowedPlayers.get("MockPetId").get(0));

            verify(this.dbWrapper, times(1)).insertAllowedPlayer(anyString(), anyString());

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockAdminName allowed MockGuestName to use MockPlayerName's pet named MockPetName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.BLUE + " is now allowed to " + ChatColor.WHITE + "MockPlayerName's " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Silently fails when admin is not user")
    void cannotAdminAddPlayerToPetNotPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());
        }
    }


    @Test
    @DisplayName("Admin insufficient permissions username with f:[username] syntax")
    void cannotAdminAddPlayerToPetInsufficientPermissions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.admin.hasPermission("tppets.allowother")).thenReturn(false);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin cannot add player to pet without valid user")
    void cannotAdminAddPlayerToPetNoPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.hasPlayedBefore()).thenReturn(false);

            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.admin.hasPermission("tppets.allowother")).thenReturn(false);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin cannot add player to pet without target pet")
    void cannotAdminAddPlayerToPetNoTargetPet() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp allow [player name] [pet name]", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Silently fails when player is not user")
    void cannotAddPlayerToPetNotPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());
        }
    }

    @Test
    @DisplayName("Cannot add player to pet without target pet")
    void cannotAddPlayerToPetNoTargetPet() {
        String[] args = {"allow", "MockGuestName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        assertEquals(0, this.allowedPlayers.size());

        verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp allow [player name] [pet name]", capturedMessageOutput);
    }


    @Test
    @DisplayName("Cannot add player to pet without target player")
    void cannotAddPlayerToPetNoTargetPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.guest.hasPlayedBefore()).thenReturn(false);

            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockGuestName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Cannot add player to pet with invalid pet name")
    void cannotAddPlayerToPetInvalidPetName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            String[] args = {"allow", "MockGuestName", "MockPetName;"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  "MockPetName;", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Cannot add player to pet when database fails to find pet")
    void cannotAddPlayerToPetDbFailPet() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(null);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Cannot add player to pet when no pet in database with name")
    void cannotAddPlayerToPetNoPetWithName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(new ArrayList<>());

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Cannot add player to pet when player is already added to pet")
    void cannotAddPlayerToPetAlreadyDone() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            List<String> alreadyAllowedPlayers = new ArrayList<>();
            alreadyAllowedPlayers.add("MockGuestId");
            this.allowedPlayers.put("MockPetId", alreadyAllowedPlayers);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.RED + " is already allowed to " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Admin cannot add player to pet when player is already added to pet")
    void cannotAdminAddPlayerToPetAlreadyDone() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            List<String> alreadyAllowedPlayers = new ArrayList<>();
            alreadyAllowedPlayers.add("MockGuestId");
            this.allowedPlayers.put("MockPetId", alreadyAllowedPlayers);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"allow", "f:MockPlayerName", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.RED + " is already allowed to " + ChatColor.WHITE + "MockPlayerName's " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Cannot add player to pet when database fails to add player to pet")
    void cannotAddPlayerToPetDbFailAdd() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(this.guest);

            when(this.dbWrapper.getPetByName("MockPlayerId", "MockPetName")).thenReturn(this.petStorageList);
            when(this.dbWrapper.insertAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(false);
            when(this.tpPets.getAllowedPlayers()).thenReturn(this.allowedPlayers);

            String[] args = {"allow", "MockGuestName", "MockPetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            assertEquals(0, this.allowedPlayers.size());

            verify(this.dbWrapper, times(1)).insertAllowedPlayer(anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", capturedMessageOutput);
        }
    }
}
