package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TPPCommandTeleportListTest {
    private World world;
    private List<World> worldList;
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach() {
        this.world = mock(World.class);
        when(this.world.getName()).thenReturn("MockWorld");
        this.worldList = new ArrayList<>();
        this.worldList.add(this.world);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", this.world, null, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.parrots", "tppets.cats", "tppets.dogs"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, null, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.parrots", "tppets.cats", "tppets.dogs", "tppets.teleportother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, logWrapper, true, false, true);
        this.command = mock(Command.class);
    }

    void setAliases() {
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("list");
        aliases.put("list", altAlias);

        this.commandTPP = new CommandTPP(aliases, this.tpPets);
    }

    private static Stream<Arguments> petTypeProvider() {
        return Stream.of(
                Arguments.of(PetType.Pets.HORSE),
                Arguments.of(PetType.Pets.HORSE),
                Arguments.of(PetType.Pets.HORSE),
                Arguments.of(PetType.Pets.DONKEY),
                Arguments.of(PetType.Pets.LLAMA),
                Arguments.of(PetType.Pets.MULE),
                Arguments.of(PetType.Pets.PARROT),
                Arguments.of(PetType.Pets.DOG),
                Arguments.of(PetType.Pets.CAT)
        );
    }

    @ParameterizedTest
    @MethodSource("petTypeProvider")
    void listsValidPets(PetType.Pets petType) throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", PetType.getIndexFromPet(petType), 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", PetType.getIndexFromPet(petType), 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("MockPetId2", PetType.getIndexFromPet(petType), 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"list", petType.toString().toLowerCase()};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getAllPetsFromOwner(anyString());

            verify(this.player, times(5)).sendMessage(this.messageCaptor.capture());
            List<String> messages = this.messageCaptor.getAllValues();
            assertEquals(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockPlayerName's " + petType.toString().toLowerCase() + ChatColor.BLUE + " names ]" + ChatColor.DARK_GRAY + "---------", messages.get(0));
            assertEquals(ChatColor.WHITE + "  1) CorrectPet0", messages.get(1));
            assertEquals(ChatColor.WHITE + "  2) CorrectPet1", messages.get(2));
            assertEquals(ChatColor.WHITE + "  3) CorrectPet2", messages.get(3));
            assertEquals(ChatColor.DARK_GRAY + "----------------------------------", messages.get(4));
        }
    }

    @Test
    @DisplayName("Cannot list when pet type is not specified")
    void listsValidPets() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            this.setAliases();

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command sender
            CommandSender sender = mock(CommandSender.class);

            // Command object
            String[] args = {"list", "horse"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            verify(this.sqlWrapper, never()).getAllPetsFromOwner(anyString());
            verify(sender, never()).sendMessage(this.messageCaptor.capture());
        }
    }

    @Test
    @DisplayName("Cannot list when sender is not player")
    void cannotListNoPetType() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            this.setAliases();

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"list"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, never()).getAllPetsFromOwner(anyString());
            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp all [pet type]", message);
        }
    }

    @Test
    @DisplayName("Cannot list when database failure occurs")
    void cannotListDbFail() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenThrow(new SQLException());

            this.setAliases();

            // Command object
            String[] args = {"list", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getAllPetsFromOwner(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not allow user to pet", message);
        }
    }

    @Test
    @DisplayName("Cannot list when database finds no pets")
    void cannotListNoPetsFound() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(new ArrayList<>());

            this.setAliases();

            // Command object
            String[] args = {"list", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getAllPetsFromOwner(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not find any " + ChatColor.WHITE + "horses", message);
        }
    }

    @Test
    @DisplayName("Cannot list with invalid pet type")
    void cannotListInvalidPetType() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            this.setAliases();

            // Command object
            String[] args = {"list", "invalidpettype"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, never()).getAllPetsFromOwner(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp all [pet type]", message);
        }
    }

    @Test
    @DisplayName("Cannot list without permission to pet type")
    void cannotListInsufficientPermissionsToPetType() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            this.setAliases();

            // Permission Changes
            when(this.player.hasPermission("tppets.horses")).thenReturn(false);

            // Command object
            String[] args = {"list", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, never()).getAllPetsFromOwner(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", message);
        }
    }

    @Test
    @DisplayName("Admin lists valid pets")
    void adminListsValidPets() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"list", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getAllPetsFromOwner(anyString());

            verify(this.admin, times(5)).sendMessage(this.messageCaptor.capture());
            List<String> messages = this.messageCaptor.getAllValues();
            assertEquals(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockPlayerName's horse" + ChatColor.BLUE + " names ]" + ChatColor.DARK_GRAY + "---------", messages.get(0));
            assertEquals(ChatColor.WHITE + "  1) CorrectPet0", messages.get(1));
            assertEquals(ChatColor.WHITE + "  2) CorrectPet1", messages.get(2));
            assertEquals(ChatColor.WHITE + "  3) CorrectPet2", messages.get(3));
            assertEquals(ChatColor.DARK_GRAY + "----------------------------------", messages.get(4));
        }
    }

    @Test
    @DisplayName("Cannot admin list valid pets when player has not played before")
    void cannotAdminListsValidPets() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Player who owns the pet
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"list", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, never()).getAllPetsFromOwner(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", message);
        }
    }

    @Test
    @DisplayName("Cannot admin list valid pets when admin is not a player")
    void cannotAdminSenderNotPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            this.setAliases();

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Sender
            CommandSender sender = mock(CommandSender.class);

            // Command object
            String[] args = {"list", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            verify(this.sqlWrapper, never()).getAllPetsFromOwner(anyString());
            verify(sender, never()).sendMessage(this.messageCaptor.capture());
        }
    }

    @Test
    @DisplayName("Cannot admin list valid pets when admin has insufficient permissions")
    void cannotAdminListInsufficientPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            this.setAliases();

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Permission modifications
            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);

            // Command object
            String[] args = {"list", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, never()).getAllPetsFromOwner(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", message);
        }
    }

    @Test
    @DisplayName("Cannot admin list valid pets when no pet type specified")
    void cannotAdminListNoPetType() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            this.setAliases();

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"list", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, never()).getAllPetsFromOwner(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp all [pet type]", message);
        }
    }
}