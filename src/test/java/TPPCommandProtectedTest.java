import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
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
import static org.mockito.Mockito.never;

public class TPPCommandProtectedTest {
    private Player admin;
    private ArgumentCaptor<String> stringCaptor;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private TPPets tpPets;
    private Command command;
    private CommandTPP commandTPP;

    @BeforeEach
    public void beforeEach() {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.protected"});
        this.stringCaptor = ArgumentCaptor.forClass(String.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("protected");
        aliases.put("protected", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, tpPets); }

    @Test
    @DisplayName("Can't run protected region command without a player sender")
    void cantRunProtectedRegionNotPlayer() throws SQLException {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("tppets.protected")).thenReturn(true);

        String[] args = {"protected", "remove", "ProtectedRegionName"};
        this.commandTPP.onCommand(sender, this.command, "", args);

        verify(this.dbWrapper, never()).removeProtectedRegion(anyString());
        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.tpPets, never()).removeProtectedRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("Can't run protected region command without command type")
    void cantRunProtectedRegionNoCommandType() throws SQLException {
        String[] args = {"protected"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).removeProtectedRegion(anyString());
        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.tpPets, never()).removeProtectedRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp pr [add/remove/list]", capturedMessage);
    }

    @Test
    @DisplayName("Can't run protected region command without a valid command type")
    void cantRunProtectedRegionInvalidCommandType() throws SQLException {
        String[] args = {"protected", "invalidtype"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).removeProtectedRegion(anyString());
        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.tpPets, never()).removeProtectedRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Syntax Error! Usage: /tpp pr [add/remove/list]", capturedMessage);
    }

    @Test
    @DisplayName("Can't run protected region command with f:[username] who hasn't played")
    void cantRunProtectedNoPlayer() throws SQLException {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            OfflinePlayer player = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
            when(player.hasPlayedBefore()).thenReturn(false);
            bukkit.when(() -> Bukkit.getOfflinePlayer("MockPlayerName")).thenReturn(player);

            String[] args = {"protected", "f:MockPlayerName", "remove", "ProtectedRegionName"};
            this.commandTPP.onCommand(this.admin, this.command, "", args);

            verify(this.dbWrapper, never()).removeProtectedRegion(anyString());
            verify(this.dbWrapper, never()).getProtectedRegion(anyString());
            verify(this.tpPets, never()).removeProtectedRegion(anyString());
            verify(this.logWrapper, never()).logSuccessfulAction(anyString());

            verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
            String capturedMessage = this.stringCaptor.getValue();
            assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName", capturedMessage);
        }
    }

    @Test
    @DisplayName("Can't run protected region command with invalid f:[username]")
    void cantRunProtectedRegionInvalidPlayer() throws SQLException {
        String[] args = {"protected", "f:MockPlayerName;", "remove", "ProtectedRegionName"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.dbWrapper, never()).removeProtectedRegion(anyString());
        verify(this.dbWrapper, never()).getProtectedRegion(anyString());
        verify(this.tpPets, never()).removeProtectedRegion(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());

        verify(this.admin, times(1)).sendMessage(this.stringCaptor.capture());
        String capturedMessage = this.stringCaptor.getValue();
        assertEquals(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + "MockPlayerName;", capturedMessage);
    }
}
