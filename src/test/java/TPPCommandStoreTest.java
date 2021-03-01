import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TPPCommandStoreTest {
    World world;
    Player player;
    Player admin;
    ArgumentCaptor<String> messageCaptor;
    ArgumentCaptor<String> logCaptor;
    ArgumentCaptor<Location> teleportCaptor;
    DBWrapper dbWrapper;
    Command command;
    CommandTPP commandTPP;
    StorageLocation storageLocation;
    LogWrapper logWrapper;
    PetStorage pet;
    Chunk chunk;
    Horse horse;

    @BeforeEach
    public void beforeEach() {
        // Players
        this.world = mock(World.class);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", this.world, null, new String[]{"tppets.store"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, null, new String[]{"tppets.store", "tppets.teleportother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.teleportCaptor = ArgumentCaptor.forClass(Location.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.horse = (Horse) MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Horse.class);

        // Plugin
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);

        // Command
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("store");
        aliases.put("store", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        // Storage Location
        this.storageLocation = MockFactory.getStorageLocation("StorageOne", 100, 200, 300, this.world);
        this.pet = new PetStorage("MockPetId", 7, 700, 800, 900, "MockWorld", "MockPlayerId", "MockPetName", "MockPetName");
        this.chunk = mock(Chunk.class);
        when(this.world.getChunkAt(700, 900)).thenReturn(this.chunk);
    }

    @Test
    @DisplayName("Teleports user's pets to default storage")
    void teleportsUsersPetsToDefaultStorage() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.world.getEntitiesByClasses(org.bukkit.entity.Tameable.class)).thenReturn(Collections.singletonList(this.horse));

            when(this.dbWrapper.getDefaultServerStorageLocation(this.world)).thenReturn(this.storageLocation);
            when(this.dbWrapper.getPetByName("MockPlayerId", "PetName")).thenReturn(Collections.singletonList(this.pet));

            String[] args = {"store", "PetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.dbWrapper, times(1)).getDefaultServerStorageLocation(any(World.class));

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
    void adminTeleportsUsersPetsToDefaultStorage() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.world.getEntitiesByClasses(org.bukkit.entity.Tameable.class)).thenReturn(Collections.singletonList(this.horse));

            when(this.dbWrapper.getDefaultServerStorageLocation(this.world)).thenReturn(this.storageLocation);
            when(this.dbWrapper.getPetByName("MockPlayerId", "PetName")).thenReturn(Collections.singletonList(this.pet));

            String[] args = {"store", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, times(1)).getDefaultServerStorageLocation(any(World.class));

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
    void teleportsUsersPetsToSpecificStorage() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.world.getEntitiesByClasses(org.bukkit.entity.Tameable.class)).thenReturn(Collections.singletonList(this.horse));

            when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
            when(this.dbWrapper.getPetByName("MockPlayerId", "PetName")).thenReturn(Collections.singletonList(this.pet));

            String[] args = {"store", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.dbWrapper, times(1)).getStorageLocation(anyString(), anyString());

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
    void adminTeleportsUsersPetsToSpecificStorage() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.world.getEntitiesByClasses(org.bukkit.entity.Tameable.class)).thenReturn(Collections.singletonList(this.horse));

            when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
            when(this.dbWrapper.getPetByName("MockPlayerId", "PetName")).thenReturn(Collections.singletonList(this.pet));

            String[] args = {"store", "f:MockPlayerName", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, times(1)).getStorageLocation(anyString(), anyString());

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
    void cannotAdminTeleportUsersPetsNotPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("PlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.store")).thenReturn(true);

            String[] args = {"store", "f:PlayerName", "PetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            verify(this.dbWrapper, never()).getDefaultServerStorageLocation(any(World.class));
            verify(this.chunk, never()).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());
            verify(sender, never()).sendMessage(anyString());
        }
    }


    @Test
    @DisplayName("Admin cannot teleport user's pets without permission")
    void cannotAdminTeleportUsersPetsNoPermission() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.admin.hasPermission("tppets.teleportother")).thenReturn(false);

            String[] args = {"store", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).getDefaultServerStorageLocation(any(World.class));
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
    void cannotAdminTeleportUsersPetsNoPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.hasPlayedBefore()).thenReturn(false);

            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"store", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).getDefaultServerStorageLocation(any(World.class));
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
    void cannotAdminTeleportUsersPetsNoPetName() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"store", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).getDefaultServerStorageLocation(any(World.class));
            verify(this.chunk, never()).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp store [pet name] [storage name]", message);
        }
    }


    @Test
    @DisplayName("Cannot teleport user's pets with a non-player sender")
    void cannotTeleportUsersPetsNotPlayer() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.store")).thenReturn(true);

        String[] args = {"store", "PetName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verify(this.dbWrapper, never()).getDefaultServerStorageLocation(any(World.class));
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(sender, never()).sendMessage(anyString());
    }


    @Test
    @DisplayName("Cannot teleport user's pets without pet name")
    void cannotTeleportUsersPetsNoPetName() {
        String[] args = {"store"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.dbWrapper, never()).getDefaultServerStorageLocation(any(World.class));
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp store [pet name] [storage name]", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets without valid pet name")
    void cannotTeleportUsersPetsInvalidPetName() {
        String[] args = {"store", "MyPet;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.dbWrapper, never()).getDefaultServerStorageLocation(any(World.class));
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  "MyPet;", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets with an invalid storage name")
    void cannotTeleportUsersPetsInvalidStorageName() {
        String[] args = {"store", "MyPet", "MyStorage;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.dbWrapper, never()).getStorageLocation(anyString(), anyString());
        verify(this.chunk, never()).load();
        verify(this.horse, never()).teleport(any(Location.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String message = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find " + ChatColor.WHITE + "MyStorage;", message);
    }


    @Test
    @DisplayName("Cannot teleport user's pets with non-existent storage")
    void cannotTeleportUsersPetsNonExistentStorage() {
        when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(null);

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
    void cannotTeleportUsersPetsNonExistentDefaultStorage() {
        when(this.dbWrapper.getDefaultServerStorageLocation(this.world)).thenReturn(null);

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
    @DisplayName("Reports generic inability to teleport pet to user")
    void cannotTeleportReportsError() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.world.getEntitiesByClasses(org.bukkit.entity.Tameable.class)).thenReturn(Collections.singletonList(this.horse));

            when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
            when(this.dbWrapper.getPetByName("MockPlayerId", "PetName")).thenReturn(null);

            String[] args = {"store", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.dbWrapper, times(1)).getStorageLocation(anyString(), anyString());

            verify(this.chunk, never()).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not store your pet", message);
        }
    }

    @Test
    @DisplayName("Reports generic inability to teleport pet to admin")
    void cannotAdminTeleportReportsError() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            when(this.world.getEntitiesByClasses(org.bukkit.entity.Tameable.class)).thenReturn(Collections.singletonList(this.horse));

            when(this.dbWrapper.getStorageLocation("MockPlayerId", "StorageName")).thenReturn(this.storageLocation);
            when(this.dbWrapper.getPetByName("MockPlayerId", "PetName")).thenReturn(null);

            String[] args = {"store", "f:MockPlayerName", "PetName", "StorageName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, times(1)).getStorageLocation(anyString(), anyString());

            verify(this.chunk, never()).load();
            verify(this.horse, never()).teleport(any(Location.class));
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String message = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Could not store " + ChatColor.WHITE + "MockPlayerName's "  + ChatColor.RED + "pet", message);        }
    }
}