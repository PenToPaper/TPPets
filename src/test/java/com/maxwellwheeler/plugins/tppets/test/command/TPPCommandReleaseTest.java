package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TPPCommandReleaseTest {
    private Player player;
    private Player admin;
    private World world;
    private Chunk chunk;
    private Horse horse;
    private SQLWrapper sqlWrapper;
    private Command command;
    private CommandTPP commandTPP;
    private TPPets tpPets;
    private Server server;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.dogs"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.dogs", "tppets.releaseother"});
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.horse = MockFactory.getMockEntity("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", Horse.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);
        when(this.tpPets.getAllowUntamingPets()).thenReturn(true);

        PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 700, 800, 900, "MockWorld", "MockPlayerId", "MockPetName", "MockPetName");
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(pet);
        when(this.sqlWrapper.removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")).thenReturn(true);

        this.world = mock(World.class);
        when(this.world.getName()).thenReturn("MockWorld");
        this.chunk = mock(Chunk.class);
        when(this.world.getChunkAt(43, 56)).thenReturn(this.chunk);

        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("release");
        aliases.put("release", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, this.tpPets);

        this.server = mock(Server.class);
        when(this.tpPets.getServer()).thenReturn(this.server);
        when(this.server.getEntity(UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"))).thenReturn(this.horse);
    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - release - " + commandStatus.toString(), logCaptor.getValue());
    }

    @Test
    @DisplayName("Releases a pet")
    void releasePet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            String[] args = {"release", "PetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verify(this.chunk, times(1)).load();
            verify(this.horse, times(1)).setOwner(null);
            verify(this.horse, times(1)).setTamed(false);
            verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - release - released MockPlayerName's PetName");
            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, times(1)).removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "PetName" + ChatColor.BLUE + " has been released");
        }
    }

    @Test
    @DisplayName("Cannot release pet not a player")
    void cannotReleasePetNotPlayer() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.dogs")).thenReturn(true);

        String[] args = {"release", "PetName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verifyLoggedUnsuccessfulAction("Unknown Sender", CommandStatus.INVALID_SENDER);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Cannot release pet no pet specified")
    void cannotReleaseNoPet() throws SQLException {
        String[] args = {"release"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.SYNTAX_ERROR);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp release [pet name]");
    }

    @Test
    @DisplayName("Cannot release pet not enabled")
    void cannotReleaseNotEnabled() throws SQLException {
        when(this.tpPets.getAllowUntamingPets()).thenReturn(false);

        String[] args = {"release", "PetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NOT_ENABLED);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "You can't release pets");
    }

    @Test
    @DisplayName("Can release pet when not enabled if player has tppets.releaseother")
    void overridesConfigWithPermission() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.tpPets.getAllowUntamingPets()).thenReturn(false);
            when(this.player.hasPermission("tppets.releaseother")).thenReturn(true);

            String[] args = {"release", "PetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);


            verify(this.chunk, times(1)).load();
            verify(this.horse, times(1)).setOwner(null);
            verify(this.horse, times(1)).setTamed(false);
            verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - release - released MockPlayerName's PetName");
            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, times(1)).removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "PetName" + ChatColor.BLUE + " has been released");
        }
    }

    @Test
    @DisplayName("Cannot release pet invalid pet name specified")
    void cannotReleaseInvalidName() throws SQLException {
        String[] args = {"release", "PetName;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_PET);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not find pet named " + ChatColor.WHITE + "PetName;");
    }

    @Test
    @DisplayName("Cannot release pet db can't find pet")
    void cannotReleaseDbNoPet() throws SQLException {
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(null);

        String[] args = {"release", "PetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_PET);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not find pet named " + ChatColor.WHITE + "PetName");
    }

    @Test
    @DisplayName("Cannot release pet db fails finding pet")
    void cannotReleaseDbFailFinding() throws SQLException {
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenThrow(new SQLException());

        String[] args = {"release", "PetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.chunk, never()).load();
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not release pet");
    }


    @Test
    @DisplayName("Cannot release pet server can't find pet")
    void cannotReleaseNoEntity() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.server.getEntity(UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"))).thenReturn(null);

            String[] args = {"release", "PetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_ENTITY);

            verify(this.chunk, times(1)).load();
            verify(this.horse, never()).setOwner(any(AnimalTamer.class));
            verify(this.horse, never()).setTamed(anyBoolean());
            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, never()).removePet(anyString());
            verify(this.player, times(1)).sendMessage(ChatColor.RED + "Can't find pet");
        }
    }

    @Test
    @DisplayName("Cannot release pet server can't find accurate pet")
    void cannotReleaseNoAccurateEntity() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            Villager villager = MockFactory.getMockEntity("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", org.bukkit.entity.Villager.class);
            when(this.server.getEntity(UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"))).thenReturn(villager);

            String[] args = {"release", "PetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_ENTITY);

            verify(this.chunk, times(1)).load();
            verify(this.horse, never()).setOwner(any(AnimalTamer.class));
            verify(this.horse, never()).setTamed(anyBoolean());
            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, never()).removePet(anyString());
            verify(this.player, times(1)).sendMessage(ChatColor.RED + "Can't find pet");
        }
    }

    @Test
    @DisplayName("Cannot release pet db can't remove pet")
    void cannotReleaseDbNoRemovePet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.sqlWrapper.removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")).thenReturn(false);

            String[] args = {"release", "PetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

            verify(this.chunk, times(1)).load();
            verify(this.horse, never()).setOwner(any(AnimalTamer.class));
            verify(this.horse, never()).setTamed(anyBoolean());
            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, times(1)).removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not release pet");
        }
    }

    @Test
    @DisplayName("Cannot release pet db can't remove pet")
    void cannotReleaseDbFailRemovePet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);

            when(this.sqlWrapper.removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")).thenThrow(new SQLException());

            String[] args = {"release", "PetName"};
            this.commandTPP.onCommand(this.player, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

            verify(this.chunk, times(1)).load();
            verify(this.horse, never()).setOwner(any(AnimalTamer.class));
            verify(this.horse, never()).setTamed(anyBoolean());
            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, times(1)).removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not release pet");
        }
    }

    @Test
    @DisplayName("Admin releases a pet")
    void adminReleasePet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(this.world);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"release", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.chunk, times(1)).load();
            verify(this.horse, times(1)).setOwner(null);
            verify(this.horse, times(1)).setTamed(false);
            verify(this.logWrapper, times(1)).logSuccessfulAction("MockAdminName - release - released MockPlayerName's PetName");
            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, times(1)).removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            verify(this.admin, times(1)).sendMessage(ChatColor.WHITE + "MockPlayerName's" + ChatColor.BLUE + " pet " + ChatColor.WHITE + "PetName" + ChatColor.BLUE + " has been released");
        }
    }

    @Test
    @DisplayName("Admin cannot release pet not a player")
    void adminCannotReleasePetNotPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.dogs")).thenReturn(true);
            when(sender.hasPermission("tppets.releaseother")).thenReturn(true);

            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"release", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            verifyLoggedUnsuccessfulAction("Unknown Sender", CommandStatus.INVALID_SENDER);

            verify(this.chunk, never()).load();
            verify(this.horse, never()).setOwner(any(AnimalTamer.class));
            verify(this.horse, never()).setTamed(anyBoolean());
            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).removePet(anyString());
            verify(sender, never()).sendMessage(anyString());
        }
    }

    @Test
    @DisplayName("Admin cannot release pet insufficient permissions")
    void adminCannotReleasePetInsufficientPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.admin.hasPermission("tppets.releaseother")).thenReturn(false);

            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"release", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.INSUFFICIENT_PERMISSIONS);

            verify(this.chunk, never()).load();
            verify(this.horse, never()).setOwner(any(AnimalTamer.class));
            verify(this.horse, never()).setTamed(anyBoolean());
            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).removePet(anyString());
            verify(this.admin, times(1)).sendMessage(ChatColor.RED + "You don't have permission to do that");
        }
    }

    @Test
    @DisplayName("Admin cannot release pet no pet specified")
    void adminCannotReleasePetNoPet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"release", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

            verify(this.chunk, never()).load();
            verify(this.horse, never()).setOwner(any(AnimalTamer.class));
            verify(this.horse, never()).setTamed(anyBoolean());
            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).removePet(anyString());
            verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp release [pet name]");
        }
    }

    @Test
    @DisplayName("Admin cannot release pet cannot find player")
    void adminCannotReleasePetNoPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"release", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.chunk, never()).load();
            verify(this.horse, never()).setOwner(any(AnimalTamer.class));
            verify(this.horse, never()).setTamed(anyBoolean());
            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).removePet(anyString());
            verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName");
        }
    }
}
