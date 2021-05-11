package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation;
import com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
    private LogWrapper logWrapper;
    private PetStorage pet;
    private Chunk chunk;
    private Horse horse;
    private TPPets tpPets;

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
        this.horse = MockFactory.getMockEntity("MockPetId", Horse.class);

        // Plugin
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, true, false, true);

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
        this.pet = new PetStorage("MockPetId", 7, 700, 800, 900, "MockWorld", "MockPlayerId", "MockPetName", "MockPetName");
        this.chunk = mock(Chunk.class);
        when(this.world.getChunkAt(700, 900)).thenReturn(this.chunk);
        when(this.chunk.getEntities()).thenReturn(new Entity[]{this.horse});
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
            assertEquals("Player MockPlayerName teleported their pet PetName to storage location at: x: 100, y: 200, z: 300", capturedLogOutput);

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet has been stored successfully", message);
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
            assertEquals("Player MockAdminName teleported MockPlayerName's pet PetName to storage location at: x: 100, y: 200, z: 300", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's" + ChatColor.BLUE + " pet has been stored successfully", message);
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
            assertEquals("Player MockPlayerName teleported their pet PetName to storage location at: x: 100, y: 200, z: 300", capturedLogOutput);

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet has been stored successfully", message);
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
            assertEquals("Player MockAdminName teleported MockPlayerName's pet PetName to storage location at: x: 100, y: 200, z: 300", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's" + ChatColor.BLUE + " pet has been stored successfully", message);
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

        when(this.tpPets.canTpThere(any(Player.class), any(Location.class))).thenReturn(false);

        String[] args = {"store", "PetName", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

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
            assertEquals("Player MockPlayerName teleported their pet PetName to storage location at: x: 100, y: 200, z: 300", capturedLogOutput);

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet has been stored successfully", message);
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
            assertEquals("Player MockAdminName teleported their pet PetName to storage location at: x: 100, y: 200, z: 300", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet has been stored successfully", message);
        }
    }


    @Test
    @DisplayName("Cannot teleport user's pets with a non-player sender")
    void cannotTeleportUsersPetsNotPlayer() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.store")).thenReturn(true);

        String[] args = {"store", "PetName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

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
        String[] args = {"store", "MyPet", "MyStorage;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, never()).getStorageLocation(anyString(), anyString());
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find " + ChatColor.WHITE + "MyStorage;", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets when database fails to find storage")
    void cannotTeleportUsersPetsDbFailFindStorage() throws SQLException {
        String[] args = {"store", "MyPet", "MyStorage"};
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "MyStorage")).thenThrow(new SQLException());
        this.commandTPP.onCommand(this.player, this.command, "", args);

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
        String[] args = {"store", "MyPet"};
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenThrow(new SQLException());
        this.commandTPP.onCommand(this.player, this.command, "", args);

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

        String[] args = {"store", "PetName", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find " + ChatColor.WHITE + "StorageName", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets with non-existent default storage")
    void cannotTeleportUsersPetsNonExistentDefaultStorage() throws SQLException {
        when(this.sqlWrapper.getServerStorageLocation("default", this.world)).thenReturn(null);

        String[] args = {"store", "PetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find default storage", message);
    }

    @Test
    @DisplayName("Cannot teleport user's pets with non-existent pet")
    void cannotTeleportUsersPetsNonExistentPet() throws SQLException {
        when(this.sqlWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.playerStorageLocation);
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(null);

        String[] args = {"store", "PetName", "StorageName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getStorageLocation(anyString(), anyString());

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
            when(this.chunk.getEntities()).thenReturn(new Entity[]{});

            String[] args = {"store", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

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
            when(this.chunk.getEntities()).thenReturn(new Entity[]{});

            String[] args = {"store", "f:MockPlayerName", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getStorageLocation(anyString(), anyString());

            verify(this.chunk, times(1)).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not store " + ChatColor.WHITE + "MockPlayerName's "  + ChatColor.RED + "pet", message);        }
    }
}