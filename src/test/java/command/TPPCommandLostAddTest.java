package command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.RegionSelectionManager;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
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
import static org.mockito.Mockito.*;

public class TPPCommandLostAddTest {
    private org.bukkit.World world;
    private Player admin;
    private ArgumentCaptor<String> messageCaptor;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<String> logCaptor;
    private ArgumentCaptor<String> stringCaptor;
    private ArgumentCaptor<LostAndFoundRegion> regionCaptor;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;
    private RegionSelectionManager regionSelectionManager;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.lost"});
        this.messageCaptor = ArgumentCaptor.forClass(String.class);
        this.regionCaptor = ArgumentCaptor.forClass(LostAndFoundRegion.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.logCaptor = ArgumentCaptor.forClass(String.class);
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("lost");
        aliases.put("lost", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);
        this.world = mock(org.bukkit.World.class);
        this.regionSelectionManager = new RegionSelectionManager();
        this.regionSelectionManager.setStartLocation(this.admin, new Location(this.world, 100, 200, 300));
        this.regionSelectionManager.setEndLocation(this.admin, new Location(this.world, 400, 500, 600));

        when(this.world.getName()).thenReturn("MockWorldName");
        when(this.tpPets.getRegionSelectionManager()).thenReturn(this.regionSelectionManager);
        when(this.dbWrapper.getLostRegion("LostRegionName")).thenReturn(null);
        when(this.dbWrapper.insertLostRegion(any(LostAndFoundRegion.class))).thenReturn(true);
    }

    void assertEqualsLostAndFoundRegion(LostAndFoundRegion expectedLfr, LostAndFoundRegion actualLfr) {
        assertEquals(expectedLfr.getRegionName(), actualLfr.getRegionName());
        assertEquals(expectedLfr.getWorldName(), actualLfr.getWorldName());
        assertEquals(expectedLfr.getWorld(), actualLfr.getWorld());
        assertEquals(expectedLfr.getMinLoc().getBlockX(), actualLfr.getMinLoc().getBlockX());
        assertEquals(expectedLfr.getMinLoc().getBlockY(), actualLfr.getMinLoc().getBlockY());
        assertEquals(expectedLfr.getMinLoc().getBlockZ(), actualLfr.getMinLoc().getBlockZ());
        assertEquals(expectedLfr.getMinLoc().getWorld(), actualLfr.getMinLoc().getWorld());
        assertEquals(expectedLfr.getMaxLoc().getBlockX(), actualLfr.getMaxLoc().getBlockX());
        assertEquals(expectedLfr.getMaxLoc().getBlockY(), actualLfr.getMaxLoc().getBlockY());
        assertEquals(expectedLfr.getMaxLoc().getBlockZ(), actualLfr.getMaxLoc().getBlockZ());
        assertEquals(expectedLfr.getMaxLoc().getWorld(), actualLfr.getMaxLoc().getWorld());
    }

    @Test
    @DisplayName("Adds a lost and found region")
    void addsLostAndFoundRegion() throws SQLException {
        String[] args = {"lost", "add", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        LostAndFoundRegion expectedRegion = MockFactory.getLostAndFoundRegion("LostRegionName", "MockWorldName", this.world,100, 200, 300, 400, 500, 600);

        verify(this.dbWrapper, times(1)).getLostRegion(anyString());

        verify(this.dbWrapper, times(1)).insertLostRegion(this.regionCaptor.capture());
        LostAndFoundRegion capturedInsert = this.regionCaptor.getValue();
        assertEqualsLostAndFoundRegion(expectedRegion, capturedInsert);

        verify(this.tpPets, times(1)).addLostRegion(this.regionCaptor.capture());
        LostAndFoundRegion capturedCache = this.regionCaptor.getValue();
        assertEqualsLostAndFoundRegion(expectedRegion, capturedCache);

        verify(this.tpPets, times(1)).updateLFReference(this.stringCaptor.capture());
        String capturedLfReferenceString = this.stringCaptor.getValue();
        assertEquals(capturedLfReferenceString, "LostRegionName");

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.logCaptor.capture());
        String capturedLogOutput = this.logCaptor.getValue();
        assertEquals("Player MockAdminName added lost and found region LostRegionName", capturedLogOutput);

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.BLUE + "You have added lost and found region " + ChatColor.WHITE + "LostRegionName", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a lost and found region without name")
    void cannotAddLostAndFoundRegionNoName() throws SQLException {
        String[] args = {"lost", "add"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getLostRegion(anyString());
        verify(this.dbWrapper, never()).insertLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).addLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).updateLFReference(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp lost add [region name]", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a lost and found region with invalid name")
    void cannotAddLostAndFoundRegionInvalidName() throws SQLException {
        String[] args = {"lost", "add", "LostRegionName;"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getLostRegion(anyString());
        verify(this.dbWrapper, never()).insertLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).addLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).updateLFReference(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Invalid region name: " + ChatColor.WHITE + "LostRegionName;", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a lost and found region without a region selection")
    void cannotAddLostAndFoundRegionNoSelection() throws SQLException {
        this.regionSelectionManager.clearPlayerSession(this.admin);

        String[] args = {"lost", "add", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getLostRegion(anyString());
        verify(this.dbWrapper, never()).insertLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).addLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).updateLFReference(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't add region without a region selection", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a lost and found region without a complete region selection")
    void cannotAddLostAndFoundRegionIncompleteSelection() throws SQLException {
        this.regionSelectionManager.clearPlayerSession(this.admin);
        this.regionSelectionManager.setStartLocation(this.admin, new Location(this.world, 100, 200, 300));

        String[] args = {"lost", "add", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).getLostRegion(anyString());
        verify(this.dbWrapper, never()).insertLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).addLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).updateLFReference(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't add region without a region selection", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a lost and found region when one with the same name exists")
    void cannotAddLostAndFoundRegionAlreadyDone() throws SQLException {
        when(this.dbWrapper.getLostRegion("LostRegionName")).thenReturn(MockFactory.getLostAndFoundRegion("LostRegionName", "MockWorldName", this.world,100, 200, 300, 400, 500, 600));

        String[] args = {"lost", "add", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, times(1)).getLostRegion(anyString());
        verify(this.dbWrapper, never()).insertLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).addLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).updateLFReference(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Region " + ChatColor.WHITE + "LostRegionName" + ChatColor.RED + " already exists", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a lost and found region when database fails to determine if region already exists")
    void cannotAddLostAndFoundRegionDbSearchFail() throws SQLException {
        when(this.dbWrapper.getLostRegion("LostRegionName")).thenThrow(new SQLException());

        String[] args = {"lost", "add", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, times(1)).getLostRegion(anyString());
        verify(this.dbWrapper, never()).insertLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).addLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).updateLFReference(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add lost and found region", capturedMessage);
    }

    @Test
    @DisplayName("Can't add a lost and found region when database fails to insert new region")
    void cannotAddLostAndFoundRegionDbInsertFail() throws SQLException {
        when(this.dbWrapper.insertLostRegion(any(LostAndFoundRegion.class))).thenReturn(false);

        String[] args = {"lost", "add", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, times(1)).getLostRegion(anyString());
        verify(this.dbWrapper, times(1)).insertLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).addLostRegion(any(LostAndFoundRegion.class));
        verify(this.tpPets, never()).updateLFReference(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.messageCaptor.capture());
        String capturedMessage = this.messageCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not add lost and found region", capturedMessage);
    }
}