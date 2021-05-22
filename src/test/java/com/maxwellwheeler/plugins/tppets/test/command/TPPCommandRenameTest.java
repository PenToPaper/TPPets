package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
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
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPCommandRenameTest {
    private Player player;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.rename"});
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.rename", "tppets.renameother"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);
        PetStorage pet = new PetStorage("MockPetId", 7, 100, 200, 300, "MockWorldName", "MockPlayerId", "OldPetName", "OldPetName");
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("rename");
        aliases.put("rename", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "OldPetName")).thenReturn(pet);
        when(this.sqlWrapper.renamePet("MockPlayerId", "OldPetName", "NewPetName")).thenReturn(true);
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "NewPetName")).thenReturn(null);
    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - rename - " + commandStatus.toString(), logCaptor.getValue());
    }

    @Test
    @DisplayName("Renames a pet")
    void renamePet() throws SQLException {
        String[] args = {"rename", "OldPetName", "NewPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verify(this.sqlWrapper, times(2)).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "OldPetName");
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "NewPetName");
        verify(this.sqlWrapper, times(1)).renamePet(anyString(), anyString(), anyString());

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("MockPlayerName - rename - renamed MockPlayerName's OldPetName to NewPetName", capturedLogOutput);

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "OldPetName" + ChatColor.BLUE + " has been renamed to " + ChatColor.WHITE + "NewPetName", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot rename pet not a player")
    void cannotRenamePetNotPlayer() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.rename")).thenReturn(true);

        String[] args = {"rename", "OldPetName", "NewPetName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verifyLoggedUnsuccessfulAction("Unknown Sender", CommandStatus.INVALID_SENDER);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());
        verify(this.player, never()).sendMessage(this.messageCaptor.capture());
    }

    @Test
    @DisplayName("Cannot rename pet no new pet name specified")
    void cannotRenamePetNoNewName() throws SQLException {
        String[] args = {"rename", "OldPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.SYNTAX_ERROR);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp rename [old name] [new name]", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot rename pet no old pet name specified")
    void cannotRenamePetNoOldName() throws SQLException {
        String[] args = {"rename"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.SYNTAX_ERROR);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp rename [old name] [new name]", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot rename pet invalid old pet name")
    void cannotRenamePetInvalidOldName() throws SQLException {
        String[] args = {"rename", "OldPetName;", "NewPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_PET);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find pet named " + ChatColor.WHITE + "OldPetName;", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot rename pet invalid new pet name")
    void cannotRenamePetInvalidNewName() throws SQLException {
        String[] args = {"rename", "OldPetName", "NewPetName;"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.INVALID_NAME);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Invalid pet name: " + ChatColor.WHITE + "NewPetName;", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot rename pet db fails to get pet")
    void cannotRenamePetDbFailGet() throws SQLException {
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "OldPetName")).thenThrow(new SQLException());

        String[] args = {"rename", "OldPetName", "NewPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(1)).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not rename pet", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot rename pet db fails to find pet")
    void cannotRenamePetDbNoPet() throws SQLException {
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "OldPetName")).thenReturn(null);

        String[] args = {"rename", "OldPetName", "NewPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.NO_PET);

        verify(this.sqlWrapper, times(1)).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not find pet named " + ChatColor.WHITE + "OldPetName", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot rename pet if pet name already exists")
    void cannotRenamePetNameNotUnique() throws SQLException {
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "NewPetName")).thenReturn(new PetStorage("PetId", 7, 1, 2, 3, "PetWorld", "OwnerId", "PetName", "PetName"));

        String[] args = {"rename", "OldPetName", "NewPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.ALREADY_DONE);

        verify(this.sqlWrapper, times(2)).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "OldPetName");
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "NewPetName");
        verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Pet name " + ChatColor.WHITE + "NewPetName" + ChatColor.RED + " is already in use", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot rename pet if db fails to find if name is unique")
    void cannotRenameDbFailsDeterminingUnique() throws SQLException {
        when(this.sqlWrapper.getSpecificPet("MockPlayerId", "NewPetName")).thenThrow(new SQLException());

        String[] args = {"rename", "OldPetName", "NewPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(2)).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "OldPetName");
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "NewPetName");
        verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not rename pet", capturedMessageOutput);
    }

    @Test
    @DisplayName("Cannot rename pet db fails to rename pet")
    void cannotRenamePetDbFailRename() throws SQLException {
        when(this.sqlWrapper.renamePet("MockPlayerId", "OldPetName", "NewPetName")).thenReturn(false);

        String[] args = {"rename", "OldPetName", "NewPetName"};
        this.commandTPP.onCommand(this.player, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockPlayerName", CommandStatus.DB_FAIL);

        verify(this.sqlWrapper, times(2)).getSpecificPet(anyString(), anyString());
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "OldPetName");
        verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "NewPetName");
        verify(this.sqlWrapper, times(1)).renamePet(anyString(), anyString(), anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

        verify(this.player, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessageOutput = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not rename pet", capturedMessageOutput);
    }

    @Test
    @DisplayName("Admin renames a pet")
    void adminRenamePet() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"rename", "f:MockPlayerName", "OldPetName", "NewPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);


            verify(this.sqlWrapper, times(2)).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "OldPetName");
            verify(this.sqlWrapper, times(1)).getSpecificPet("MockPlayerId", "NewPetName");
            verify(this.sqlWrapper, times(1)).renamePet(anyString(), anyString(), anyString());

            verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
            String capturedLogOutput = this.logCaptor.getValue();
            assertEquals("MockAdminName - rename - renamed MockPlayerName's OldPetName to NewPetName", capturedLogOutput);

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.WHITE + "MockPlayerName's" + ChatColor.BLUE + " pet " + ChatColor.WHITE + "OldPetName" + ChatColor.BLUE + " has been renamed to " + ChatColor.WHITE + "NewPetName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin cannot rename pet if f:[username] hasn't played before")
    void cannotAdminRenamePetUserNotPlayedBefore() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"rename", "f:MockPlayerName", "OldPetName", "NewPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin cannot rename pet if admin is not a player")
    void cannotAdminRenamePetNotUser() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            CommandSender sender = mock(CommandSender.class);
            when(sender.hasPermission("tppets.renameother")).thenReturn(true);
            when(sender.hasPermission("tppets.rename")).thenReturn(true);

            String[] args = {"rename", "f:MockPlayerName", "OldPetName", "NewPetName"};
            this.commandTPP.onCommand(sender, this.command, "", args);

            verifyLoggedUnsuccessfulAction("Unknown Sender", CommandStatus.INVALID_SENDER);

            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());
            verify(sender, never()).sendMessage(this.messageCaptor.capture());
        }
    }

    @Test
    @DisplayName("Admin cannot rename pet if admin has insufficient permissions")
    void cannotAdminRenameInsufficientPermissions() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            when(this.admin.hasPermission("tppets.renameother")).thenReturn(false);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"rename", "f:MockPlayerName", "OldPetName", "NewPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.INSUFFICIENT_PERMISSIONS);

            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "You don't have permission to do that", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin cannot rename pet if no new name specified")
    void cannotAdminRenameNoNewName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"rename", "f:MockPlayerName", "OldPetName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp rename [old name] [new name]", capturedMessageOutput);
        }
    }

    @Test
    @DisplayName("Admin cannot rename pet if no old name specified")
    void cannotAdminRenameNoOldName() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(this.player);

            String[] args = {"rename", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

            verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
            verify(this.sqlWrapper, never()).renamePet(anyString(), anyString(), anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(this.logCaptor.capture());

            verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
            String capturedMessageOutput = this.messageCaptor.getValue();
            assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp rename [old name] [new name]", capturedMessageOutput);
        }
    }
}