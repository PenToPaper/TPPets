import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.World;
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
import java.util.UUID;

// TODO: REFACTOR SO NOT REPEATING AS MUCH
@DisplayName("Teleporting owned pets to players")
class ToOwnerTeleportTest {

    @Test
    @DisplayName("Teleports valid owned horses with /tpp horse [horse name]")
    void teleportsValidHorse() {
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
            Horse correctPet = mock(Horse.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

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
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("horses");
            aliases.put("horses", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission("tppets.teleportother")).thenReturn(false);
            when(sender.hasPermission("tppets.horses")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "HORSE0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named HORSE0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }


    @Test
    @DisplayName("Teleports valid owned zombie horses with /tpp horse [horse name]")
    void teleportsValidZombieHorse() {
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
            ZombieHorse correctPet = mock(ZombieHorse.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

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
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("horses");
            aliases.put("horses", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission("tppets.teleportother")).thenReturn(false);
            when(sender.hasPermission("tppets.horses")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "HORSE0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named HORSE0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }


    @Test
    @DisplayName("Teleports valid owned zombie horses with /tpp horse [horse name]")
    void teleportsValidSkeletonHorse() {
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
            SkeletonHorse correctPet = mock(SkeletonHorse.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

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
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("horses");
            aliases.put("horses", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission("tppets.teleportother")).thenReturn(false);
            when(sender.hasPermission("tppets.horses")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "HORSE0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named HORSE0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }


    @Test
    @DisplayName("Teleports valid owned dogs with /tpp dog [dog name]")
    void teleportsValidDog() {
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
            Wolf correctPet = mock(Wolf.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(org.bukkit.entity.Wolf.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "DOG0", "DOG0");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockPlayerId", "DOG0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "DOG0", PetType.Pets.DOG)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("dogs");
            aliases.put("dogs", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission("tppets.teleportother")).thenReturn(false);
            when(sender.hasPermission("tppets.dogs")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"dogs", "DOG0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet, times(1)).setSitting(false);
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named DOG0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "DOG0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }


    @Test
    @DisplayName("Teleports valid owned cats with /tpp cat [cat name]")
    void teleportsValidCat() {
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
            Cat correctPet = mock(Cat.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(org.bukkit.entity.Cat.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CAT0", "CAT0");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockPlayerId", "CAT0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "CAT0", PetType.Pets.CAT)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("cats");
            aliases.put("cats", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission("tppets.teleportother")).thenReturn(false);
            when(sender.hasPermission("tppets.cats")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"cats", "CAT0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet, times(1)).setSitting(false);
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named CAT0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "CAT0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }


    @Test
    @DisplayName("Teleports valid owned birds with /tpp birds [bird name]")
    void teleportsValidBirds() {
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
            Parrot correctPet = mock(Parrot.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(org.bukkit.entity.Parrot.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "PARROT0", "PARROT0");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockPlayerId", "PARROT0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "PARROT0", PetType.Pets.PARROT)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("birds");
            aliases.put("birds", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission("tppets.teleportother")).thenReturn(false);
            when(sender.hasPermission("tppets.birds")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"birds", "PARROT0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet, times(1)).setSitting(false);
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named PARROT0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "PARROT0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }


    @Test
    @DisplayName("Teleports valid owned mules with /tpp mules [mule name]")
    void teleportsValidMules() {
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
            Mule correctPet = mock(Mule.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(org.bukkit.entity.Mule.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "MULE0", "MULE0");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockPlayerId", "MULE0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "MULE0", PetType.Pets.MULE)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("mules");
            aliases.put("mules", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission("tppets.teleportother")).thenReturn(false);
            when(sender.hasPermission("tppets.mules")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"mules", "MULE0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals(sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named MULE0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "MULE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }


    @Test
    @DisplayName("Teleports valid owned llamas with /tpp llamas [llama name]")
    void teleportsValidLlamas() {
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
            Llama correctPet = mock(Llama.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(org.bukkit.entity.Llama.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "LLAMA0", "LLAMA0");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockPlayerId", "LLAMA0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "LLAMA0", PetType.Pets.LLAMA)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("llamas");
            aliases.put("llamas", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission("tppets.teleportother")).thenReturn(false);
            when(sender.hasPermission("tppets.llamas")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"llamas", "LLAMA0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals(sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named LLAMA0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "LLAMA0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }


    @Test
    @DisplayName("Teleports valid owned donkeys with /tpp donkeys [donkey name]")
    void teleportsValidDonkeys() {
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
            Donkey correctPet = mock(Donkey.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(org.bukkit.entity.Donkey.class)).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "DONKEY0", "DONKEY0");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockPlayerId", "DONKEY0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockPlayerId", "DONKEY0", PetType.Pets.DONKEY)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("donkeys");
            aliases.put("donkeys", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission("tppets.teleportother")).thenReturn(false);
            when(sender.hasPermission("tppets.donkeys")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"donkeys", "DONKEY0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals(sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named DONKEY0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "DONKEY0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }

}