package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.LostRegionManager;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import com.maxwellwheeler.plugins.tppets.test.ObjectFactory;
import org.apache.commons.lang.StringUtils;
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
    private TPPets tpPets;
    private LostRegionManager lostRegionManager;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach() {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.lost"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, true);
        this.lostRegionManager = mock(LostRegionManager.class);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("lost");
        aliases.put("lost", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        World world = mock(World.class);


        List<LostAndFoundRegion> lostAndFoundRegions = new ArrayList<>();
        lostAndFoundRegions.add(ObjectFactory.getLostAndFoundRegion("LostRegion1", "MockWorldName", world, 100, 200, 300, 400, 500, 600));
        lostAndFoundRegions.add(ObjectFactory.getLostAndFoundRegion("LostRegion2", "MockWorldName", world, 10, 20, 30, 40, 50, 60));
        lostAndFoundRegions.add(ObjectFactory.getLostAndFoundRegion("LostRegion3", "MockWorldName", world, 1, 2, 3, 4, 5, 6));
        when(this.tpPets.getLostRegionManager()).thenReturn(this.lostRegionManager);
        when(this.lostRegionManager.getLostRegions()).thenReturn(lostAndFoundRegions);
        when(this.lostRegionManager.getLostRegion("LostRegion1")).thenReturn(lostAndFoundRegions.get(0));
    }

    @Test
    @DisplayName("Lists all lost and found regions")
    void listsAllLostAndFoundRegions() {
        String[] args = {"lost", "list"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(14)).sendMessage(this.messageCaptor.capture());
        List<String> capturedMessages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Lost and Found Regions ]" + ChatColor.DARK_GRAY + "---------", capturedMessages.get(0));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "LostRegion1", capturedMessages.get(1));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(2));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "100, 200, 300", capturedMessages.get(3));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "400, 500, 600", capturedMessages.get(4));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "LostRegion2", capturedMessages.get(5));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(6));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "10, 20, 30", capturedMessages.get(7));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "40, 50, 60", capturedMessages.get(8));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "LostRegion3", capturedMessages.get(9));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(10));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "1, 2, 3", capturedMessages.get(11));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "4, 5, 6", capturedMessages.get(12));
        assertEquals(ChatColor.DARK_GRAY + StringUtils.repeat("-", 42), capturedMessages.get(13));
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
        assertEquals(ChatColor.DARK_GRAY + StringUtils.repeat("-", 42), capturedMessages.get(5));
    }

    @Test
    @DisplayName("Can't list specific lost and found region without valid name")
    void cannotListsSpecificLostAndFoundRegionInvalidName() {
        String[] args = {"lost", "list", "LostRegion1;"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.lostRegionManager, never()).getLostRegion(anyString());
        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't find region named " + ChatColor.WHITE + "LostRegion1;", capturedMessage);
    }

    @Test
    @DisplayName("Can't list specific lost and found region where region doesn't exist")
    void cannotListsSpecificLostAndFoundRegionNoRegion() {
        when(this.lostRegionManager.getLostRegion("LostRegion1")).thenReturn(null);

        String[] args = {"lost", "list", "LostRegion1"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't find region named " + ChatColor.WHITE + "LostRegion1", capturedMessage);
    }
}
