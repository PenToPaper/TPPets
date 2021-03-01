package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TPPCommandLostListTest {
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private Hashtable<String, LostAndFoundRegion> lostAndFoundRegions;
    private World world;

    @BeforeEach
    public void beforeEach() {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.lost"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("lost");
        aliases.put("lost", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        this.world = mock(World.class);

        this.lostAndFoundRegions = new Hashtable<>();
        this.lostAndFoundRegions.put("LostRegion1", MockFactory.getLostAndFoundRegion("LostRegion1", "MockWorldName", this.world, 100, 200, 300, 400, 500, 600));
        this.lostAndFoundRegions.put("LostRegion2", MockFactory.getLostAndFoundRegion("LostRegion2", "MockWorldName", this.world, 10, 20, 30, 40, 50, 60));
        this.lostAndFoundRegions.put("LostRegion3", MockFactory.getLostAndFoundRegion("LostRegion3", "MockWorldName", this.world, 1, 2, 3, 4, 5, 6));
        when(this.tpPets.getLostRegions()).thenReturn(this.lostAndFoundRegions);
        when(this.tpPets.getLostRegion("LostRegion1")).thenReturn(this.lostAndFoundRegions.get("LostRegion1"));
    }

    @Test
    @DisplayName("Lists all lost and found regions")
    void listsAllLostAndFoundRegions() {
        String[] args = {"lost", "list"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(14)).sendMessage(this.messageCaptor.capture());
        List<String> capturedMessages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Lost and Found Regions ]" + ChatColor.DARK_GRAY + "---------", capturedMessages.get(0));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "LostRegion3", capturedMessages.get(1));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(2));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "1, 2, 3", capturedMessages.get(3));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "4, 5, 6", capturedMessages.get(4));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "LostRegion2", capturedMessages.get(5));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(6));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "10, 20, 30", capturedMessages.get(7));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "40, 50, 60", capturedMessages.get(8));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "LostRegion1", capturedMessages.get(9));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(10));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "100, 200, 300", capturedMessages.get(11));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "400, 500, 600", capturedMessages.get(12));
        assertEquals(ChatColor.DARK_GRAY + "-----------------------------------------", capturedMessages.get(13));
    }

    @Test
    @DisplayName("Lists specific lost and found regions")
    void listsSpecificLostAndFoundRegions() {
        String[] args = {"lost", "list", "LostRegion1"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(6)).sendMessage(this.messageCaptor.capture());
        List<String> capturedMessages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Lost and Found Regions ]" + ChatColor.DARK_GRAY + "---------", capturedMessages.get(0));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "LostRegion1", capturedMessages.get(1));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(2));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "100, 200, 300", capturedMessages.get(3));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "400, 500, 600", capturedMessages.get(4));
        assertEquals(ChatColor.DARK_GRAY + "-----------------------------------------", capturedMessages.get(5));
    }

    @Test
    @DisplayName("Can't list specific lost and found region without valid name")
    void cannotListsSpecificLostAndFoundRegionInvalidName() {
        String[] args = {"lost", "list", "LostRegion1;"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.tpPets, never()).getLostRegion(anyString());
        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't find region named " + ChatColor.WHITE + "LostRegion1;", capturedMessage);
    }

    @Test
    @DisplayName("Can't list specific lost and found region where region doesn't exist")
    void cannotListsSpecificLostAndFoundRegionNoRegion() {
        when(this.tpPets.getLostRegion("LostRegion1")).thenReturn(null);

        String[] args = {"lost", "list", "LostRegion1"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't find region named " + ChatColor.WHITE + "LostRegion1", capturedMessage);
    }
}
