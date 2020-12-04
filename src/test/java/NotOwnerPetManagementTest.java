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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

@DisplayName("Teleporting unowned pets to valid players with permission")
class NotOwnerPetManagementTest {

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

    @Test
    @DisplayName("Teleports all unowned horses to players that have tppets.teleportother")
    void teleportsValidAllUnownedHorses() {
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
            Player sender = TeleportMocksFactory.getMockPlayer("MockPlayerId", sendTo, world,"MockPlayerName", new String[]{"tppets.horses", "tppets.teleportother"});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Player who owns the pet
            OfflinePlayer owner = TeleportMocksFactory.getMockOfflinePlayer("MockOwnerId", "MockOwnerName");
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(owner);

            // The 1st correct pet Entity instance
            Tameable correctPetOne = (Tameable) TeleportMocksFactory.getMockEntity("MockPetOneId", org.bukkit.entity.Horse.class);
            when(correctPetOne.isTamed()).thenReturn(true);
            when(correctPetOne.getOwner()).thenReturn(owner);
            ArgumentCaptor<Location> correctPetCaptorOne = ArgumentCaptor.forClass(Location.class);

            // The 2nd correct pet Entity instance
            Tameable correctPetTwo = (Tameable) TeleportMocksFactory.getMockEntity("MockPetTwoId", org.bukkit.entity.Horse.class);
            when(correctPetTwo.isTamed()).thenReturn(true);
            when(correctPetTwo.getOwner()).thenReturn(owner);
            ArgumentCaptor<Location> correctPetCaptorTwo = ArgumentCaptor.forClass(Location.class);

            // The incorrect pet Entity instance
            Tameable incorrectPetOne = (Tameable) TeleportMocksFactory.getMockEntity("MockIncorrectId", org.bukkit.entity.Horse.class);
            when(incorrectPetOne.isTamed()).thenReturn(false);

            // The incorrect pet Entity instance
            Tameable incorrectPetTwo = (Tameable) TeleportMocksFactory.getMockEntity("MockIncorrectId", org.bukkit.entity.Horse.class);
            when(incorrectPetTwo.isTamed()).thenReturn(true);
            when(incorrectPetTwo.getOwner()).thenReturn(null);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPetOne);
            entityList.add(correctPetTwo);
            entityList.add(incorrectPetOne);
            entityList.add(incorrectPetTwo);
            when(world.getEntitiesByClasses(org.bukkit.entity.Horse.class, org.bukkit.entity.SkeletonHorse.class, org.bukkit.entity.ZombieHorse.class)).thenReturn(entityList);

            // PetStorage
            PetStorage petOne = new PetStorage("MockPetOneId", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "MockPetOneName", "MockPetOneName");
            PetStorage petTwo = new PetStorage("MockPetTwoId", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "MockPetTwoName", "MockPetTwoName");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(petOne);
            petList.add(petTwo);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetsGeneric("MockOwnerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

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

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "f:MockOwnerName", "all"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunkOne, times(1)).load();
            verify(chunkTwo, times(1)).load();
            verify(correctPetOne, times(1)).eject();
            verify(correctPetTwo, times(1)).eject();

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
            assertEquals("Player MockPlayerName teleported 2 of MockOwnerName's HORSEs to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockOwnerName's " + ChatColor.WHITE + "HORSEs " + ChatColor.BLUE + "have been teleported to you", capturedMessageOutput);

