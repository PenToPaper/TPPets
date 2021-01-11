import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TPPTeleportTest {
    World world;
    List<World> worldList;
    Location playerLocation;
    Player player;
    Location adminLocation;
    Player admin;
    ArgumentCaptor<String> messageCaptor;
    Chunk chunk;
    DBWrapper dbWrapper;
    LogWrapper logWrapper;
    ArgumentCaptor<String> logCaptor;
    ArgumentCaptor<Location> teleportCaptor;
    TPPets tpPets;
    Command command;
    CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach(){
        this.world = mock(World.class);
        this.worldList = new ArrayList<>();
        this.worldList.add(this.world);
        this.playerLocation = MockFactory.getMockLocation(this.world, 100, 200, 300);
        this.adminLocation = MockFactory.getMockLocation(this.world, 400, 500, 600);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", this.world, this.playerLocation, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.birds", "tppets.cats", "tppets.dogs"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, this.adminLocation, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.birds", "tppets.cats", "tppets.dogs", "tppets.teleportother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.chunk = mock(Chunk.class);
        when(this.world.getChunkAt(100, 100)).thenReturn(this.chunk);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.teleportCaptor = ArgumentCaptor.forClass(Location.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        this.command = mock(Command.class);
    }

    void setAliases(String alias) {
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add(alias);
        aliases.put(alias, altAlias);

        this.commandTPP = new CommandTPP(aliases, this.tpPets);
    }

    @ParameterizedTest
    @MethodSource("teleportsPetsProvider")
    void teleportsValidPets(String commandString, PetType.Pets petType, Class<? extends Entity> className) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            String petName = commandString.toUpperCase() + "0";

            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Entity instances
            Entity correctPet = MockFactory.getMockEntity("MockPetId", className);
            Entity incorrectPet = MockFactory.getMockEntity("MockIncorrectPetId", className);

            // A list of both entities
            List<Entity> entityList = Arrays.asList(correctPet, incorrectPet);
            when(this.world.getEntitiesByClasses(PetType.getClassTranslate(petType))).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", petName, petName);
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", petName)).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", petName, petType)).thenReturn(petList);

            this.setAliases(commandString);

            // Command object
            String[] args = {commandString, petName};
            this.commandTPP.onCommand(this.player, this.command, "", args);


            verify(this.chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            if (correctPet instanceof Sittable) {
                verify((Sittable)correctPet, times(1)).setSitting(false);
            }
            verify(correctPet).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getX(), 0.5);
            assertEquals(200, capturedPetLocation.getY(), 0.5);
            assertEquals(300, capturedPetLocation.getZ(), 0.5);
            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named " + petName + " to their location at: x: 100, y: 200, z: 300", capturedLogOutput);
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + petName + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));
        }
    }

    private static Stream<Arguments> teleportsPetsProvider() {
        return Stream.of(
                Arguments.of("horses", PetType.Pets.HORSE, org.bukkit.entity.Horse.class),
                Arguments.of("horses", PetType.Pets.HORSE, org.bukkit.entity.SkeletonHorse.class),
                Arguments.of("horses", PetType.Pets.HORSE, org.bukkit.entity.ZombieHorse.class),
                Arguments.of("donkeys", PetType.Pets.DONKEY, org.bukkit.entity.Donkey.class),
                Arguments.of("llamas", PetType.Pets.LLAMA, org.bukkit.entity.Llama.class),
                Arguments.of("mules", PetType.Pets.MULE, org.bukkit.entity.Mule.class),
                Arguments.of("birds", PetType.Pets.PARROT, org.bukkit.entity.Parrot.class),
                Arguments.of("dogs", PetType.Pets.DOG, org.bukkit.entity.Wolf.class),
                Arguments.of("cats", PetType.Pets.CAT, org.bukkit.entity.Cat.class)
        );
    }

    @Test
    @DisplayName("Can't teleport in ProtectedRegions without tppets.tpanywhere")
    void cannotTeleportInProtectedRegionsWithoutPermission() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Entity instances
            Entity correctPet = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);
            Entity incorrectPet = MockFactory.getMockEntity("MockIncorrectPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Arrays.asList(correctPet, incorrectPet);
            when(this.world.getEntitiesByClasses(PetType.getClassTranslate(PetType.Pets.HORSE))).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Permissions modifications
            when(tpPets.canTpThere(any())).thenReturn(false);

            this.setAliases("horses");

            // Command object with no second argument
            String[] args = {"horses", "HORSE0"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Can't teleport without tppets.horses")
    void cannotTeleportWithoutPetPermission() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Entity instances
            Entity correctPet = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);
            Entity incorrectPet = MockFactory.getMockEntity("MockIncorrectPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Arrays.asList(correctPet, incorrectPet);
            when(this.world.getEntitiesByClasses(PetType.getClassTranslate(PetType.Pets.HORSE))).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Permissions modifications
            when(this.player.hasPermission("tppets.horses")).thenReturn(false);

            this.setAliases("horses");

            // Command object with no second argument
            String[] args = {"horses", "HORSE0"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You do not have permission to use that command.", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("No pet name provided")
    void cannotTeleportWithoutName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Entity instances
            Entity correctPet = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);
            Entity incorrectPet = MockFactory.getMockEntity("MockIncorrectPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Arrays.asList(correctPet, incorrectPet);
            when(this.world.getEntitiesByClasses(PetType.getClassTranslate(PetType.Pets.HORSE))).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases("horses");

            // Command object with no second argument
            String[] args = {"horses"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax error! /tpp [pet type] [all/list/dog name]", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Defaults to teleporting first argument pet name when 2 arguments provided without f:[username] syntax")
    void defaultsToTeleportingPetWithFirstArgument() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Entity instances
            Entity correctPet = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);
            Entity incorrectPet = MockFactory.getMockEntity("MockIncorrectPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Arrays.asList(correctPet, incorrectPet);
            when(this.world.getEntitiesByClasses(PetType.getClassTranslate(PetType.Pets.HORSE))).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases("horses");

            // Command object
            String[] args = {"horses", "HORSE0", "spare argument"};
            this.commandTPP.onCommand(this.player, this.command, "", args);


            verify(this.chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            if (correctPet instanceof Sittable) {
                verify((Sittable)correctPet, times(1)).setSitting(false);
            }
            verify(correctPet).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getX(), 0.5);
            assertEquals(200, capturedPetLocation.getY(), 0.5);
            assertEquals(300, capturedPetLocation.getZ(), 0.5);
            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named HORSE0 to their location at: x: 100, y: 200, z: 300", capturedLogOutput);
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));
        }
    }

    @Test
    @DisplayName("Pet with name not in database")
    void invalidPetName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Entity instances
            Entity correctPet = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Collections.singletonList(correctPet);
            when(this.world.getEntitiesByClasses(PetType.getClassTranslate(PetType.Pets.HORSE))).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases("horses");

            // Command object with no second argument
            String[] args = {"horses", "HORSE0;"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find pet with name " + ChatColor.WHITE + "HORSE0;", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Invalid pet name")
    void petNotInDatabase() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Entity instances
            Entity correctPet = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Collections.singletonList(correctPet);
            when(this.world.getEntitiesByClasses(PetType.getClassTranslate(PetType.Pets.HORSE))).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(null);
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases("horses");

            // Command object with no second argument
            String[] args = {"horses", "HORSE0"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.player).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find pet with name " + ChatColor.WHITE + "HORSE0", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin teleporting another player's horse")
    void adminTeleportsHorse() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // The correct pet Entity instance
            Horse correctPet = (Horse) MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);

            // The incorrect pet Entity instance
            Horse incorrectPet = (Horse) MockFactory.getMockEntity("MockIncorrectPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Arrays.asList(correctPet, incorrectPet);
            when(this.world.getEntitiesByClasses(org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Command aliases
            this.setAliases("horses");

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Command object
            String[] args = {"horses", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(400, capturedPetLocation.getX(), 0.5);
            assertEquals(500, capturedPetLocation.getY(), 0.5);
            assertEquals(600, capturedPetLocation.getZ(), 0.5);
            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("Player MockAdminName teleported MockPlayerName's pet named HORSE0 to their location at: x: 400, y: 500, z: 600", capturedLogOutput);
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's " + ChatColor.BLUE + "pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }

    @Test
    @DisplayName("Admin teleporting another player's horse when the player hasn't played before")
    void adminHorseTeleportPlayerNotPlayedBefore() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // The correct pet Entity instance
            Horse correctPet = (Horse) MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Collections.singletonList(correctPet);
            when(this.world.getEntitiesByClasses(org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Command aliases
            this.setAliases("horses");

            // Player who owns the pet
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Command object
            String[] args = {"horses", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player " + ChatColor.WHITE + "MockOwnerName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin teleporting another player's horse with invalid player name")
    void adminHorseTeleportInvalidPlayerName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // The correct pet Entity instance
            Horse correctPet = (Horse) MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Collections.singletonList(correctPet);
            when(this.world.getEntitiesByClasses(org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Command aliases
            this.setAliases("horses");

            // Command object
            String[] args = {"horses", "f:MockOwnerName;", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player " + ChatColor.WHITE + "MockOwnerName;", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Player without permissions trying to teleport another player's pet")
    void playerHorseTeleportInsufficientPermissions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // The correct pet Entity instance
            Horse correctPet = (Horse) MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Collections.singletonList(correctPet);
            when(this.world.getEntitiesByClasses(org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Command aliases
            this.setAliases("horses");

            // Permissions adjustments
            when(this.tpPets.isAllowedToPet(anyString(), anyString())).thenReturn(false);
            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Command object
            String[] args = {"horses", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Player with permissions trying to teleport another player's pet")
    void guestHorseTeleportWithPermissions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // The correct pet Entity instance
            Horse correctPet = (Horse) MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);

            // The incorrect pet Entity instance
            Horse incorrectPet = (Horse) MockFactory.getMockEntity("MockIncorrectPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Arrays.asList(correctPet, incorrectPet);
            when(this.world.getEntitiesByClasses(org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Command aliases
            this.setAliases("horses");

            // Permissions adjustments
            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Command object
            String[] args = {"horses", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.chunk, times(1)).load();
            // Not ejected because players shouldn't be able to kick other players off
            verify(correctPet, never()).eject();
            verify(correctPet).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(400, capturedPetLocation.getX(), 0.5);
            assertEquals(500, capturedPetLocation.getY(), 0.5);
            assertEquals(600, capturedPetLocation.getZ(), 0.5);
            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("Player MockAdminName teleported MockPlayerName's pet named HORSE0 to their location at: x: 400, y: 500, z: 600", capturedLogOutput);
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's " + ChatColor.BLUE + "pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));
        }
    }

    @Test
    @DisplayName("Player with permissions trying to teleport another player's pet while there is a passenger")
    void guestHorseTeleportWithPermissionsPassenger() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // The correct pet Entity instance
            Horse correctPet = (Horse) MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);

            // The incorrect pet Entity instance
            Horse incorrectPet = (Horse) MockFactory.getMockEntity("MockIncorrectPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = Arrays.asList(correctPet, incorrectPet);
            when(this.world.getEntitiesByClasses(org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = Collections.singletonList(pet);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(Collections.singletonList(pet));
            when(this.dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Command aliases
            this.setAliases("horses");

            // Permissions adjustments
            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(this.player);

            // Passenger adjustments. Entity list does not have size = 0
            when(correctPet.getPassengers()).thenReturn(entityList);

            // Command object
            String[] args = {"horses", "f:MockOwnerName", "HORSE0"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.chunk, times(1)).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(this.admin).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't teleport " + ChatColor.WHITE + "HORSE0", capturedMessageOutput);
        }
    }
}
