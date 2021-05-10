package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegionManager;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static org.mockito.Mockito.*;

public class TPPCommandProtectedRelinkTest {
    private Player admin;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private ProtectedRegion protectedRegion;
    private Command command;
    private CommandTPP commandTPP;
    private TPPets tpPets;
    private ProtectedRegionManager protectedRegionManager;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.protected"});
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, true, false, true);
        this.command = mock(Command.class);

        this.protectedRegionManager = mock(ProtectedRegionManager.class);
        this.protectedRegion = mock(ProtectedRegion.class);

        when(this.protectedRegionManager.getProtectedRegion("ProtectedRegion")).thenReturn(this.protectedRegion);
        when(this.tpPets.getProtectedRegionManager()).thenReturn(this.protectedRegionManager);
        when(this.sqlWrapper.relinkProtectedRegion("ProtectedRegion", "LostRegion")).thenReturn(true);

        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = new ArrayList<>();
        altAlias.add("protected");
        aliases.put("protected", altAlias);

        this.commandTPP = new CommandTPP(aliases, tpPets);
    }

    @Test
    @DisplayName("Relinks existing protected regions to lost regions")
    void relinks() throws SQLException {
        String[] args = {"protected", "relink", "ProtectedRegion", "LostRegion"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(1)).sendMessage(ChatColor.BLUE + "You have relinked protected region " + ChatColor.WHITE + "ProtectedRegion" + ChatColor.BLUE + " to lost and found region " + ChatColor.WHITE + "LostRegion");
        verify(this.sqlWrapper, times(1)).relinkProtectedRegion("ProtectedRegion", "LostRegion");
        verify(this.protectedRegion, times(1)).setLfName("LostRegion");
        verify(this.protectedRegion, times(1)).updateLFReference(this.tpPets);
        verify(this.logWrapper, times(1)).logSuccessfulAction("Player " + this.admin.getName() + " relinked protected region " + this.protectedRegion.getRegionName() + " to " + this.protectedRegion.getLfName());
    }

    @Test
    @DisplayName("Does not relink if not enough arguments provided")
    void doesNotRelinkNotEnoughArgs() throws SQLException {
        String[] args = {"protected", "relink", "ProtectedRegion"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp protected relink [protected region name] [lost and found region name]");
        verify(this.sqlWrapper, never()).relinkProtectedRegion(anyString(), anyString());
        verify(this.protectedRegion, never()).setLfName(anyString());
        verify(this.protectedRegion, never()).updateLFReference(any(TPPets.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Does not relink if given invalid protected region name")
    void doesNotRelinkInvalidPrName() throws SQLException {
        String[] args = {"protected", "relink", "ProtectedRegion;", "LostRegion"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Invalid protected region name: " + ChatColor.WHITE + "ProtectedRegion;");
        verify(this.sqlWrapper, never()).relinkProtectedRegion(anyString(), anyString());
        verify(this.protectedRegion, never()).setLfName(anyString());
        verify(this.protectedRegion, never()).updateLFReference(any(TPPets.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Does not relink if given invalid lost region name")
    void doesNotRelinkInvalidLfrName() throws SQLException {
        String[] args = {"protected", "relink", "ProtectedRegion", "LostRegion;"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Invalid lost and found region name: " + ChatColor.WHITE + "LostRegion;");
        verify(this.sqlWrapper, never()).relinkProtectedRegion(anyString(), anyString());
        verify(this.protectedRegion, never()).setLfName(anyString());
        verify(this.protectedRegion, never()).updateLFReference(any(TPPets.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Does not relink if cannot find protected region with name")
    void doesNotRelinkNoPr() throws SQLException {
        when(this.protectedRegionManager.getProtectedRegion("ProtectedRegion")).thenReturn(null);

        String[] args = {"protected", "relink", "ProtectedRegion", "LostRegion"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Can't find protected region: " + ChatColor.WHITE + "ProtectedRegion");
        verify(this.sqlWrapper, never()).relinkProtectedRegion(anyString(), anyString());
        verify(this.protectedRegion, never()).setLfName(anyString());
        verify(this.protectedRegion, never()).updateLFReference(any(TPPets.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Does not relink if database doesn't update row")
    void doesNotRelinkDbNoUpdate() throws SQLException {
        when(this.sqlWrapper.relinkProtectedRegion("ProtectedRegion", "LostRegion")).thenReturn(false);

        String[] args = {"protected", "relink", "ProtectedRegion", "LostRegion"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Could not relink regions");
        verify(this.sqlWrapper, times(1)).relinkProtectedRegion("ProtectedRegion", "LostRegion");
        verify(this.protectedRegion, never()).setLfName(anyString());
        verify(this.protectedRegion, never()).updateLFReference(any(TPPets.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Does not relink if database fails")
    void doesNotRelinkDbFail() throws SQLException {
        when(this.sqlWrapper.relinkProtectedRegion("ProtectedRegion", "LostRegion")).thenThrow(new SQLException());

        String[] args = {"protected", "relink", "ProtectedRegion", "LostRegion"};
        this.commandTPP.onCommand(this.admin, this.command, "", args);

        verify(this.admin, times(1)).sendMessage(ChatColor.RED + "Could not relink regions");
        verify(this.sqlWrapper, times(1)).relinkProtectedRegion("ProtectedRegion", "LostRegion");
        verify(this.protectedRegion, never()).setLfName(anyString());
        verify(this.protectedRegion, never()).updateLFReference(any(TPPets.class));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
    }
}
