package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.GuestManager;
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

public class TPPCommandTeleportPetTest {
    private World world;
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private Chunk chunk;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<Location> teleportCaptor;
    private ProtectedRegionManager protectedRegionManager;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private Server server;

    @BeforeEach
    public void beforeEach(){
        this.world = mock(World.class);
        Location playerLocation = MockFactory.getMockLocation(this.world, 100, 200, 300);
        Location adminLocation = MockFactory.getMockLocation(this.world, 400, 500, 600);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", this.world, playerLocation, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.parrots", "tppets.cats", "tppets.dogs"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, adminLocation, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.parrots", "tppets.cats", "tppets.dogs", "tppets.teleportother", "tppets.tpanywhere"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.chunk = mock(Chunk.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.teleportCaptor = ArgumentCaptor.forClass(Location.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);
        this.protectedRegionManager = mock(ProtectedRegionManager.class);
        this.command = mock(Command.class);
        this.server = mock(Server.class);

        when(this.tpPets.getServer()).thenReturn(this.server);
        when(this.world.getName()).thenReturn("MockWorld");
        when(this.world.getChunkAt(6, 6)).thenReturn(this.chunk);
        when(this.protectedRegionManager.canTpThere(any(Player.class), any(Location.class))).thenReturn(true);
        when(this.tpPets.getProtectedRegionManager()).thenReturn(this.protectedRegionManager);

    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - tp - " + commandStatus.toString(), logCaptor.getValue());
    }

    void setAliases() {
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("tp");
        aliases.put("tp", altAlias);

        this.commandTPP = new CommandTPP(aliases, this.tpPets);
    }

    Entity[] getEntityList(String[] ids, Class<? extends Entity> className) {
        Entity[] ret = new Entity[ids.length];
        for (int i = 0; i < ids.length; i++) {
            ret[i] = MockFactory.getMockEntity(ids[i], className);
            when(this.server.getEntity(UUID.fromString(ids[i]))).thenReturn(ret[i]);
        }
        return ret;
    }

