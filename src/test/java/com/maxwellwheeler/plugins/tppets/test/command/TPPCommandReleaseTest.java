package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.mockito.Mockito.*;

public class TPPCommandReleaseTest {
    private Player player;
    private Player admin;
    private SQLWrapper sqlWrapper;
    private Command command;
    private CommandTPP commandTPP;
    private TPPets tpPets;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.dogs"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.dogs", "tppets.releaseother"});
        this.sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, logWrapper, false, true);
        when(this.tpPets.getAllowUntamingPets()).thenReturn(true);

        PetStorage pet = new PetStorage("MockPetId", 7, 100, 200, 300, "MockWorldName", "MockPlayerId", "PetName", "PetName");
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(pet);
        when(this.sqlWrapper.removePet("MockPetId")).thenReturn(true);

        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("release");
        aliases.put("release", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
    }

    @Test
    @DisplayName("Releases a pet")
    void releasePet() throws SQLException {
        String[] args = {"release", "PetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
        verify(this.sqlWrapper, times(1)).removePet("MockPetId");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "PetName" + ChatColor.BLUE + " has been released");
    }

    @Test
    @DisplayName("Cannot release pet not a player")
    void cannotReleasePetNotPlayer() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.dogs")).thenReturn(true);

        String[] args = {"release", "PetName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Cannot release pet no pet specified")
    void cannotReleaseNoPet() throws SQLException {
        String[] args = {"release"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

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

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "You can't release pets");
    }

    @Test
    @DisplayName("Can release pet when not enabled if player has tppets.releaseother")
    void overridesConfigWithPermission() throws SQLException {
        when(this.tpPets.getAllowUntamingPets()).thenReturn(false);
        when(this.player.hasPermission("tppets.releaseother")).thenReturn(true);

        String[] args = {"release", "PetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
        verify(this.sqlWrapper, times(1)).removePet("MockPetId");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "PetName" + ChatColor.BLUE + " has been released");
    }

    @Test
    @DisplayName("Cannot release pet invalid pet name specified")
    void cannotReleaseInvalidName() throws SQLException {
        String[] args = {"release", "PetName;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

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

        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not release pet");
    }

    @Test
    @DisplayName("Cannot release pet db can't remove pet")
    void cannotReleaseDbNoRemovePet() throws SQLException {
        when(this.sqlWrapper.removePet("MockPetId")).thenReturn(false);

        String[] args = {"release", "PetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
        verify(this.sqlWrapper, times(1)).removePet("MockPetId");
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not release pet");
    }

    @Test
    @DisplayName("Cannot release pet db can't remove pet")
    void cannotReleaseDbFailRemovePet() throws SQLException {
        when(this.sqlWrapper.removePet("MockPetId")).thenThrow(new SQLException());

        String[] args = {"release", "PetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
        verify(this.sqlWrapper, times(1)).removePet("MockPetId");
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not release pet");
    }

    @Test
    @DisplayName("Admin releases a pet")
    void adminReleasePet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() ->Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"release", "f:MockPlayerName", "PetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, times(1)).removePet("MockPetId");
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

            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).removePet(anyString());
            verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName");
        }
    }
}
