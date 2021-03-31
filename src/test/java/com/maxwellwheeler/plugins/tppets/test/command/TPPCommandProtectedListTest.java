package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import com.maxwellwheeler.plugins.tppets.test.ObjectFactory;
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

public class TPPCommandProtectedListTest {
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach() {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.protected"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("protected");
        aliases.put("protected", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        World world = mock(World.class);

        LostAndFoundRegion lfr = ObjectFactory.getLostAndFoundRegion("LostAndFoundRegion", "MockWorldName", world, 1, 2, 3, 4, 5, 6);

        when(this.tpPets.getLostRegion("LostAndFoundRegion")).thenReturn(lfr);

        Hashtable<String, ProtectedRegion> protectedRegions = new Hashtable<>();
        protectedRegions.put("ProtectedRegion1", ObjectFactory.getProtectedRegion("ProtectedRegion1", "Enter Message", "MockWorldName", world, 100, 200, 300, 400, 500, 600, "LostAndFoundRegion", this.tpPets));
        protectedRegions.put("ProtectedRegion2", ObjectFactory.getProtectedRegion("ProtectedRegion2", "Enter Message", "MockWorldName", world, 10, 20, 30, 40, 50, 60, "LostRegion", this.tpPets));

        when(this.tpPets.getProtectedRegions()).thenReturn(protectedRegions);
        when(this.tpPets.getProtectedRegion("ProtectedRegion1")).thenReturn(protectedRegions.get("ProtectedRegion1"));
    }

    @Test
    @DisplayName("Lists all protected regions")
    void listsAllProtectedRegions() {
        String[] args = {"protected", "list"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(14)).sendMessage(this.messageCaptor.capture());
        List<String> capturedMessages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Protected Regions ]" + ChatColor.DARK_GRAY + "---------", capturedMessages.get(0));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "ProtectedRegion2", capturedMessages.get(1));
        assertEquals(ChatColor.BLUE + "    Enter Message: " + ChatColor.WHITE + "Enter Message", capturedMessages.get(2));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(3));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "10, 20, 30", capturedMessages.get(4));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "40, 50, 60", capturedMessages.get(5));
        assertEquals(ChatColor.BLUE + "    Lost Region: " + ChatColor.WHITE + "LostRegion" + ChatColor.BLUE + " (Unset)", capturedMessages.get(6));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "ProtectedRegion1", capturedMessages.get(7));
        assertEquals(ChatColor.BLUE + "    Enter Message: " + ChatColor.WHITE + "Enter Message", capturedMessages.get(8));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(9));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "100, 200, 300", capturedMessages.get(10));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "400, 500, 600", capturedMessages.get(11));
        assertEquals(ChatColor.BLUE + "    Lost Region: " + ChatColor.WHITE + "LostAndFoundRegion", capturedMessages.get(12));
        assertEquals(ChatColor.DARK_GRAY + "-----------------------------------------", capturedMessages.get(13));
    }

    @Test
    @DisplayName("Lists specific protected regions")
    void listsSpecificProtectedRegions() {
        String[] args = {"protected", "list", "ProtectedRegion1"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(8)).sendMessage(this.messageCaptor.capture());
        List<String> capturedMessages = this.messageCaptor.getAllValues();
        assertEquals(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Protected Regions ]" + ChatColor.DARK_GRAY + "---------", capturedMessages.get(0));
        assertEquals(ChatColor.BLUE + "Name: " + ChatColor.WHITE + "ProtectedRegion1", capturedMessages.get(1));
        assertEquals(ChatColor.BLUE + "    Enter Message: " + ChatColor.WHITE + "Enter Message", capturedMessages.get(2));
        assertEquals(ChatColor.BLUE + "    World: " + ChatColor.WHITE + "MockWorldName", capturedMessages.get(3));
        assertEquals(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + "100, 200, 300", capturedMessages.get(4));
        assertEquals(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + "400, 500, 600", capturedMessages.get(5));
        assertEquals(ChatColor.BLUE + "    Lost Region: " + ChatColor.WHITE + "LostAndFoundRegion", capturedMessages.get(6));
        assertEquals(ChatColor.DARK_GRAY + "-----------------------------------------", capturedMessages.get(7));
    }

    @Test
    @DisplayName("Can't list specific protected region without valid name")
    void cannotListsSpecificProtectedRegionInvalidName() {
        String[] args = {"protected", "list", "ProtectedRegion1;"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.tpPets, never()).getProtectedRegion(anyString());
        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't find region named " + ChatColor.WHITE + "ProtectedRegion1;", capturedMessage);
    }

    @Test
    @DisplayName("Can't list specific protected region where region doesn't exist")
    void cannotListsSpecificProtectedRegionNoRegion() {
        when(this.tpPets.getProtectedRegion("ProtectedRegion1")).thenReturn(null);

        String[] args = {"protected", "list", "ProtectedRegion1"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.tpPets, times(1)).getProtectedRegion(anyString());
        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't find region named " + ChatColor.WHITE + "ProtectedRegion1", capturedMessage);
    }
}
