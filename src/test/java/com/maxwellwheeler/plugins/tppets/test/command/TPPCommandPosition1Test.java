package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.RegionSelectionManager;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

public class TPPCommandPosition1Test {
    private Player admin;
    private ArgumentCaptor<String> stringCaptor;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private RegionSelectionManager regionSelectionManager;
    private World world;

    @BeforeEach
    public void beforeEach() {
        this.world = mock(World.class);
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", this.world, new Location(this.world, 100, 200, 300), new String[]{"tppets.lost"});
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("position1");
        aliases.put("position1", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        this.regionSelectionManager = new RegionSelectionManager();

        when(this.tpPets.getRegionSelectionManager()).thenReturn(this.regionSelectionManager);
    }

    @Test
    @DisplayName("Position 1 is settable as first position through the com.maxwellwheeler.plugins.tppets.test.command")
    void position1FirstPosition() {
        String[] args = {"position1"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        assertNotNull(this.regionSelectionManager.getSelectionSession(this.admin));

        this.regionSelectionManager.setEndLocation(this.admin, new Location(this.world, 400, 500, 600));

        assertTrue(this.regionSelectionManager.getSelectionSession(this.admin).isCompleteSelection());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.BLUE + "First position set!", capturedMessage);
    }

    @Test
    @DisplayName("Position 1 is settable as second position through the com.maxwellwheeler.plugins.tppets.test.command")
    void position1SecondPosition() {
        this.regionSelectionManager.setEndLocation(this.admin, new Location(this.world, 400, 500, 600));

        String[] args = {"position1"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        assertNotNull(this.regionSelectionManager.getSelectionSession(this.admin));
        assertTrue(this.regionSelectionManager.getSelectionSession(this.admin).isCompleteSelection());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.BLUE + "First position set! Selection is complete.", capturedMessage);
    }

    @Test
    @DisplayName("Position 1 is not settable when sent by non-player")
    void cannotSetPosition1NotAPlayer() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.lost")).thenReturn(true);

        this.regionSelectionManager = mock(RegionSelectionManager.class);

        String[] args = {"position1"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verify(this.regionSelectionManager, never()).setStartLocation(any(Player.class), any(Location.class));

        verify(sender, never()).sendMessage(this.stringCaptor.capture());
    }

    @Test
    @DisplayName("Position 1 is not settable when sent using f:[username] syntax to find user that hasn't played before")
    void cannotSetPosition1NoPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            OfflinePlayer player = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
            when(player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(player);

            String[] args = {"position1", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertNull(this.regionSelectionManager.getSelectionSession(this.admin));

            verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
            String capturedMessage = this.stringCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessage);
        }
    }
}
