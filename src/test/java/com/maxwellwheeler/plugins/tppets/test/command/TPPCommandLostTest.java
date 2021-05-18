package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandStatus;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostRegionManager;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegionManager;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
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

public class TPPCommandLostTest {
    private Player admin;
    private ArgumentCaptor<String> stringCaptor;
    private ArgumentCaptor<String> logCaptor;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private LostRegionManager lostRegionManager;
    private ProtectedRegionManager protectedRegionManager;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach() {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.lost"});
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("lost");
        aliases.put("lost", altAlias);
        this.lostRegionManager = mock(LostRegionManager.class);
        this.protectedRegionManager = mock(ProtectedRegionManager.class);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        when(tpPets.getLostRegionManager()).thenReturn(this.lostRegionManager);
        when(tpPets.getProtectedRegionManager()).thenReturn(this.protectedRegionManager);
    }

    public void verifyLoggedUnsuccessfulAction(String expectedPlayerName, CommandStatus commandStatus) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logUnsuccessfulAction(logCaptor.capture());
        assertEquals(expectedPlayerName + " - lost - " + commandStatus.toString(), logCaptor.getValue());
    }

    @Test
    @DisplayName("Can't run lost and found region command without a player sender")
    void cantRunLostAndFoundNotPlayer() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.lost")).thenReturn(true);

        String[] args = {"lost", "remove", "LostRegionName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verifyLoggedUnsuccessfulAction("Unknown Sender", CommandStatus.INVALID_SENDER);

        verify(this.sqlWrapper, never()).removeLostRegion(anyString());
        verify(this.sqlWrapper, never()).getLostRegion(anyString());
        verify(this.protectedRegionManager, never()).updateLFReferences(anyString());
        verify(this.lostRegionManager, never()).removeLostRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Can't run lost and found region command without command type")
    void cantRunLostAndFoundNoCommandType() throws SQLException {
        String[] args = {"lost"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

        verify(this.sqlWrapper, never()).removeLostRegion(anyString());
        verify(this.sqlWrapper, never()).getLostRegion(anyString());
        verify(this.protectedRegionManager, never()).updateLFReferences(anyString());
        verify(this.lostRegionManager, never()).removeLostRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp lost [add/remove/list]", capturedMessage);
    }

    @Test
    @DisplayName("Can't run lost and found region command without valid command type")
    void cantRunLostAndFoundInvalidCommandType() throws SQLException {
        String[] args = {"lost", "invalidtype"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.SYNTAX_ERROR);

        verify(this.sqlWrapper, never()).removeLostRegion(anyString());
        verify(this.sqlWrapper, never()).getLostRegion(anyString());
        verify(this.protectedRegionManager, never()).updateLFReferences(anyString());
        verify(this.lostRegionManager, never()).removeLostRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp lost [add/remove/list]", capturedMessage);
    }

    @Test
    @DisplayName("Can't run lost and found region command with f:[username] who hasn't played")
    void cantRunLostAndFoundNoPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            OfflinePlayer player = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
            when(player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(player);

            String[] args = {"lost", "f:MockPlayerName", "remove", "LostRegionName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

            verify(this.sqlWrapper, never()).removeLostRegion(anyString());
            verify(this.sqlWrapper, never()).getLostRegion(anyString());
            verify(this.protectedRegionManager, never()).updateLFReferences(anyString());
            verify(this.lostRegionManager, never()).removeLostRegion(anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
            String capturedMessage = this.stringCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessage);
        }
    }

    @Test
    @DisplayName("Can't run lost and found region command with invalid f:[username]")
    void cantRunLostAndFoundInvalidPlayer() throws SQLException {
        String[] args = {"lost", "f:MockPlayerName;", "remove", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verifyLoggedUnsuccessfulAction("MockAdminName", CommandStatus.NO_PLAYER);

        verify(this.sqlWrapper, never()).removeLostRegion(anyString());
        verify(this.sqlWrapper, never()).getLostRegion(anyString());
        verify(this.protectedRegionManager, never()).updateLFReferences(anyString());
        verify(this.lostRegionManager, never()).removeLostRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName;", capturedMessage);
    }
}
