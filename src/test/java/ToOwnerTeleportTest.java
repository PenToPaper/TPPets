import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.junit.jupiter.api.DisplayName;
import static org.junit.Assert.*;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.command.Command;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@DisplayName("Teleporting owned pets to players")
class ToOwnerTeleportTest {

    @ParameterizedTest
    @MethodSource("teleportsPetsProvider")
    void teleportsValidPets(String commandString, PetType.Pets petType, Class<? extends Entity> className) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            String petName = commandString.toUpperCase() + "0";

            // World instance
            World world = mock(World.class);
            List<World> worlds = new ArrayList<>();
            worlds.add(world);

            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(world);
            bukkit.when(Bukkit::getWorlds).thenReturn(worlds);

            // Chunk + Location the correct pet is in
            Chunk chunk = mock(Chunk.class);
            Location location = mock(Location.class);
            when(location.getChunk()).thenReturn(chunk);
            when(world.getChunkAt(100, 100)).thenReturn(chunk);

            // The correct pet Entity instance
            Entity correctPet = TeleportMocksFactory.getMockEntity("MockPetId", className);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);

            // The incorrect pet Entity instance
            Entity incorrectPet = TeleportMocksFactory.getMockEntity("MockIncorrectPetId", className);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(PetType.getClassTranslate(petType))).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", petName, petName);
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockPlayerId", petName)).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", petName, petType)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, true);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add(commandString);
            aliases.put(commandString, altAlias);

            // Location to send the pet to
            Location sendTo = TeleportMocksFactory.getMockLocation(world, 1000, 100, 1000);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockPlayerId", sendTo, world,"MockPlayerName", new String[]{"tppets." + commandString});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {commandString, petName};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            if (correctPet instanceof Sittable) {
                verify((Sittable)correctPet, times(1)).setSitting(false);
            }
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            assertEquals( sendTo.getY(), capturedPetLocation.getY(), 0.5);
            assertEquals( sendTo.getZ(), capturedPetLocation.getZ(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named " + petName + " to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + petName + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }

    @ParameterizedTest
    @MethodSource("teleportsPetsProvider")
    void teleportsAllValidPets(String commandString, PetType.Pets petType, Class<? extends Entity> className) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            // World instance
            World world = mock(World.class);
            when(world.getName()).thenReturn("MockWorld");
            List<World> worlds = new ArrayList<>();
            worlds.add(world);

            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(world);
            bukkit.when(Bukkit::getWorlds).thenReturn(worlds);

            // Server
            Server server = mock(Server.class);
            when(server.getWorlds()).thenReturn(worlds);
            bukkit.when(Bukkit::getServer).thenReturn(server);

            // Chunk + Location the correct pet is in
            Chunk chunkOne = mock(Chunk.class);
            Location locationOne = mock(Location.class);
            when(locationOne.getChunk()).thenReturn(chunkOne);
            Chunk chunkTwo = mock(Chunk.class);
            Location locationTwo = mock(Location.class);
            when(locationTwo.getChunk()).thenReturn(chunkTwo);
            when(world.getChunkAt(100, 100)).thenReturn(chunkOne);
            when(world.getChunkAt(200, 200)).thenReturn(chunkTwo);

            // Location to send the pet to
            Location sendTo = TeleportMocksFactory.getMockLocation(world, 1000, 100, 1000);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockPlayerId", sendTo, world,"MockPlayerName", new String[]{"tppets." + commandString});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // The 1st correct pet Entity instance
            Tameable correctPetOne = (Tameable) TeleportMocksFactory.getMockEntity("MockPetOneId", className);
            when(correctPetOne.isTamed()).thenReturn(true);
            when(correctPetOne.getOwner()).thenReturn(sender);
            ArgumentCaptor<Location> correctPetCaptorOne = ArgumentCaptor.forClass(Location.class);

            // The 2nd correct pet Entity instance
            Tameable correctPetTwo = (Tameable) TeleportMocksFactory.getMockEntity("MockPetTwoId", className);
            when(correctPetTwo.isTamed()).thenReturn(true);
            when(correctPetTwo.getOwner()).thenReturn(sender);
            ArgumentCaptor<Location> correctPetCaptorTwo = ArgumentCaptor.forClass(Location.class);

            // The incorrect pet Entity instance
            Tameable incorrectPetOne = (Tameable) TeleportMocksFactory.getMockEntity("MockIncorrectId", className);
            when(incorrectPetOne.isTamed()).thenReturn(false);

            // The incorrect pet Entity instance
            Tameable incorrectPetTwo = (Tameable) TeleportMocksFactory.getMockEntity("MockIncorrectId", className);
            when(incorrectPetTwo.isTamed()).thenReturn(true);
            when(incorrectPetTwo.getOwner()).thenReturn(null);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPetOne);
            entityList.add(correctPetTwo);
            entityList.add(incorrectPetOne);
            entityList.add(incorrectPetTwo);
            when(world.getEntitiesByClasses(PetType.getClassTranslate(petType))).thenReturn(entityList);

            // PetStorage
            PetStorage petOne = new PetStorage("MockPetOneId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "MockPetOneName", "MockPetOneName");
            PetStorage petTwo = new PetStorage("MockPetTwoId", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "MockPetTwoName", "MockPetTwoName");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(petOne);
            petList.add(petTwo);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", petType)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, true);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add(commandString);
            aliases.put(commandString, altAlias);

            // Command object
            Command command = mock(Command.class);
            String[] args = {commandString, "all"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunkOne, times(1)).load();
            verify(chunkTwo, times(1)).load();
            verify(correctPetOne, times(1)).eject();
            if (correctPetOne instanceof Sittable) {
                verify((Sittable)correctPetOne, times(1)).setSitting(false);
            }
            verify(correctPetTwo, times(1)).eject();
            if (correctPetTwo instanceof Sittable) {
                verify((Sittable)correctPetTwo, times(1)).setSitting(false);
            }
            verify(correctPetOne).teleport(correctPetCaptorOne.capture());
            Location capturedPetLocationOne = correctPetCaptorOne.getValue();
            assertEquals( sendTo.getX(), capturedPetLocationOne.getX(), 0.5);
            assertEquals( sendTo.getY(), capturedPetLocationOne.getY(), 0.5);
            assertEquals( sendTo.getZ(), capturedPetLocationOne.getZ(), 0.5);

            verify(correctPetTwo).teleport(correctPetCaptorTwo.capture());
            Location capturedPetLocationTwo = correctPetCaptorTwo.getValue();
            assertEquals( sendTo.getX(), capturedPetLocationTwo.getX(), 0.5);
            assertEquals( sendTo.getY(), capturedPetLocationTwo.getY(), 0.5);
            assertEquals( sendTo.getZ(), capturedPetLocationTwo.getZ(), 0.5);

            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported 2 of MockPlayerName's " + petType.toString() + "s to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your " + ChatColor.WHITE + petType.toString() + "s " + ChatColor.BLUE + "have been teleported to you", capturedMessageOutput);

            verify(incorrectPetOne, times(0)).teleport(any(Location.class));
            verify(incorrectPetTwo, times(0)).teleport(any(Location.class));
        }
    }


    @ParameterizedTest
    @MethodSource("teleportsPetsProvider")
    void listsAllOwnedPets(String commandString, PetType.Pets petType, Class<? extends Entity> className) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            // World instance
            World world = mock(World.class);
            when(world.getName()).thenReturn("MockWorld");
            List<World> worlds = new ArrayList<>();
            worlds.add(world);

            // Server
            Server server = mock(Server.class);
            when(server.getWorlds()).thenReturn(worlds);
            bukkit.when(Bukkit::getServer).thenReturn(server);

            // Location of sender
            Location sendTo = TeleportMocksFactory.getMockLocation(world, 1000, 100, 1000);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockPlayerId", sendTo, world,"MockPlayerName", new String[]{"tppets." + commandString});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // PetStorage
            PetStorage petOne = new PetStorage("MockPetOneId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "MockPetOneName", "MockPetOneName");
            PetStorage petTwo = new PetStorage("MockPetTwoId", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "MockPetTwoName", "MockPetTwoName");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(petOne);
            petList.add(petTwo);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", petType)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, true);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add(commandString);
            aliases.put(commandString, altAlias);

            // Command object
            Command command = mock(Command.class);
            String[] args = {commandString, "list"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(sender, times(4)).sendMessage(playerMessageCaptor.capture());
            List<String> messages = playerMessageCaptor.getAllValues();
            assertEquals(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockPlayerName's " + ChatColor.BLUE + petType.toString() + " names ]" + ChatColor.DARK_GRAY + "---------", messages.get(0));
            assertEquals(ChatColor.WHITE + "  1) MockPetOneName", messages.get(1));
            assertEquals(ChatColor.WHITE + "  2) MockPetTwoName", messages.get(2));
            assertEquals(ChatColor.DARK_GRAY + "----------------------------------", messages.get(3));
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
}