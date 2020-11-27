import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.command.Command;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

@DisplayName("Teleporting pets you don't have permission to teleport")
class LackPermissionsTeleportTest {

    @Test
    @DisplayName("Denies teleporting pets in ProtectedRegions without admin permissions")
    void doesNotTeleportWithinProtectedRegions() {
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
            when(dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, false, false, true);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("horses");
            aliases.put("horses", altAlias);

            // Location to send the pet to
            Location sendTo = TeleportMocksFactory.getMockLocation(world, 1000, 100, 1000);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockPlayerId", sendTo, world,"MockPlayerName", new String[]{"tppets.horses", "tppets.teleportother"});

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "HORSE0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(incorrectPet, never()).teleport(any(Location.class));
        }
    }


    @Test
    @DisplayName("Denies teleporting pets for players without teleport.[mob type] permissions")
    void doesNotTeleportWithoutTeleportMobPermissions() {
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
            when(dbWrapper.getPetByName("MockPlayerId", "HORSE0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);

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
            Player sender = TeleportMocksFactory.getMockPlayer("MockPlayerId", sendTo, world,"MockPlayerName", new String[]{});

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "HORSE0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(incorrectPet, never()).teleport(any(Location.class));
        }
    }

    @Test
    @DisplayName("Denies teleporting pets when player does not have correct owner name f:[owner]")
    void doesNotTeleportWithIncorrectPetName() {
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

            // The incorrect pet Entity instance
            Horse incorrectPet = (Horse) TeleportMocksFactory.getMockEntity("MockIncorrectPetId", org.bukkit.entity.Horse.class);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("NotMockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockPlayerId", "HORSE1")).thenReturn(null);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "HORSE1", PetType.Pets.HORSE)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);

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

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "HORSE1"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find pet with name " + ChatColor.WHITE + "HORSE1", capturedMessageOutput);
            verify(incorrectPet, never()).teleport(any(Location.class));
        }
    }


    @Test
    @DisplayName("Denies teleporting pets when player does not have permission to the pet")
    void doesNotTeleportWithoutPermission() {
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

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, false);

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
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(owner);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "f:MockOwnerName", "HORSE0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, never()).load();
            verify(correctPet, never()).eject();
            verify(correctPet, never()).teleport(any(Location.class));
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", capturedMessageOutput);
            verify(incorrectPet, never()).teleport(any(Location.class));

        }
    }
}