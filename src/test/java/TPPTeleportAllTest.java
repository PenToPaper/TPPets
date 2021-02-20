import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TPPTeleportAllTest {
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
    public void beforeEach() {
        this.world = mock(World.class);
        when(this.world.getName()).thenReturn("MockWorld");
        this.worldList = new ArrayList<>();
        this.worldList.add(this.world);
        this.playerLocation = MockFactory.getMockLocation(this.world, 100, 200, 300);
        this.adminLocation = MockFactory.getMockLocation(this.world, 400, 500, 600);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", this.world, this.playerLocation, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.parrots", "tppets.cats", "tppets.dogs"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, this.adminLocation, new String[]{"tppets.donkeys", "tppets.llamas", "tppets.mules", "tppets.horses", "tppets.parrots", "tppets.cats", "tppets.dogs", "tppets.teleportother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.chunk = mock(Chunk.class);
        when(this.world.getChunkAt(anyInt(), anyInt())).thenReturn(this.chunk);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.teleportCaptor = ArgumentCaptor.forClass(Location.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        this.command = mock(Command.class);
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
    void teleportsValidPets(PetType.Pets petType, Class<? extends Entity> className) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, className);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", PetType.getIndexFromPet(petType), 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", PetType.getIndexFromPet(petType), 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", PetType.getIndexFromPet(petType), 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", petType)).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all", petType.toString().toLowerCase()};
            this.commandTPP.onCommand(this.player, this.command, "", args);

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
    void cannotTeleportInProtectedRegionsWithoutPermission() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Permissions modifications
            when(this.tpPets.canTpThere(any())).thenReturn(false);

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

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
    void cannotTeleportWithoutPetPermission() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Permissions modifications
            when(this.player.hasPermission("tppets.horses")).thenReturn(false);

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

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
    void cannotTeleportWithoutType() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

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
    void invalidPetType() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all", "notapet"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

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
    void cannotTeleportNoPetsInDb() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(new ArrayList<>());

            this.setAliases();

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

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
    void cannotTeleportDbError() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(null);

            this.setAliases();

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.player, ChatColor.RED + "Could not get any pets", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Reports teleporting error to user")
    void cannotTeleportTeleportError() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // Chunk
            when(this.chunk.getEntities()).thenReturn(new Entity[]{});

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.chunk, times(3)).load();
            checkPlayerResponse(this.player, ChatColor.RED + "Could not teleport pets", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Does not report teleporting error to user if some pets were successfully teleported")
    void cannotTeleportTeleportErrorUnreported() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all", "horse"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.chunk, times(3)).load();
            checkPetIsTeleported(entities[0], this.playerLocation, this.teleportCaptor);
            checkPetIsNotTeleported(entities[1]);
            checkPlayerResponse(this.player, ChatColor.BLUE + "Your " + ChatColor.WHITE + "horses " + ChatColor.BLUE + "have been teleported to you", this.messageCaptor);
        }
    }

    @Test
    @DisplayName("Admin teleporting all of another player's horses")
    void adminTeleportsHorses() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Player who owns the pet
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"all", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

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
    void cantAdminTeleportHorsesPlayerNotPlayedBefore() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Player who owns the pet
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"all", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

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
    void cantAdminTeleportHorsesInvalidPlayerName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Command object
            String[] args = {"all", "f:MockPlayerName;", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

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
    void cantAdminTeleportHorsesInsufficientPermissions() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(Bukkit::getWorlds).thenReturn(this.worldList);

            // A list of both entities
            Entity[] entities = getEntityList(new String[]{"MockPetId0", "MockPetId1", "MockPetId2", "MockIncorrectPetId"}, org.bukkit.entity.Horse.class);
            when(this.chunk.getEntities()).thenReturn(entities);

            // PetStorage
            PetStorage pet0 = new PetStorage("MockPetId0", 7, 100, 100, 100, "MockWorld", "MockPlayerId", "CorrectPet0", "CorrectPet0");
            PetStorage pet1 = new PetStorage("MockPetId1", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet1", "CorrectPet0");
            PetStorage pet2 = new PetStorage("MockPetId2", 7, 200, 200, 200, "MockWorld", "MockPlayerId", "CorrectPet2", "CorrectPet0");
            List<PetStorage> petList = Arrays.asList(pet0, pet1, pet2);

            // Plugin database wrapper instance
            when(this.dbWrapper.getPetsGeneric("MockPlayerId", "MockWorld", PetType.Pets.HORSE)).thenReturn(petList);

            this.setAliases();

            // Player who owns the pet
            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            // Command object
            String[] args = {"all", "f:MockPlayerName", "horse"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.chunk, never()).load();
            checkPetIsNotTeleported(entities[0]);
            checkPetIsNotTeleported(entities[1]);
            checkPetIsNotTeleported(entities[2]);
            checkPetIsNotTeleported(entities[3]);
            checkPlayerResponse(this.admin, ChatColor.RED + "You don't have permission to do that", this.messageCaptor);
        }
    }
}