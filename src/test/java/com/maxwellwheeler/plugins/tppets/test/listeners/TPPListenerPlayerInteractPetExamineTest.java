package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.ToolsManager;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerPlayerInteractPetExamine;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class TPPListenerPlayerInteractPetExamineTest {
    private PlayerInteractEntityEvent playerInteractEntityEvent;
    private ListenerPlayerInteractPetExamine listenerPlayerInteractPetExamine;
    private Horse horse;
    private Player player;
    private ToolsManager toolsManager;
    private SQLWrapper sqlWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.playerInteractEntityEvent = mock(PlayerInteractEntityEvent.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.toolsManager = mock(ToolsManager.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, logWrapper, false, false, false);
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.player);

        EquipmentSlot playerHand = EquipmentSlot.HAND;
        PetStorage horseStorage = new PetStorage("MockHorseId", 7, 100, 200, 300, "MockWorldName", "MockPlayerId", "MockHorseName", "MockHorseName");
        this.listenerPlayerInteractPetExamine = new ListenerPlayerInteractPetExamine(tpPets);

        PlayerInventory playerInventory = mock(PlayerInventory.class);
        ItemStack itemStack = mock(ItemStack.class);

        when(this.playerInteractEntityEvent.getHand()).thenReturn(playerHand);
        when(this.playerInteractEntityEvent.getPlayer()).thenReturn(this.player);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(this.horse);
        when(this.player.isSneaking()).thenReturn(true);
        when(this.player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getItemInMainHand()).thenReturn(itemStack);
        when(itemStack.getType()).thenReturn(Material.BONE);
        when(tpPets.getToolsManager()).thenReturn(this.toolsManager);
        when(this.toolsManager.isMaterialValidTool("get_owner", Material.BONE)).thenReturn(true);
        when(this.sqlWrapper.getSpecificPet("MockHorseId", "MockPlayerId")).thenReturn(Collections.singletonList(horseStorage));
    }

    @Test
    @DisplayName("Displays pet owner")
    void displaysPetOwner() throws SQLException {
        this.listenerPlayerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).getSpecificPet(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "This is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " and belongs to " + ChatColor.WHITE + "MockPlayerName");
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't attempt to display pet owner if clicking with offhand")
    void doesNotAttemptToDisplayWhenClickingWithOffhand() throws SQLException {
        when(this.playerInteractEntityEvent.getHand()).thenReturn(EquipmentSlot.OFF_HAND);

        this.listenerPlayerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't attempt to display pet owner if player isn't sneaking")
    void doesNotAttemptToDisplayWhenNotSneaking() throws SQLException {
        when(this.player.isSneaking()).thenReturn(false);

        this.listenerPlayerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't attempt to display pet owner if player isn't clicking with the correct material")
    void doesNotAttemptToDisplayInvalidMaterial() throws SQLException {
        when(this.toolsManager.isMaterialValidTool("get_owner", Material.BONE)).thenReturn(false);

        this.listenerPlayerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet type is not tracked by TPPets")
    void displaysNoOwnerInvalidPetType() throws SQLException {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", Villager.class);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(villager);

        this.listenerPlayerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "This pet doesn't have an owner");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet is not tamed")
    void displaysNoOwnerNotOwned() throws SQLException {
        when(this.horse.isTamed()).thenReturn(false);

        this.listenerPlayerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "This pet doesn't have an owner");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet has no owner")
    void displaysNoOwnerNoOwner() throws SQLException {
        when(this.horse.getOwner()).thenReturn(null);

        this.listenerPlayerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).getSpecificPet(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "This pet doesn't have an owner");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays db fail if database fails")
    void displaysDbFailWhenDbFails() throws SQLException {
        when(this.sqlWrapper.getSpecificPet("MockHorseId", "MockPlayerId")).thenReturn(null);

        this.listenerPlayerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).getSpecificPet(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not get pet data");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays db fail if database can't find the pet in the database")
    void displaysDbFailCantFindPet() throws SQLException {
        when(this.sqlWrapper.getSpecificPet("MockHorseId", "MockPlayerId")).thenReturn(new ArrayList<>());

        this.listenerPlayerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).getSpecificPet(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not get pet data");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }
}
