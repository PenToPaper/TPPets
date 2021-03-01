package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.RegionSelectionManager;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TPPCommandProtectedAddTest {
    private org.bukkit.World world;
    private Player admin;
    private ArgumentCaptor<String> stringCaptor;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<ProtectedRegion> regionCaptor;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private RegionSelectionManager regionSelectionManager;
    private LostAndFoundRegion linkedLostRegion;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.protected"});
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.regionCaptor = ArgumentCaptor.forClass(ProtectedRegion.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("protected");
        aliases.put("protected", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        this.world = mock(org.bukkit.World.class);
        this.regionSelectionManager = new RegionSelectionManager();
        this.regionSelectionManager.setStartLocation(this.admin, new Location(this.world, 100, 200, 300));
        this.regionSelectionManager.setEndLocation(this.admin, new Location(this.world, 400, 500, 600));
        this.linkedLostRegion = new LostAndFoundRegion("LostRegionName", "MockWorldName", this.world, new Location(this.world, 100, 200, 300), new Location(this.world, 400, 500, 600));

        when(this.world.getName()).thenReturn("MockWorldName");
        when(this.tpPets.getLostRegion("LostRegionName")).thenReturn(this.linkedLostRegion);
        when(this.tpPets.getRegionSelectionManager()).thenReturn(this.regionSelectionManager);
        when(this.dbWrapper.getProtectedRegion("ProtectedRegionName")).thenReturn(null);
        when(this.dbWrapper.insertProtectedRegion(any(ProtectedRegion.class))).thenReturn(true);
    }

    void assertEqualsProtectedRegion(ProtectedRegion expectedPr, ProtectedRegion actualPr) {
        assertEquals(expectedPr.getRegionName(), actualPr.getRegionName());
        assertEquals(expectedPr.getWorldName(), actualPr.getWorldName());
        assertEquals(expectedPr.getWorld(), actualPr.getWorld());
        assertEquals(expectedPr.getMinLoc().getBlockX(), actualPr.getMinLoc().getBlockX());
        assertEquals(expectedPr.getMinLoc().getBlockY(), actualPr.getMinLoc().getBlockY());
        assertEquals(expectedPr.getMinLoc().getBlockZ(), actualPr.getMinLoc().getBlockZ());
        assertEquals(expectedPr.getMinLoc().getWorld(), actualPr.getMinLoc().getWorld());
        assertEquals(expectedPr.getMaxLoc().getBlockX(), actualPr.getMaxLoc().getBlockX());
        assertEquals(expectedPr.getMaxLoc().getBlockY(), actualPr.getMaxLoc().getBlockY());
        assertEquals(expectedPr.getMaxLoc().getBlockZ(), actualPr.getMaxLoc().getBlockZ());
        assertEquals(expectedPr.getMaxLoc().getWorld(), actualPr.getMaxLoc().getWorld());
        assertEquals(expectedPr.getLfName(), actualPr.getLfName());
        assertEquals(expectedPr.getLfReference(), actualPr.getLfReference());
        assertEquals(expectedPr.getEnterMessage(), actualPr.getEnterMessage());
    }

    @Test
    @DisplayName("Adds a protected region")
    void addsProtectedRegion() throws SQLException {
        String[] args = {"protected", "add", "ProtectedRegionName", "LostRegionName", "Can't teleport here"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        ProtectedRegion expectedRegion = MockFactory.getProtectedRegion("ProtectedRegionName", "Can't teleport here", "MockWorldName", this.world,100, 200, 300, 400, 500, 600, "LostRegionName", this.tpPets);

        verify(this.dbWrapper, times(1)).getProtectedRegion(anyString());

        verify(this.dbWrapper, times(1)).insertProtectedRegion(this.regionCaptor.capture());
        ProtectedRegion capturedInsert = this.regionCaptor.getValue();
        assertEqualsProtectedRegion(expectedRegion, capturedInsert);

        verify(this.tpPets, times(1)).addProtectedRegion(this.regionCaptor.capture());
        ProtectedRegion capturedCache = this.regionCaptor.getValue();
        assertEqualsProtectedRegion(expectedRegion, capturedCache);

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.stringCaptor.capture());
        String capturedLogOutput = this.stringCaptor.getValue();
        assertEquals("Player MockAdminName added protected region ProtectedRegionName", capturedLogOutput);

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.BLUE + "You have added protected region " + ChatColor.WHITE + "ProtectedRegionName", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a protected region without all 3 required arguments")
    void cantAddProtectedRegionWithoutArgs() throws SQLException {
        String[] args = {"protected", "add", "ProtectedRegionName", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.dbWrapper, never()).insertProtectedRegion(any(ProtectedRegion.class));
        verify(this.tpPets, never()).addProtectedRegion(any(ProtectedRegion.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp protected add [region name] [lost and found region] [enter message]", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a protected region with invalid protected region name")
    void cantAddProtectedRegionInvalidPrName() throws SQLException {
        String[] args = {"protected", "add", "ProtectedRegionName;", "LostRegionName", "Can't teleport here"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.dbWrapper, never()).insertProtectedRegion(any(ProtectedRegion.class));
        verify(this.tpPets, never()).addProtectedRegion(any(ProtectedRegion.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Invalid protected region name: " + ChatColor.WHITE + "ProtectedRegionName;", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a protected region with invalid lost and found region name")
    void cantAddProtectedRegionInvalidLfrName() throws SQLException {
        String[] args = {"protected", "add", "ProtectedRegionName", "LostRegionName;", "Can't teleport here"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.dbWrapper, never()).insertProtectedRegion(any(ProtectedRegion.class));
        verify(this.tpPets, never()).addProtectedRegion(any(ProtectedRegion.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Invalid lost and found region name: " + ChatColor.WHITE + "LostRegionName;", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a protected region with invalid enter message")
    void cantAddProtectedRegionInvalidEnterMessage() throws SQLException {
        String[] args = {"protected", "add", "ProtectedRegionName", "LostRegionName", "Can't teleport here;"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.dbWrapper, never()).insertProtectedRegion(any(ProtectedRegion.class));
        verify(this.tpPets, never()).addProtectedRegion(any(ProtectedRegion.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Invalid enter message: " + ChatColor.WHITE + "Can't teleport here;", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a protected region without selection object")
    void cantAddProtectedRegionNoRegionSelection() throws SQLException {
        this.regionSelectionManager.clearPlayerSession(this.admin);

        String[] args = {"protected", "add", "ProtectedRegionName", "LostRegionName", "Can't teleport here"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.dbWrapper, never()).insertProtectedRegion(any(ProtectedRegion.class));
        verify(this.tpPets, never()).addProtectedRegion(any(ProtectedRegion.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't add region without a region selection", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a protected region with incomplete selection object")
    void cantAddProtectedRegionIncompleteRegionSelection() throws SQLException {
        this.regionSelectionManager.clearPlayerSession(this.admin);
        this.regionSelectionManager.setStartLocation(this.admin, new Location(this.world, 100, 200, 300));

        String[] args = {"protected", "add", "ProtectedRegionName", "LostRegionName", "Can't teleport here"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.dbWrapper, never()).insertProtectedRegion(any(ProtectedRegion.class));
        verify(this.tpPets, never()).addProtectedRegion(any(ProtectedRegion.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't add region without a region selection", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a protected region when database fails when getting protected region")
    void cantAddProtectedRegionDbFailGettingPr() throws SQLException {
        when(this.dbWrapper.getProtectedRegion("ProtectedRegionName")).thenThrow(new SQLException());

        String[] args = {"protected", "add", "ProtectedRegionName", "LostRegionName", "Can't teleport here"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, times(1)).getProtectedRegion(anyString());
        verify(this.dbWrapper, never()).insertProtectedRegion(any(ProtectedRegion.class));
        verify(this.tpPets, never()).addProtectedRegion(any(ProtectedRegion.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add protected region", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a protected region when database finds a protected region with same name")
    void cantAddProtectedRegionAlreadyDone() throws SQLException {
        ProtectedRegion pr = MockFactory.getProtectedRegion("ProtectedRegionName", "Enter message", "MockWorldName", this.world, 100, 200, 300, 400, 500, 600, "LostAndFoundRegionName", this.tpPets);
        when(this.dbWrapper.getProtectedRegion("ProtectedRegionName")).thenReturn(pr);

        String[] args = {"protected", "add", "ProtectedRegionName", "LostRegionName", "Can't teleport here"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, times(1)).getProtectedRegion(anyString());
        verify(this.dbWrapper, never()).insertProtectedRegion(any(ProtectedRegion.class));
        verify(this.tpPets, never()).addProtectedRegion(any(ProtectedRegion.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Region " + ChatColor.WHITE + "ProtectedRegionName" + ChatColor.RED + " already exists", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a protected region when database fails to insert the new protected region")
    void cantAddProtectedRegionDbFailInsertingPr() throws SQLException {
        when(this.dbWrapper.insertProtectedRegion(any(ProtectedRegion.class))).thenReturn(false);

        String[] args = {"protected", "add", "ProtectedRegionName", "LostRegionName", "Can't teleport here"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, times(1)).getProtectedRegion(anyString());
        verify(this.dbWrapper, times(1)).insertProtectedRegion(any(ProtectedRegion.class));
        verify(this.tpPets, never()).addProtectedRegion(any(ProtectedRegion.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add protected region", capturedMessage);
    }
}
