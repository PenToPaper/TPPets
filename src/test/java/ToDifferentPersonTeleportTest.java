import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.*;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.command.Command;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@DisplayName("Teleporting unowned pets to valid players with permission")
class ToDifferentPersonTeleportTest {

    @Test
    @DisplayName("Teleports unowned horses to players that have been explicitly granted permission by the owner")
    void teleportsValidUnownedHorse() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
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
            Horse correctPet = (Horse) TeleportMocksFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);

            // The incorrect pet Entity instance
            Horse incorrectPet = (Horse) TeleportMocksFactory.getMockEntity("MockIncorrectPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockOwnerId", "HORSE0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockOwnerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, true);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("horses");
            aliases.put("horses", altAlias);

            // Location to send the pet to
            Location sendTo = TeleportMocksFactory.getMockLocation(world, 1000, 100, 1000);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockPlayerId", sendTo, world,"MockPlayerName", new String[]{"tppets.horses"});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Player who owns the pet
            OfflinePlayer owner = TeleportMocksFactory.getMockOfflinePlayer("MockOwnerId", "MockOwnerName");
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(owner);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "f:MockOwnerName", "HORSE0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockOwnerName's pet named HORSE0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockOwnerName's " + ChatColor.BLUE + "pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }

}