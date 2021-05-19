package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegionManager;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TPPCommandTeleportAllTest {
    private World world;
    private Location playerLocation;
    private Player player;
    private Location adminLocation;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private Chunk chunk;
    private SQLWrapper sqlWrapper;
    private ArgumentCaptor<Location> teleportCaptor;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private Server server;
    private ProtectedRegionManager protectedRegionManager;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() {
        this.world = mock(World.class);
        this.server = mock(Server.class);
        when(this.world.getName()).thenReturn("MockWorld");
        this.playerLocation = MockFactory.getMockLocation(this.world, 100, 200, 300);
        this.adminLocation = MockFactory.getMockLocation(this.world, 400, 500, 600);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", this.world, this.playerLocation, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.parrots", "tppets.cats", "tppets.dogs"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, this.adminLocation, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.parrots", "tppets.cats", "tppets.dogs", "tppets.teleportother", "tppets.tpanywhere"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.chunk = mock(Chunk.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.teleportCaptor = ArgumentCaptor.forClass(Location.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);
        this.protectedRegionManager = mock(ProtectedRegionManager.class);
        this.command = mock(Command.class);
        when(this.protectedRegionManager.canTpThere(any(Player.class), any(Location.class))).thenReturn(true);
        when(this.tpPets.getProtectedRegionManager()).thenReturn(this.protectedRegionManager);
        when(this.tpPets.getServer()).thenReturn(this.server);
        when(this.world.getChunkAt(anyInt(), anyInt())).thenReturn(this.chunk);
    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - all - " + commandStatus.toString(), logCaptor.getValue());
    }

    void setAliases() {
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("all");
        aliases.put("all", altAlias);

        this.commandTPP = new CommandTPP(aliases, this.tpPets);
    }

    void checkPetIsTeleported(Entity entity, Location expectedLocation, ArgumentCaptor<Location> captor) {
        verify(entity, times(1)).eject();
        verify(entity).teleport(captor.capture());
        Location teleportedTo = captor.getValue();
        assertEquals(this.world, teleportedTo.getWorld());
        assertEquals(expectedLocation.getBlockX(), teleportedTo.getX(), 0.5);
        assertEquals(expectedLocation.getBlockY(), teleportedTo.getY(), 0.5);
        assertEquals(expectedLocation.getBlockZ(), teleportedTo.getZ(), 0.5);
        if (entity instanceof Sittable) {
            verify((Sittable)entity, times(1)).setSitting(false);
        }
    }

    void checkPetIsNotTeleported(Entity entity) {
        verify(entity, times(0)).teleport(any(Location.class));
    }

    void checkPlayerResponse(Player player, String expected, ArgumentCaptor<String> captor) {
        verify(player).sendMessage(captor.capture());
        String capturedMessageOutput = captor.getValue();
        assertEquals(expected, capturedMessageOutput);
    }

    Entity[] getEntityList(String[] ids, Class<? extends Entity> className) {
        Entity[] ret = new Entity[ids.length];
        for (int i = 0; i < ids.length; i++) {
            ret[i] = MockFactory.getMockEntity(ids[i], className);
            when(this.server.getEntity(UUID.fromString(ids[i]))).thenReturn(ret[i]);
        }
        return ret;
    }

    private static Stream<Arguments> teleportsPetsProvider() {
        return Stream.of(
                Arguments.of(PetType.Pets.HORSE, org.bukkit.entity.Horse.class),
                Arguments.of(PetType.Pets.HORSE, org.bukkit.entity.SkeletonHorse.class),
                Arguments.of(PetType.Pets.HORSE, org.bukkit.entity.ZombieHorse.class),
                Arguments.of(PetType.Pets.DONKEY, org.bukkit.entity.Donkey.class),
                Arguments.of(PetType.Pets.LLAMA, org.bukkit.entity.Llama.class),
                Arguments.of(PetType.Pets.MULE, org.bukkit.entity.Mule.class),
                Arguments.of(PetType.Pets.PARROT, org.bukkit.entity.Parrot.class),
                Arguments.of(PetType.Pets.DOG, org.bukkit.entity.Wolf.class),
                Arguments.of(PetType.Pets.CAT, org.bukkit.entity.Cat.class)
        );
    }

    @ParameterizedTest
    @MethodSource("teleportsPetsProvider")
    void teleportsValidPets(PetType.Pets petType, Class<? extends Entity> className) throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, className);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all", petType.toString().toLowerCase()};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - all - teleported MockPlayerName's " + petType.toString().toLowerCase() + "s");

            verify(this.chunk, times(3)).load();
            checkPetIsTeleported(entities[0], this.playerLocation, this.teleportCaptor);
            checkPetIsTeleported(entities[1], this.playerLocation, this.teleportCaptor);
            checkPetIsTeleported(entities[2], this.playerLocation, this.teleportCaptor);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.player, ChatColor.BLUE + "Your " + ChatColor.WHITE + petType.toString().toLowerCase() + "s " + ChatColor.BLUE + "have been teleported to you", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Can't teleport all in ProtectedRegions without tppets.tpanywhere")
    void cannotTeleportInProtectedRegionsWithoutPermission() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Permissions modifications
            when(this.protectedRegionManager.canTpThere(any(Player.class), any(Location.class))).thenReturn(false);

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.CANT_TELEPORT_IN_PR);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            verify(this.player, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Can't teleport all without tppets.horses")
    void cannotTeleportWithoutPetPermission() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Permissions modifications
            when(this.player.hasPermission("tppets.horses")).thenReturn(false);

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.INSUFFICIENT_PERMISSIONS);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.player, ChatColor.RED + "You don't have permission to do that", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("No pet type provided")
    void cannotTeleportWithoutType() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.SYNTAX_ERROR);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.player, ChatColor.RED + "Syntax Error! Usage: /tpp [pet type] all", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Invalid pet type provided")
    void invalidPetType() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all", "notapet"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_PET_TYPE);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.player, ChatColor.RED + "Syntax Error! Usage: /tpp [pet type] all", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Notifies users when no pets can be found in the database")
    void cannotTeleportNoPetsInDb() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(new ArrayList<>());

            this.setAliases();

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_PET);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.player, ChatColor.RED + "Could not find any " + ChatColor.WHITE + "horses", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Can't teleport pets when database error occurs")
    void cannotTeleportDbError() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenThrow(new SQLException());

            this.setAliases();

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.player, ChatColor.RED + "Could not get any pets", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Reports teleporting error to user if some pets could not be found")
    void cannotFindSomePets() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);
            when(this.server.getEntity(UUID.fromString("CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC"))).thenReturn(entities[0]);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.CANT_TELEPORT);

            verify(this.chunk, times(3)).load();
            checkPetIsTeleported(entities[0], this.playerLocation, this.teleportCaptor);
            checkPetIsNotTeleported(entities[1]);
            checkPlayerResponse(this.player, ChatColor.RED + "Teleported all pets except: " + ChatColor.WHITE + "CorrectPet0, CorrectPet1", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Reports teleporting error to user if some pets were in a different world")
    void teleportingWithoutTPBetweenWorlds() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Putting player in different world
            when(this.world.getName()).thenReturn("RandomName");

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.CANT_TELEPORT);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPlayerResponse(this.player, ChatColor.RED + "Teleported all pets except: " + ChatColor.WHITE + "CorrectPet0, CorrectPet1, CorrectPet2", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Allows teleporting between worlds with config option")
    void teleportingWithTPBetweenWorlds() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Putting player in different world
            when(this.tpPets.getAllowTpBetweenWorlds()).thenReturn(true);
            when(this.world.getName()).thenReturn("RandomName");

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - all - teleported MockPlayerName's horses");

            verify(this.chunk, times(3)).load();
            checkPetIsTeleported(entities[0], this.playerLocation, this.teleportCaptor);
            checkPetIsTeleported(entities[1], this.playerLocation, this.teleportCaptor);
            checkPetIsTeleported(entities[2], this.playerLocation, this.teleportCaptor);
            checkPlayerResponse(this.player, ChatColor.BLUE + "Your " + ChatColor.WHITE + "horses " + ChatColor.BLUE + "have been teleported to you", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Allows teleporting between worlds if player has tppets.tpanywhere")
    void teleportingWithoutTPBetweenWorldsPlayerWithPermission() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockAdminId")).thenReturn(petList);

            this.setAliases();

            // Putting player in different world
            when(this.world.getName()).thenReturn("RandomName");

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockAdminName - all - teleported MockAdminName's horses");

            verify(this.chunk, times(3)).load();
            checkPetIsTeleported(entities[0], this.adminLocation, this.teleportCaptor);
            checkPetIsTeleported(entities[1], this.adminLocation, this.teleportCaptor);
            checkPetIsTeleported(entities[2], this.adminLocation, this.teleportCaptor);
            checkPlayerResponse(this.admin, ChatColor.BLUE + "Your " + ChatColor.WHITE + "horses " + ChatColor.BLUE + "have been teleported to you", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Admin teleporting all of another player's horses")
    void adminTeleportsHorses() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"all", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockAdminName - all - teleported MockPlayerName's horses");

            verify(this.chunk, times(3)).load();
            checkPetIsTeleported(entities[0], this.adminLocation, this.teleportCaptor);
            checkPetIsTeleported(entities[1], this.adminLocation, this.teleportCaptor);
            checkPetIsTeleported(entities[2], this.adminLocation, this.teleportCaptor);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.admin, ChatColor.WHITE + "MockPlayerName's " + ChatColor.WHITE + "horses " + ChatColor.BLUE + "have been teleported to you", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Admin can't teleport all of another player's pets if they haven't played before")
    void cantAdminTeleportHorsesPlayerNotPlayedBefore() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Player who owns the pet
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"all", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.admin, ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Admin can't teleport all of another player's pets if their name is invalid")
    void cantAdminTeleportHorsesInvalidPlayerName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all", "f:MockPlayerName;", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.admin, ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName;", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Admin without permission can't teleport all of another player's pets")
    void cantAdminTeleportHorsesInsufficientPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBBB", "CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCCC", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet0 = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet1");
            PetStorage pet2 = new PetStorage("CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet2");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getAllPetsFromOwner("MockPlayerId")).thenReturn(petList);

            this.setAliases();

            // Player who owns the pet
            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"all", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.INSUFFICIENT_PERMISSIONS);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.admin, ChatColor.RED + "You don't have permission to do that", this.messageCaptor);
        }
    }
}