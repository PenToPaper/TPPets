import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
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
import static org.mockito.Mockito.times;

public class TPPCommandLostRemoveTest {
    private Player admin;
    private ArgumentCaptor<String> stringCaptor;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.lost"});
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("lost");
        aliases.put("lost", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets);

        World world = mock(World.class);

        when(this.dbWrapper.getLostRegion("LostRegionName")).thenReturn(new LostAndFoundRegion("LostRegionName", "MockWorldName", world, new Location(world, 100, 200, 300), new Location(world, 400, 500, 600)));
        when(this.dbWrapper.removeLostRegion("LostRegionName")).thenReturn(true);
    }

    @Test
    @DisplayName("Removes a lost and found region")
    void removesLostAndFoundRegion() throws SQLException {
        String[] args = {"lost", "remove", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, times(1)).removeLostRegion(this.stringCaptor.capture());
        verify(this.dbWrapper, times(1)).getLostRegion(this.stringCaptor.capture());
        verify(this.tpPets, times(1)).removeLFReference(this.stringCaptor.capture());
        verify(this.tpPets, times(1)).removeLostRegion(this.stringCaptor.capture());

        List<String> capturedRegionNames = this.stringCaptor.getAllValues();
        assertEquals("LostRegionName", capturedRegionNames.get(0));
        assertEquals("LostRegionName", capturedRegionNames.get(1));
        assertEquals("LostRegionName", capturedRegionNames.get(2));
        assertEquals("LostRegionName", capturedRegionNames.get(3));

        verify(this.logWrapper, times(1)).logSuccessfulAction(this.stringCaptor.capture());
        String capturedLogOutput = this.stringCaptor.getValue();
        assertEquals("Player MockAdminName removed lost and found region LostRegionName", capturedLogOutput);

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.BLUE + "You have removed lost and found region " + ChatColor.WHITE + "LostRegionName", capturedMessage);
    }

    @Test
    @DisplayName("Can't remove lost and found region without a region name")
    void cannotRemoveLostAndFoundRegionWithoutName() throws SQLException {
        String[] args = {"lost", "remove"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).removeLostRegion(anyString());
        verify(this.dbWrapper, never()).getLostRegion(anyString());
        verify(this.tpPets, never()).removeLFReference(anyString());
        verify(this.tpPets, never()).removeLostRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp lost remove [region name]", capturedMessage);
    }

    @Test
    @DisplayName("Can't remove lost and found region with invalid region name")
    void cannotRemoveLostAndFoundRegionInvalidName() throws SQLException {
        String[] args = {"lost", "remove", "LostRegionName;"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).removeLostRegion(anyString());
        verify(this.dbWrapper, never()).getLostRegion(anyString());
        verify(this.tpPets, never()).removeLFReference(anyString());
        verify(this.tpPets, never()).removeLostRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(this.stringCaptor.capture());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Invalid region name: " + ChatColor.WHITE + "LostRegionName;", capturedMessage);
    }

    @Test
    @DisplayName("Can't remove lost and found region when db fails to get region")
    void cannotRemoveLostAndFoundRegionDbFailGet() throws SQLException {
        when(this.dbWrapper.getLostRegion("LostRegionName")).thenThrow(new SQLException());

        String[] args = {"lost", "remove", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).removeLostRegion(anyString());
        verify(this.dbWrapper, times(1)).getLostRegion(anyString());
        verify(this.tpPets, never()).removeLFReference(anyString());
        verify(this.tpPets, never()).removeLostRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not remove lost and found region", capturedMessage);
    }

    @Test
    @DisplayName("Can't remove lost and found region when db finds no region")
    void cannotRemoveLostAndFoundRegionDbNoGet() throws SQLException {
        when(this.dbWrapper.getLostRegion("LostRegionName")).thenReturn(null);

        String[] args = {"lost", "remove", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).removeLostRegion(anyString());
        verify(this.dbWrapper, times(1)).getLostRegion(anyString());
        verify(this.tpPets, never()).removeLFReference(anyString());
        verify(this.tpPets, never()).removeLostRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Lost and Found Region " + ChatColor.WHITE + "LostRegionName" + ChatColor.RED + " already does not exist", capturedMessage);
    }

    @Test
    @DisplayName("Can't remove lost and found region when db can't remove region")
    void cannotRemoveLostAndFoundRegionDbFailRemove() throws SQLException {
        when(this.dbWrapper.removeLostRegion("LostRegionName")).thenReturn(false);

        String[] args = {"lost", "remove", "LostRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, times(1)).removeLostRegion(anyString());
        verify(this.dbWrapper, times(1)).getLostRegion(anyString());
        verify(this.tpPets, never()).removeLFReference(anyString());
        verify(this.tpPets, never()).removeLostRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Could not remove lost and found region", capturedMessage);
    }
}
