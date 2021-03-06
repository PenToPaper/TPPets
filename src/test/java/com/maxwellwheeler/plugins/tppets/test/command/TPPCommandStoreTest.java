package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegionManager;
import com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPCommandStoreTest {
    private World world;
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private ArgumentCaptor<String> logCaptor;
    private ArgumentCaptor<Location> teleportCaptor;
    private SQLWrapper sqlWrapper;
    private Command command;
    private CommandTPP commandTPP;
    private ServerStorageLocation serverStorageLocation;
    private PlayerStorageLocation playerStorageLocation;
    private ProtectedRegionManager protectedRegionManager;
    private LogWrapper logWrapper;
    private PetStorage pet;
    private Chunk chunk;
    private Horse horse;
    private TPPets tpPets;
    private Server server;

    @BeforeEach
    public void beforeEach() {
        // Players
        this.world = mock(World.class);
        when(this.world.getName()).thenReturn("MockWorld");
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", this.world, null, new String[]{"tppets.store"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, null, new String[]{"tppets.store", "tppets.teleportother", "tppets.tpanywhere"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.teleportCaptor = ArgumentCaptor.forClass(Location.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.horse = MockFactory.getMockEntity("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", Horse.class);

        // Plugin
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);
        this.protectedRegionManager = mock(ProtectedRegionManager.class);
        when(this.protectedRegionManager.canTpThere(any(Player.class), any(Location.class))).thenReturn(true);
        when(this.tpPets.getProtectedRegionManager()).thenReturn(this.protectedRegionManager);

        // Command
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("store");
        aliases.put("store", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, this.tpPets);

        // Storage Location
        this.serverStorageLocation = MockFactory.getServerStorageLocation("StorageOne", 100, 200, 300, this.world);
        this.playerStorageLocation = MockFactory.getPlayerStorageLocation("StorageTwo", "MockPlayerId", 100, 200, 300, this.world);
        this.pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 700, 800, 900, "MockWorld", "MockPlayerId", "MockPetName", "MockPetName");
        this.chunk = mock(Chunk.class);

        this.server = mock(Server.class);
        when(this.tpPets.getServer()).thenReturn(this.server);
        when(this.server.getEntity(UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"))).thenReturn(this.horse);
        when(this.world.getChunkAt(43, 56)).thenReturn(this.chunk);
    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - store - " + commandStatus.toString(), logCaptor.getValue());
    }

    @Test
    @DisplayName("Teleports user's pets to default storage")
    void teleportsUsersPetsToDefaultStorage() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(this.serverStorageLocation);
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);

            String[] args = {"store", "PetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getServerStorageLocation(anyString(), any(World.class));

            verify(this.chunk, times(1)).load();

            verify(this.horse).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getBlockX(), 0.5);
            assertEquals(200, capturedPetLocation.getBlockY(), 0.5);
            assertEquals(300, capturedPetLocation.getBlockZ(), 0.5);

            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockPlayerName - store - stored MockPlayerName's PetName at default", capturedLogOutput);

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + " has been stored successfully", message);
        }
    }


    @Test
    @DisplayName("Admin teleports user's pets to default storage")
    void adminTeleportsUsersPetsToDefaultStorage() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(this.serverStorageLocation);
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);

            String[] args = {"store", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getServerStorageLocation(anyString(), any(World.class));

            verify(this.chunk, times(1)).load();

            verify(this.horse).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getBlockX(), 0.5);
            assertEquals(200, capturedPetLocation.getBlockY(), 0.5);
            assertEquals(300, capturedPetLocation.getBlockZ(), 0.5);

            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockAdminName - store - stored MockPlayerName's PetName at default", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's" + ChatColor.BLUE + " pet " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + " has been stored successfully", message);
        }
    }


    @Test
    @DisplayName("Teleports user's pets to specific storage")
    void teleportsUsersPetsToSpecificStorage() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.playerStorageLocation);
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);

            String[] args = {"store", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getStorageLocation(anyString(), anyString());

            verify(this.chunk, times(1)).load();

            verify(this.horse).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getBlockX(), 0.5);
            assertEquals(200, capturedPetLocation.getBlockY(), 0.5);
            assertEquals(300, capturedPetLocation.getBlockZ(), 0.5);

            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockPlayerName - store - stored MockPlayerName's PetName at StorageName", capturedLogOutput);

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + " has been stored successfully", message);
        }
    }


    @Test
    @DisplayName("Admin teleports user's pets to specific storage")
    void adminTeleportsUsersPetsToSpecificStorage() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.playerStorageLocation);
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);

            String[] args = {"store", "f:MockPlayerName", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getStorageLocation(anyString(), anyString());

            verify(this.chunk, times(1)).load();

            verify(this.horse).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getBlockX(), 0.5);
            assertEquals(200, capturedPetLocation.getBlockY(), 0.5);
            assertEquals(300, capturedPetLocation.getBlockZ(), 0.5);

            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockAdminName - store - stored MockPlayerName's PetName at StorageName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's" + ChatColor.BLUE + " pet " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + " has been stored successfully", message);
        }
    }


    @Test
    @DisplayName("Fails silently when admin sending is not a player")
    void cannotAdminTeleportUsersPetsNotPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("PlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.store")).thenReturn(true);

            String[] args = {"store", "f:PlayerName", "PetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            verifyLoggedUnsuccessfulAction("Unknown Sender", CommandStatus.INVALID_SENDER);

            verify(this.sqlWrapper, never()).getServerStorageLocation(anyString(), any(World.class));
            verify(this.chunk, never()).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(sender, never()).sendMessage(anyString());
        }
    }


    @Test
    @DisplayName("Admin cannot teleport user's pets without permission")
    void cannotAdminTeleportUsersPetsNoPermission() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);

            String[] args = {"store", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.INSUFFICIENT_PERMISSIONS);

            verify(this.sqlWrapper, never()).getServerStorageLocation(anyString(), any(World.class));
            verify(this.chunk, never()).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", message);
        }
    }

    @Test
    @DisplayName("Admin cannot teleport user's pets without valid user")
    void cannotAdminTeleportUsersPetsNoPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.hasPlayedBefore()).thenReturn(false);

            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"store", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.sqlWrapper, never()).getServerStorageLocation(anyString(), any(World.class));
            verify(this.chunk, never()).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", message);
        }
    }


    @Test
    @DisplayName("Admin cannot teleport user's pets without pet name")
    void cannotAdminTeleportUsersPetsNoPetName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"store", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

            verify(this.sqlWrapper, never()).getServerStorageLocation(anyString(), any(World.class));
            verify(this.chunk, never()).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp store [pet name] [storage name]", message);
        }
    }


    @Test
    @DisplayName("Cannot teleport pet into protected region")
    void cannotTeleportIntoProtectedRegion() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.playerStorageLocation);
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);

        when(this.protectedRegionManager.canTpThere(any(Player.class), any(Location.class))).thenReturn(false);

        String[] args = {"store", "PetName", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.CANT_TELEPORT_IN_PR);

        verify(this.sqlWrapper, times(1)).getStorageLocation("MockPlayerId", "StorageName");
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, never()).sendMessage(anyString());
    }


    @Test
    @DisplayName("Denies teleporting between worlds when config option set")
    void teleportingWithoutTPBetweenWorlds() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.playerStorageLocation);
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);

        // Putting player in different world
        when(this.world.getName()).thenReturn("RandomName");

        String[] args = {"store", "PetName", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.TP_BETWEEN_WORLDS);

        verify(this.sqlWrapper, times(1)).getStorageLocation("MockPlayerId", "StorageName");
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't teleport pet between worlds. Your pet is in " + ChatColor.WHITE + this.pet.petWorld, message);
    }


    @Test
    @DisplayName("Allows teleporting between worlds when config option set")
    void teleportingWithTPBetweenWorlds() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.playerStorageLocation);
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);

            // Putting player in different world
            when(this.world.getName()).thenReturn("RandomName");
            when(this.tpPets.getAllowTpBetweenWorlds()).thenReturn(true);

            String[] args = {"store", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getStorageLocation(anyString(), anyString());

            verify(this.chunk, times(1)).load();

            verify(this.horse).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getBlockX(), 0.5);
            assertEquals(200, capturedPetLocation.getBlockY(), 0.5);
            assertEquals(300, capturedPetLocation.getBlockZ(), 0.5);

            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockPlayerName - store - stored MockPlayerName's PetName at StorageName", capturedLogOutput);

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + " has been stored successfully", message);
        }
    }


    @Test
    @DisplayName("Allows teleporting between worlds when player has tppets.tpanywhere")
    void teleportingWithoutTPBetweenWorldsPlayerWithPermission() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.sqlWrapper.getStorageLocation("MockAdminId", "StorageName")).thenReturn(this.playerStorageLocation);
            when(this.sqlWrapper.getSpecificPet("MockAdminId", "PetName")).thenReturn(this.pet);

            // Putting player in different world
            when(this.world.getName()).thenReturn("RandomName");
            when(this.tpPets.getAllowTpBetweenWorlds()).thenReturn(true);

            String[] args = {"store", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getStorageLocation(anyString(), anyString());

            verify(this.chunk, times(1)).load();

            verify(this.horse).teleport(this.teleportCaptor.capture());
            Location capturedPetLocation = this.teleportCaptor.getValue();
            assertEquals(100, capturedPetLocation.getBlockX(), 0.5);
            assertEquals(200, capturedPetLocation.getBlockY(), 0.5);
            assertEquals(300, capturedPetLocation.getBlockZ(), 0.5);

            verify(this.logWrapper).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockAdminName - store - stored MockAdminName's PetName at StorageName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + " has been stored successfully", message);
        }
    }


    @Test
    @DisplayName("Cannot teleport user's pets with a non-player sender")
    void cannotTeleportUsersPetsNotPlayer() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.store")).thenReturn(true);

        String[] args = {"store", "PetName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verifyLoggedUnsuccessfulAction("Unknown Sender", CommandStatus.INVALID_SENDER);

        verify(this.sqlWrapper, never()).getServerStorageLocation(anyString(), any(World.class));
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(sender, never()).sendMessage(anyString());
    }


    @Test
    @DisplayName("Cannot teleport user's pets without pet name")
    void cannotTeleportUsersPetsNoPetName() throws SQLException {
        String[] args = {"store"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.SYNTAX_ERROR);

        verify(this.sqlWrapper, never()).getServerStorageLocation(anyString(), any(World.class));
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp store [pet name] [storage name]", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets without valid pet name")
    void cannotTeleportUsersPetsInvalidPetName() throws SQLException {
        String[] args = {"store", "MyPet;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_PET);

        verify(this.sqlWrapper, never()).getServerStorageLocation(anyString(), any(World.class));
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  "MyPet;", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets with an invalid storage name")
    void cannotTeleportUsersPetsInvalidStorageName() throws SQLException {
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);
        String[] args = {"store", "PetName", "MyStorage;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.INVALID_NAME);

        verify(this.sqlWrapper, never()).getStorageLocation(anyString(), anyString());
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find location: " + ChatColor.WHITE + "MyStorage;", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets when database fails to find storage")
    void cannotTeleportUsersPetsDbFailFindStorage() throws SQLException {
        String[] args = {"store", "PetName", "MyStorage"};
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "MyStorage")).thenThrow(new SQLException());
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(1)).getStorageLocation("MockPlayerId", "MyStorage");
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not process request", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets when database fails to find server storage")
    void cannotTeleportUsersPetsDbFailFindDefaultStorage() throws SQLException {
        String[] args = {"store", "PetName"};
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenThrow(new SQLException());
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, never()).getStorageLocation(anyString(), anyString());
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not process request", message);
    }

    @Test
    @DisplayName("Cannot teleport user's pets when database fails to find pet")
    void cannotTeleportUsersPetsDbFailFindPet() throws SQLException {
        String[] args = {"store", "MyPet"};
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "MyPet")).thenThrow(new SQLException());
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(this.serverStorageLocation);

        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, never()).getStorageLocation(anyString(), anyString());
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not process request", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets with non-existent storage")
    void cannotTeleportUsersPetsNonExistentStorage() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);

        String[] args = {"store", "PetName", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.INVALID_NAME);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find location: " + ChatColor.WHITE + "StorageName", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets with non-existent default storage")
    void cannotTeleportUsersPetsNonExistentDefaultStorage() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);

        String[] args = {"store", "PetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.INVALID_NAME);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find location: " + ChatColor.WHITE + "default storage", message);
    }

    @Test
    @DisplayName("Cannot teleport user's pets with non-existent pet")
    void cannotTeleportUsersPetsNonExistentPet() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.playerStorageLocation);
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(null);

        String[] args = {"store", "PetName", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_PET);

        verify(this.sqlWrapper, never()).getStorageLocation(anyString(), anyString());

        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE + "PetName", message);    }


    @Test
    @DisplayName("Reports generic inability to teleport pet to user")
    void cannotTeleportReportsError() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.playerStorageLocation);
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);
            when(this.server.getEntity(UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"))).thenReturn(null);

            String[] args = {"store", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.CANT_TELEPORT);

            verify(this.sqlWrapper, times(1)).getStorageLocation(anyString(), anyString());

            verify(this.chunk, times(1)).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not store your pet", message);
        }
    }

    @Test
    @DisplayName("Reports generic inability to teleport pet to admin")
    void cannotAdminTeleportReportsError() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.playerStorageLocation);
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(this.pet);
            when(this.server.getEntity(UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"))).thenReturn(null);

            String[] args = {"store", "f:MockPlayerName", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.CANT_TELEPORT);

            verify(this.sqlWrapper, times(1)).getStorageLocation(anyString(), anyString());

            verify(this.chunk, times(1)).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not store " + ChatColor.WHITE + "MockPlayerName's "  + ChatColor.RED + "pet", message);        }
    }
}