            verify(incorrectPetOne, times(0)).teleport(any(Location.class));
            verify(incorrectPetTwo, times(0)).teleport(any(Location.class));
        }
    }

    @Test
    @DisplayName("Lists all owned horses of other players to players that have tppets.teleportother")
    void listsAllPlayerOwnedPets() {
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

            // Player who owns the pet
            OfflinePlayer owner = TeleportMocksFactory.getMockOfflinePlayer("MockOwnerId", "MockOwnerName");
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(owner);

            // Location of sender
            Location sendTo = TeleportMocksFactory.getMockLocation(world, 1000, 100, 1000);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockPlayerId", sendTo, world,"MockPlayerName", new String[]{"tppets.horses", "tppets.teleportother"});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // PetStorage
            PetStorage petOne = new PetStorage("MockPetOneId", 7, 100, 100, 100, "MockWorld", "MockOwnerId", "MockPetOneName", "MockPetOneName");
            PetStorage petTwo = new PetStorage("MockPetTwoId", 7, 200, 200, 200, "MockWorld", "MockOwnerId", "MockPetTwoName", "MockPetTwoName");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(petOne);
            petList.add(petTwo);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetsGeneric("MockOwnerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, true);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("horses");
            aliases.put("horses", altAlias);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "f:MockOwnerName", "list"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(sender, times(4)).sendMessage(playerMessageCaptor.capture());
            List<String> messages = playerMessageCaptor.getAllValues();
            assertEquals(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + "MockOwnerName's " + ChatColor.BLUE + "HORSE names ]" + ChatColor.DARK_GRAY + "---------", messages.get(0));
            assertEquals(ChatColor.WHITE + "  1) MockPetOneName", messages.get(1));
            assertEquals(ChatColor.WHITE + "  2) MockPetTwoName", messages.get(2));
            assertEquals(ChatColor.DARK_GRAY + "----------------------------------", messages.get(3));
        }
    }


    @Test
    @DisplayName("Allows specific players to other players' pets and registers the association in database")
    void allowsOfflinePlayerAllowPermissions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockOwnerId", "MockPet", "MockPet");

            // Player who's being allowed to the pet
            OfflinePlayer guest = TeleportMocksFactory.getMockOfflinePlayer("MockGuestId", "MockGuestName");
            OfflinePlayer owner = TeleportMocksFactory.getMockOfflinePlayer("MockOwnerId", "MockOwnerName");
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(owner);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockAdminId", null, null,"MockAdminName", new String[]{"tppets.addallow", "tppets.allowother"});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockOwnerId", "MockPetName")).thenReturn(pet);
            when(dbWrapper.insertAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Allowed players hashtable instance
            List<String> allowedPlayersList = new ArrayList<>();
            Hashtable <String, List<String>> allowedPlayersTable = mock(Hashtable.class);
            when(allowedPlayersTable.containsKey("MockPetId")).thenReturn(false);
            when(allowedPlayersTable.get("MockPetId")).thenReturn(allowedPlayersList);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, true);
            when(tpPets.isAllowedToPet("MockPetId", "MockGuestId")).thenReturn(false);
            when(tpPets.getAllowedPlayers()).thenReturn(allowedPlayersTable);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("allow");
            aliases.put("allow", altAlias);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"allow", "f:MockOwnerName", "MockGuestName", "MockPetName"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(dbWrapper, times(1)).insertAllowedPlayer("MockPetId", "MockGuestId");
            verify(allowedPlayersTable, times(1)).put(any(), any());
            assertEquals("MockGuestId", allowedPlayersList.get(0));

            verify(logWrapper, times(1)).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("MockAdminName allowed MockGuestName to use MockOwnerName's pet named MockPetName", capturedLogOutput);

            verify(sender, times(1)).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.BLUE + " has been allowed to use " + ChatColor.WHITE + "MockOwnerName" + ChatColor.BLUE + "'s pet " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }


    @Test
    @DisplayName("Removes specific players' access to other players' pets and removes restiration from database")
    void allowsOfflinePlayerRemovePermissions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockOwnerId", "MockPet", "MockPet");

            // Player who's being allowed to the pet
            OfflinePlayer guest = TeleportMocksFactory.getMockOfflinePlayer("MockGuestId", "MockGuestName");
            OfflinePlayer owner = TeleportMocksFactory.getMockOfflinePlayer("MockOwnerId", "MockOwnerName");
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockGuestName")).thenReturn(guest);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(owner);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockAdminId", null, null,"MockAdminName", new String[]{"tppets.removeallow", "tppets.allowother"});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Allowed players hashtable instance
            List<String> allowedPlayersList = new ArrayList<>();
            allowedPlayersList.add("MockGuestId");
            Hashtable <String, List<String>> allowedPlayersTable = mock(Hashtable.class);
            when(allowedPlayersTable.containsKey("MockPetId")).thenReturn(false);
            when(allowedPlayersTable.get("MockPetId")).thenReturn(allowedPlayersList);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockOwnerId", "MockPetName")).thenReturn(pet);
            when(dbWrapper.deleteAllowedPlayer("MockPetId", "MockGuestId")).thenReturn(true);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, true);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.isAllowedToPet("MockPetId", "MockGuestId")).thenReturn(true);
            when(tpPets.getAllowedPlayers()).thenReturn(allowedPlayersTable);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("remove");
            aliases.put("remove", altAlias);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"remove", "f:MockOwnerName", "MockGuestName", "MockPetName"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(dbWrapper, times(1)).deleteAllowedPlayer("MockPetId", "MockGuestId");
            verify(allowedPlayersTable, times(1)).put(any(), any());
            assertEquals(0, allowedPlayersList.size());

            verify(logWrapper, times(1)).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("MockAdminName disallowed MockGuestName to use MockOwnerName's pet named MockPetName", capturedLogOutput);

            verify(sender, times(1)).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockGuestName" + ChatColor.BLUE + " is no longer allowed to use " + ChatColor.WHITE + "MockOwnerName" + ChatColor.BLUE + "'s pet " + ChatColor.WHITE + "MockPetName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Lists other players' access to other players' pets")
    void listsPlayersWithAccessToOwnedPets() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            // Owner of the pet
            OfflinePlayer owner = TeleportMocksFactory.getMockOfflinePlayer("MockOwnerId", "MockOwnerName");
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(owner);

            // Mock UUIDs allowed to pet, 32 digit UUIDs
            List<String> allowed = new ArrayList<>();
            allowed.add("00000000000000000000000000000000");
            allowed.add("11111111111111111111111111111111");
            allowed.add("22222222222222222222222222222222");
            allowed.add("33333333333333333333333333333333");

            List<UUID> allowedUUID = new ArrayList<>();
            allowedUUID.add(UUID.fromString("00000000-0000-0000-0000-000000000000"));
            allowedUUID.add(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            allowedUUID.add(UUID.fromString("22222222-2222-2222-2222-222222222222"));
            allowedUUID.add(UUID.fromString("33333333-3333-3333-3333-333333333333"));

            List<OfflinePlayer> allowedPlayers = new ArrayList<>();
            allowedPlayers.add(TeleportMocksFactory.getMockOfflinePlayer(allowed.get(0), "MockGuestName0"));
            allowedPlayers.add(TeleportMocksFactory.getMockOfflinePlayer(allowed.get(1), "MockGuestName1"));
            allowedPlayers.add(TeleportMocksFactory.getMockOfflinePlayer(allowed.get(2), "MockGuestName2"));
            allowedPlayers.add(TeleportMocksFactory.getMockOfflinePlayer(allowed.get(3), "MockGuestName3"));

            bukkit.when(() ->Bukkit.getOfflinePlayer(allowedUUID.get(0))).thenReturn(allowedPlayers.get(0));
            bukkit.when(() ->Bukkit.getOfflinePlayer(allowedUUID.get(1))).thenReturn(allowedPlayers.get(1));
            bukkit.when(() ->Bukkit.getOfflinePlayer(allowedUUID.get(2))).thenReturn(allowedPlayers.get(2));
            bukkit.when(() ->Bukkit.getOfflinePlayer(allowedUUID.get(3))).thenReturn(allowedPlayers.get(3));

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockOwnerId", "MockPet", "MockPet");

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName("MockOwnerId", "MockPetName")).thenReturn(pet);
            when(dbWrapper.getAllowedPlayers("MockPetId")).thenReturn(allowed);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, true);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockAdminId", null, null,"MockAdminName", new String[]{"tppets.listallow", "tppets.allowother"});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("list");
            aliases.put("list", altAlias);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"list", "f:MockOwnerName", "MockPetName"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(sender, times(6)).sendMessage(playerMessageCaptor.capture());
            List<String> messages = playerMessageCaptor.getAllValues();
            assertEquals(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE + "MockOwnerName's MockPetName" + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------", messages.get(0));
            assertEquals(ChatColor.WHITE + "MockGuestName0", messages.get(1));
            assertEquals(ChatColor.WHITE + "MockGuestName1", messages.get(2));
            assertEquals(ChatColor.WHITE + "MockGuestName2", messages.get(3));
            assertEquals(ChatColor.WHITE + "MockGuestName3", messages.get(4));
            assertEquals(ChatColor.GRAY + "-------------------------------------------", messages.get(5));
        }
    }


    @Test
    @DisplayName("Adds new storage locations for other players to the database")
    void addStorageLocationForOthers() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            OfflinePlayer owner = TeleportMocksFactory.getMockOfflinePlayer("MockOwnerId", "MockOwnerName");
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockOwnerName")).thenReturn(owner);

            // Player's world
            World world = mock(World.class);

            // Player's location
            Location playerLoc = TeleportMocksFactory.getMockLocation(world, 100, 200, 300);

            // Player who sent the command
            Player sender = TeleportMocksFactory.getMockPlayer("MockAdminId", playerLoc, world,"MockAdminName", new String[]{"tppets.storage", "tppets.storageother"});
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getStorageLocation("MockOwnerId", "StorageName")).thenReturn(null);
            when(dbWrapper.getStorageLocations("MockOwnerId")).thenReturn(new ArrayList<>());
            when(dbWrapper.addStorageLocation("MockOwnerId", "StorageName", playerLoc)).thenReturn(true);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = TeleportMocksFactory.getMockPlugin(dbWrapper, logWrapper, true, false, true);
            when(tpPets.getStorageLimit()).thenReturn(1);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("storage");
            aliases.put("storage", altAlias);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"storage", "f:MockOwnerName", "add", "StorageName"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);


            verify(dbWrapper, times(1)).addStorageLocation(anyString(), anyString(), any(Location.class));

            verify(logWrapper, times(1)).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockAdminId has added location StorageName x: 100, y: 200, z: 300 for MockOwnerName", capturedLogOutput);

            verify(sender, times(1)).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockOwnerName" + ChatColor.BLUE + " has added storage location " + ChatColor.WHITE + "StorageName", capturedMessageOutput);
        }
    }
}