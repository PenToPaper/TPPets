import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.Assert.*;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.TPPets;
import org.bukkit.command.Command;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

@DisplayName("Teleporting owned pets to players")
class ToOwnerTeleportTest {

    @Test
    @DisplayName("Teleports valid owned horses with /tpp horse [horse name]")
    void teleportsValidHorse() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            // World instance
            World world = mock(World.class);
            List<World> worlds = new ArrayList<>();
            worlds.add(world);

            //  Bukkit static mock
            bukkit.when(() -> Bukkit.getWorld(anyString())).thenReturn(world);
            bukkit.when(Bukkit::getWorlds).thenReturn(worlds);

            // Chunk + Location the correct pet is in
            Chunk chunk = mock(Chunk.class);
            Location location = mock(Location.class);
            when(location.getChunk()).thenReturn(chunk);
            when(world.getChunkAt(anyInt(), anyInt())).thenReturn(chunk);

            // The correct pet Entity instance
            // TODO: MAKE ENTITY IMPLEMENT SITTABLE TO TEST SETSITTING METHOD
            Entity correctPet = mock(Entity.class);
            ArgumentCaptor<Location> correctPetCaptor = ArgumentCaptor.forClass(Location.class);
            UUID correctUUID = mock(UUID.class);
            when(correctUUID.toString()).thenReturn("MockPetId");
            when(correctPet.getUniqueId()).thenReturn(correctUUID);
            when(correctPet.getPassengers()).thenReturn(null);

            // The incorrect pet Entity instance
            Entity incorrectPet = mock(Entity.class);
            UUID incorrectUUID = mock(UUID.class);
            when(incorrectUUID.toString()).thenReturn("MockIncorrectPetId");
            when(incorrectPet.getUniqueId()).thenReturn(incorrectUUID);
            when(incorrectPet.getPassengers()).thenReturn(null);

            // A list of both entities
            List<Entity> entityList = new ArrayList<>();
            entityList.add(correctPet);
            entityList.add(incorrectPet);
            when(world.getEntitiesByClasses(any())).thenReturn(entityList);

            // PetStorage
            PetStorage pet = new PetStorage("MockPetId", 7, 100, 100, 100, "MockWorld", "MockOwnerId", "HORSE0", "HORSE0");
            List<PetStorage> petList = new ArrayList<>();
            petList.add(pet);

            // Plugin database wrapper instance
            DBWrapper dbWrapper = mock(DBWrapper.class);
            when(dbWrapper.getPetByName(any(), any())).thenReturn(pet);
            when(dbWrapper.getPetsFromOwnerNamePetType(any(), any(), any())).thenReturn(petList);

            // Plugin log wrapper instance
            LogWrapper logWrapper = mock(LogWrapper.class);
            ArgumentCaptor<String> logWrapperCaptor = ArgumentCaptor.forClass(String.class);

            // Plugin instance
            TPPets tpPets = mock(TPPets.class);
            when(tpPets.getDatabase()).thenReturn(dbWrapper);
            when(tpPets.canTpThere(any())).thenReturn(true);
            when(tpPets.getAllowTpBetweenWorlds()).thenReturn(false);
            when(tpPets.getLogWrapper()).thenReturn(logWrapper);

            // Command aliases
            Hashtable<String, List<String>> aliases = new Hashtable<>();
            List<String> altAlias = new ArrayList<>();
            altAlias.add("horses");
            aliases.put("horses", altAlias);

            // Location to send the pet to
            Location sendTo = mock(Location.class);
            when(sendTo.getX()).thenReturn(1000d);
            when(sendTo.getY()).thenReturn(100d);
            when(sendTo.getZ()).thenReturn(1000d);
            when(sendTo.getWorld()).thenReturn(world);
            when(sendTo.getBlockX()).thenReturn(1000);
            when(sendTo.getBlockY()).thenReturn(100);
            when(sendTo.getBlockZ()).thenReturn(1000);

            // Player who sent the command
            Player sender = mock(Player.class);
            UUID senderUUID = mock(UUID.class);
            when(senderUUID.toString()).thenReturn("MockPlayerId");
            when(sender.getLocation()).thenReturn(sendTo);
            when(sender.getUniqueId()).thenReturn(senderUUID);
            when(sender.hasPermission(anyString())).thenReturn(true);
            when(sender.getWorld()).thenReturn(world);
            when(sender.getName()).thenReturn("MockPlayerName");
            ArgumentCaptor<String> playerMessageCaptor = ArgumentCaptor.forClass(String.class);

            // Command object
            Command command = mock(Command.class);
            String[] args = {"horses", "HORSE0"};
            CommandTPP commandTPP = new CommandTPP(aliases, tpPets);
            commandTPP.onCommand(sender, command, "", args);

            verify(chunk, times(1)).load();
            verify(correctPet, times(1)).eject();
            verify(correctPet).teleport(correctPetCaptor.capture());
            Location capturedPetLocation = correctPetCaptor.getValue();
            assertEquals( sendTo.getX(), capturedPetLocation.getX(), 0.5);
            verify(logWrapper).logSuccessfulAction(logWrapperCaptor.capture());
            String capturedLogOutput = logWrapperCaptor.getValue();
            assertEquals("Player MockPlayerName teleported MockPlayerName's pet named HORSE0 to their location at: x: 1000, y: 100, z: 1000", capturedLogOutput);
            verify(sender).sendMessage(playerMessageCaptor.capture());
            String capturedMessageOutput = playerMessageCaptor.getValue();
            assertEquals(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + "HORSE0" + ChatColor.BLUE + " has been teleported to you", capturedMessageOutput);
            verify(incorrectPet, times(0)).teleport(any(Location.class));

        }
    }
}