    @ParameterizedTest
    @MethodSource("teleportsPetsProvider")
    void teleportsValidPets(PetType.Pets petType, Class<? extends Entity> className) throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            String petName = petType.toString().toUpperCase() + "0";

            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, className);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", petName, petName);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", petName)).thenReturn(pet);

            this.setAliases();

            // Command object
            String[] args = {"tp", petName};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - tp - teleported MockPlayerName's " + petName);

            verify(this.chunk, times(1)).load();
            verify(entities[0], times(1)).eject();
            if (entities[0] instanceof Sittable) {
                verify((Sittable)entities[0], times(1)).setSitting(false);
            }
            verify(entities[0]).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getX(), 0.5);
            assertEquals(200, capturedPetLocation.getY(), 0.5);
            assertEquals(300, capturedPetLocation.getZ(), 0.5);
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + petName + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(entities[1], times(0)).teleport(any(Location.class));
        }
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

    @Test
    @DisplayName("Can't teleport in ProtectedRegions without tppets.tpanywhere")
    void cannotTeleportInProtectedRegionsWithoutPermission() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            // Permissions modifications
            when(this.protectedRegionManager.canTpThere(any(Player.class), any(Location.class))).thenReturn(false);

            this.setAliases();

            // Command object with no second argument
            String[] args = {"tp", "HORSE0"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.CANT_TELEPORT_IN_PR);

            verify(this.chunk, never()).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Can't teleport without tppets.horses")
    void cannotTeleportWithoutPetPermission() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            // Permissions modifications
            when(this.player.hasPermission("tppets.horses")).thenReturn(false);

            this.setAliases();

            // Command object with no second argument
            String[] args = {"tp", "HORSE0"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.INSUFFICIENT_PERMISSIONS);

            verify(this.chunk, never()).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("No pet name provided")
    void cannotTeleportWithoutName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            this.setAliases();

            // Command object with no second argument
            String[] args = {"tp"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.SYNTAX_ERROR);

            verify(this.chunk, never()).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp tp [pet name]", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Denies teleporting between worlds when config option set")
    void teleportingWithoutTPBetweenWorlds() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            this.setAliases();

            // Putting player in different world
            when(this.world.getName()).thenReturn("RandomName");

            // Command object with no second argument
            String[] args = {"tp", "HORSE0"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.TP_BETWEEN_WORLDS);

            verify(this.chunk, never()).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't teleport pet between worlds. Your pet is in " + ChatColor.WHITE + "MockWorld", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Allows teleporting between worlds when config option set")
    void teleportingWithTPBetweenWorlds() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            this.setAliases();

            // Putting player in different world
            when(this.tpPets.getAllowTpBetweenWorlds()).thenReturn(true);
            when(this.world.getName()).thenReturn("RandomName");

            // Command object with no second argument
            String[] args = {"tp", "HORSE0"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - tp - teleported MockPlayerName's HORSE0");

            verify(this.chunk, times(1)).load();
            verify(entities[0], times(1)).eject();
            if (entities[0] instanceof Sittable) {
                verify((Sittable)entities[0], times(1)).setSitting(false);
            }
            verify(entities[0]).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getX(), 0.5);
            assertEquals(200, capturedPetLocation.getY(), 0.5);
            assertEquals(300, capturedPetLocation.getZ(), 0.5);
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(entities[1], times(0)).teleport(any(Location.class));
        }
    }

    @Test
    @DisplayName("Allows teleporting between worlds when player has tppets.tpanywhere")
    void teleportingWithoutTPBetweenWorldsPlayerWithPermission() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockAdminId", "HORSE0")).thenReturn(pet);

            this.setAliases();

            // Putting player in different world
            when(this.world.getName()).thenReturn("RandomName");

            // Command object with no second argument
            String[] args = {"tp", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockAdminName - tp - teleported MockAdminName's HORSE0");

            verify(this.chunk, times(1)).load();
            verify(entities[0], times(1)).eject();
            if (entities[0] instanceof Sittable) {
                verify((Sittable)entities[0], times(1)).setSitting(false);
            }
            verify(entities[0]).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(400, capturedPetLocation.getX(), 0.5);
            assertEquals(500, capturedPetLocation.getY(), 0.5);
            assertEquals(600, capturedPetLocation.getZ(), 0.5);
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(entities[1], times(0)).teleport(any(Location.class));
        }
    }

    @Test
    @DisplayName("Defaults to teleporting first argument pet name when 2 arguments provided without f:[username] syntax")
    void defaultsToTeleportingPetWithFirstArgument() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            this.setAliases();

            // Command object
            String[] args = {"tp", "HORSE0", "spare argument"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - tp - teleported MockPlayerName's HORSE0");

            verify(this.chunk, times(1)).load();
            verify(entities[0], times(1)).eject();
            if (entities[0] instanceof Sittable) {
                verify((Sittable)entities[0], times(1)).setSitting(false);
            }
            verify(entities[0]).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getX(), 0.5);
            assertEquals(200, capturedPetLocation.getY(), 0.5);
            assertEquals(300, capturedPetLocation.getZ(), 0.5);
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(entities[1], times(0)).teleport(any(Location.class));
        }
    }

    @Test
    @DisplayName("Pet with name not in database")
    void invalidPetName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            this.setAliases();

            // Command object with no second argument
            String[] args = {"tp", "HORSE0;"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_PET);

            verify(this.chunk, never()).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE + "HORSE0;", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Database fails to find pet")
    void databaseFailsFindingPet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenThrow(new SQLException());

            this.setAliases();

            // Command object with no second argument
            String[] args = {"tp", "HORSE0"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

            verify(this.chunk, never()).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not find pet to teleport", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin teleporting another player's horse")
    void adminTeleportsHorse() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            // Command aliases
            this.setAliases();

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Command object
            String[] args = {"tp", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockAdminName - tp - teleported MockPlayerName's HORSE0");

            verify(this.chunk, times(1)).load();
            verify(entities[0], times(1)).eject();
            verify(entities[0]).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(400, capturedPetLocation.getX(), 0.5);
            assertEquals(500, capturedPetLocation.getY(), 0.5);
            assertEquals(600, capturedPetLocation.getZ(), 0.5);
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's " + ChatColor.BLUE + "pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(entities[1], times(0)).teleport(any(Location.class));

        }
    }

    @Test
    @DisplayName("Admin teleporting another player's horse when the player hasn't played before")
    void adminHorseTeleportPlayerNotPlayedBefore() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            // Command aliases
            this.setAliases();

            // Player who owns the pet
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Command object
            String[] args = {"tp", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.chunk, never()).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockOwnerName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin teleporting another player's horse with invalid player name")
    void adminHorseTeleportInvalidPlayerName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            // Command aliases
            this.setAliases();

            // Command object
            String[] args = {"tp", "f:MockOwnerName;", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.chunk, never()).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockOwnerName;", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Player without permissions trying to teleport another player's pet")
    void playerHorseTeleportInsufficientPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            // Command aliases
            this.setAliases();

            // Permissions adjustments
            when(this.sqlWrapper.getAllGuests()).thenReturn(new Hashtable<>());
            GuestManager guestManager = new GuestManager(this.sqlWrapper);
            when(this.tpPets.getGuestManager()).thenReturn(guestManager);
            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Command object
            String[] args = {"tp", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.INSUFFICIENT_PERMISSIONS);

            verify(this.chunk, never()).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Player with permissions trying to teleport another player's pet")
    void guestHorseTeleportWithPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);
            when(entities[0].getPassengers()).thenReturn(new ArrayList<>());

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            // Command aliases
            this.setAliases();

            // Permissions adjustments
            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Command object
            String[] args = {"tp", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.logWrapper, times(1)).logSuccessfulAction("MockAdminName - tp - teleported MockPlayerName's HORSE0");

            verify(this.chunk, times(1)).load();
            // Not ejected because players shouldn't be able to kick other players off
            verify(entities[0], never()).eject();
            verify(entities[0]).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(400, capturedPetLocation.getX(), 0.5);
            assertEquals(500, capturedPetLocation.getY(), 0.5);
            assertEquals(600, capturedPetLocation.getZ(), 0.5);
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's " + ChatColor.BLUE + "pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(entities[1], times(0)).teleport(any(Location.class));
        }
    }

    @Test
    @DisplayName("Player with permissions trying to teleport another player's pet while there is a passenger")
    void guestHorseTeleportWithPermissionsPassenger() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            // Entities in the world
            Entity[] entities = getEntityList(new String[]{"AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"}, org.bukkit.entity.Horse.class);
            when(entities[0].getPassengers()).thenReturn(Collections.singletonList(entities[1]));

            // PetStorage
            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");

            // Plugin database wrapper instance
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "HORSE0")).thenReturn(pet);

            // Command aliases
            this.setAliases();

            // Permissions adjustments
            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Command object
            String[] args = {"tp", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.CANT_TELEPORT);

            verify(this.chunk, times(1)).load();
            verify(entities[0], never()).eject();
            verify(entities[0], never()).teleport(any(Location.class));
            verify(entities[1], never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not teleport pet", capturedMessageOutput);
        }
    }
}
