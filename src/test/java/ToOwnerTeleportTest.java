import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.World;
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
            verify(correctPet).teleport(correctPetCaptor.capture());
            if (correctPet instanceof Sittable) {
                verify((Sittable)correctPet, times(1)).setSitting(false);
            }
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named " + petName + " to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
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

}