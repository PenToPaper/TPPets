package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.RegionSelectionManager;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
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

public class TPPCommandClearTest {
    private Player admin;
    private ArgumentCaptor<String> stringCaptor;
    private Command command;
    private CommandTPP commandTPP;
    private RegionSelectionManager regionSelectionManager;

    @BeforeEach
    public void beforeEach() {
        World world = mock(World.class);
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", world, new Location(world, 100, 200, 300), new String[]{"tppets.lost"});
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("clear");
        aliases.put("clear", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        this.regionSelectionManager = new RegionSelectionManager();

        when(tpPets.getRegionSelectionManager()).thenReturn(this.regionSelectionManager);
    }

    @Test
    @DisplayName("Clears position1")
    void clearsPosition1() {
        this.regionSelectionManager.setStartLocation(this.admin, mock(Location.class));
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.admin));

        String[] args = {"clear"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        assertNull(this.regionSelectionManager.getSelectionSession(this.admin));

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.BLUE + "Selection cleared.", capturedMessage);
    }

    @Test
    @DisplayName("Clears position2")
    void clearsPosition2() {
        this.regionSelectionManager.setEndLocation(this.admin, mock(Location.class));
        assertNotNull(this.regionSelectionManager.getSelectionSession(this.admin));

        String[] args = {"clear"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        assertNull(this.regionSelectionManager.getSelectionSession(this.admin));

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.BLUE + "Selection cleared.", capturedMessage);
    }

    @Test
    @DisplayName("Does not clear when sent by non-player")
    void doesNotClearNotAPlayer() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.lost")).thenReturn(true);

        this.regionSelectionManager = mock(RegionSelectionManager.class);

        String[] args = {"clear"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verify(this.regionSelectionManager, never()).clearPlayerSession(any(Player.class));
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Does not clear when using f:[username] syntax when user hasn't played before")
    void cannotClearNoPlayer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            OfflinePlayer player = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
            when(player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(player);

            this.regionSelectionManager.setEndLocation(this.admin, mock(Location.class));
            assertNotNull(this.regionSelectionManager.getSelectionSession(this.admin));

            String[] args = {"clear", "f:MockPlayerName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            assertNotNull(this.regionSelectionManager.getSelectionSession(this.admin));

            verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
            String capturedMessage = this.stringCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessage);
        }
    }
}
