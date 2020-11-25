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
            Horse correctPet = mock(Horse.class);
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

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(false);
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
            when(sender.hasPermission("tppets.teleportother")).thenReturn(true);
            when(sender.hasPermission("tppets.horses")).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");

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
            Horse correctPet = mock(Horse.class);
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
            when(sender.hasPermission("tppets.horses")).thenReturn(false);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");

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
            when(dbWrapper.getPetByName("MockOwnerId", "HORSE0")).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType("MockOwnerId", "HORSE0", PetType.Pets.HORSE)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);
            when(tpPets.isAllowedToPet(anyString(), anyString())).thenReturn(false);

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

            // Player who owns the pet
            OfflinePlayer owner = mock(OfflinePlayer.class);
            when(owner.hasPlayedBefore()).thenReturn(true);
            UUID ownerUUID = mock(UUID.class);
            when(ownerUUID.toString()).thenReturn("MockOwnerId");
            when(owner.getName()).thenReturn("MockOwnerName");
            when(owner.getUniqueId()).thenReturn(ownerUUID);
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