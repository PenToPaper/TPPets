package com.maxwellwheeler.plugins.tppets.test.command;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class TPPCommandTPPTest {
    private CommandTPP commandTPP;
    private SQLWrapper sqlWrapper;
    private TPPets tpPets;
    private Command command;

    @BeforeEach
    public void beforeEach() {
        this.sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, logWrapper, false, true);

        Hashtable<String, List<String>> aliases = new Hashtable<>();
        List<String> altAlias = Arrays.asList("release1", "release2", "release3");
        aliases.put("release", altAlias);
        this.command = mock(Command.class);
        this.commandTPP = new CommandTPP(aliases, this.tpPets);
    }

    @Test
    @DisplayName("CommandTPP command aliases run each aliased command")
    void commandTPPAliases() throws SQLException {
        // Using CommandRelease
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            World world = mock(World.class);
            Chunk chunk = mock(Chunk.class);
            when(world.getChunkAt(43, 56)).thenReturn(chunk);
            bukkit.when(() -> Bukkit.getWorld("MockWorld")).thenReturn(world);

            Horse horse = MockFactory.getMockEntity("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA", Horse.class);
            Server server = mock(Server.class);
            when(this.tpPets.getServer()).thenReturn(server);
            when(server.getEntity(UUID.fromString("AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA"))).thenReturn(horse);

            Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.dogs"});
            when(this.tpPets.getAllowUntamingPets()).thenReturn(true);

            PetStorage pet = new PetStorage("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 7, 700, 800, 900, "MockWorldName", "MockPlayerId", "PetName", "PetName");
            when(this.sqlWrapper.getSpecificPet("MockPlayerId", "PetName")).thenReturn(pet);
            when(this.sqlWrapper.removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA")).thenReturn(true);

            // Base Command
            this.commandTPP.onCommand(player, this.command, "", new String[]{"release", "PetName"});

            // Aliases
            this.commandTPP.onCommand(player, this.command, "", new String[]{"release1", "PetName"});
            this.commandTPP.onCommand(player, this.command, "", new String[]{"release2", "PetName"});
            this.commandTPP.onCommand(player, this.command, "", new String[]{"release3", "PetName"});

            verify(this.sqlWrapper, times(4)).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, times(4)).removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            verify(player, times(4)).sendMessage(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "PetName" + ChatColor.BLUE + " has been released");

            // Not aliases
            reset(this.sqlWrapper);
            reset(player);

            this.commandTPP.onCommand(player, this.command, "", new String[]{"notrelease", "PetName"});

            verify(this.sqlWrapper, never()).getSpecificPet("MockPlayerId", "PetName");
            verify(this.sqlWrapper, never()).removePet("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            verify(player, never()).sendMessage(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "PetName" + ChatColor.BLUE + " has been released");
        }
    }

    @Test
    @DisplayName("Sends teleport-related help messages if player has permission to teleport any pet")
    void teleportHelp() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});

        for (PetType.Pets petType : PetType.Pets.values()) {
            if (petType != PetType.Pets.UNKNOWN) {
                when(player.hasPermission("tppets." + petType.toString().toLowerCase() + "s")).thenReturn(true);
                this.commandTPP.onCommand(player, this.command, "", new String[]{"notrelease"});

                verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp tp [pet name]" + ChatColor.BLUE + "  ->  Teleports the pet with [pet name] to your location");
                verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp all [dogs/cats/etc]" + ChatColor.BLUE + "  ->  Teleports all [dogs/cats/etc] to your location");
                verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp list [dogs/cats/etc]" + ChatColor.BLUE + "  ->  Lists your owned [dogs/cats/etc]");
                verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp tp f:[username] [pet name]" + ChatColor.BLUE + "  ->  Teleports [username]'s pet named [pet name] to your location");

                reset(player);
            }
        }
    }

    @Test
    @DisplayName("Sends guest-related help messages if player has permission to allow guests")
    void guestHelp() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.allowguests"});

        this.commandTPP.onCommand(player, this.command, "", new String[]{"notrelease"});

        verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp allow [username] [pet name]" + ChatColor.BLUE + "  ->  Allows [username] to use teleport and mount your pet named [pet name]");
        verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp remove [username] [pet name]" + ChatColor.BLUE + "  ->  Disallows [username] to use teleport and mount your pet named [pet name]");
        verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp allowed [pet name]" + ChatColor.BLUE + "  ->  Lists all players who can teleport and mount pet named [pet name]");
    }

    @Test
    @DisplayName("Sends rename-related help messages if player has permission to rename pets")
    void renameHelp() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.rename"});

        this.commandTPP.onCommand(player, this.command, "", new String[]{"notrelease"});

        verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp rename [old name] [new name]" + ChatColor.BLUE + "  ->  Renames [old name] to [new name].");
    }

    @Test
    @DisplayName("Sends storage-related help messages if player has permission to set storage locations")
    void storageHelp() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.storage"});

        this.commandTPP.onCommand(player, this.command, "", new String[]{"notrelease"});

        verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp storage [add, remove, list] [storage name]" + ChatColor.BLUE + "  ->  Adds a new storage location.");
    }

    @Test
    @DisplayName("Sends store-related help messages if player has permission to store pets")
    void storeHelp() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.store"});

        this.commandTPP.onCommand(player, this.command, "", new String[]{"notrelease"});

        verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp store [pet name] [storage name]" + ChatColor.BLUE + "  ->  Sends [pet name] to [storage name]");
    }

    @Test
    @DisplayName("Sends protected region-related help messages if player has permission to create protected regions")
    void protectedRegionHelp() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.protected"});

        this.commandTPP.onCommand(player, this.command, "", new String[]{"notrelease"});

        verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp protected [add, remove, list, relink]" + ChatColor.BLUE + "  ->  Creates a region where pets will not be allowed");
    }

    @Test
    @DisplayName("Sends lost and found region-related help messages if player has permission to create lost and found regions")
    void lostHelp() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.lost"});

        this.commandTPP.onCommand(player, this.command, "", new String[]{"notrelease"});

        verify(player, times(1)).sendMessage(ChatColor.WHITE + "/tpp lost [add, remove, list]" + ChatColor.BLUE + "  ->  Creates a region where lost pets will be teleported to");
    }

    @Test
    @DisplayName("Sends position1 and position2-related help messages if player has permission to create any region")
    void regionPositionHelp() {
        Player lostAndFoundRegion = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.lost"});

        this.commandTPP.onCommand(lostAndFoundRegion, this.command, "", new String[]{"notrelease"});

        verify(lostAndFoundRegion, times(1)).sendMessage(ChatColor.WHITE + "/tpp position1" + ChatColor.BLUE + "  ->  Assigns your current location as the first position for region creation");
        verify(lostAndFoundRegion, times(1)).sendMessage(ChatColor.WHITE + "/tpp position2" + ChatColor.BLUE + "  ->  Assigns your current location as the second position for region creation");


        Player protectedRegion = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{"tppets.protected"});

        this.commandTPP.onCommand(protectedRegion, this.command, "", new String[]{"notrelease"});

        verify(protectedRegion, times(1)).sendMessage(ChatColor.WHITE + "/tpp position1" + ChatColor.BLUE + "  ->  Assigns your current location as the first position for region creation");
        verify(protectedRegion, times(1)).sendMessage(ChatColor.WHITE + "/tpp position2" + ChatColor.BLUE + "  ->  Assigns your current location as the second position for region creation");
    }

    @Test
    @DisplayName("Sends permission message if player doesn't have permission for the specified command")
    void permissionMessage() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});

        // Base Command
        this.commandTPP.onCommand(player, this.command, "", new String[]{"release", "PetName"});

        verify(player, times(1)).sendMessage(ChatColor.RED + "You do not have permission to use that command.");
    }
}
