package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.ToolsManager;
import com.maxwellwheeler.plugins.tppets.listeners.PlayerInteractPetExamine;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
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

import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class TPPPlayerInteractPetExamineTest {
    private PlayerInteractEntityEvent playerInteractEntityEvent;
    private PlayerInteractPetExamine playerInteractPetExamine;
    private Horse horse;
    private Player player;
    private ToolsManager toolsManager;
    private DBWrapper dbWrapper;

    @BeforeEach
    public void beforeEach() {
        this.playerInteractEntityEvent = mock(PlayerInteractEntityEvent.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.toolsManager = mock(ToolsManager.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        TPPets tpPets = MockFactory.getMockPlugin(this.dbWrapper, logWrapper, false, false, false);
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.player);

        EquipmentSlot playerHand = EquipmentSlot.HAND;
        PetStorage horseStorage = new PetStorage("MockHorseId", 7, 100, 200, 300, "MockWorldname", "MockPlayerId", "MockHorseName", "MockHorseName");
        this.playerInteractPetExamine = new PlayerInteractPetExamine(tpPets);

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
        when(this.dbWrapper.getPetsFromUUIDs("MockHorseId", "MockPlayerId")).thenReturn(Collections.singletonList(horseStorage));
    }

    @Test
    @DisplayName("Displays pet owner")
    void displaysPetOwner() {
        this.playerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, times(1)).getPetsFromUUIDs(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "This is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " and belongs to " + ChatColor.WHITE + "MockPlayerName");
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't attempt to display pet owner if clicking with offhand")
    void doesNotAttemptToDisplayWhenClickingWithOffhand() {
        when(this.playerInteractEntityEvent.getHand()).thenReturn(EquipmentSlot.OFF_HAND);

        this.playerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).getPetsFromUUIDs(anyString(), anyString());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't attempt to display pet owner if player isn't sneaking")
    void doesNotAttemptToDisplayWhenNotSneaking() {
        when(this.player.isSneaking()).thenReturn(false);

        this.playerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).getPetsFromUUIDs(anyString(), anyString());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't attempt to display pet owner if player isn't clicking with the correct material")
    void doesNotAttemptToDisplayInvalidMaterial() {
        when(this.toolsManager.isMaterialValidTool("get_owner", Material.BONE)).thenReturn(false);

        this.playerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).getPetsFromUUIDs(anyString(), anyString());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet type is not tracked by TPPets")
    void displaysNoOwnerInvalidPetType() {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", Villager.class);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(villager);

        this.playerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).getPetsFromUUIDs(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "This pet doesn't have an owner");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet is not tamed")
    void displaysNoOwnerNotOwned() {
        when(this.horse.isTamed()).thenReturn(false);

        this.playerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).getPetsFromUUIDs(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "This pet doesn't have an owner");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet has no owner")
    void displaysNoOwnerNoOwner() {
        when(this.horse.getOwner()).thenReturn(null);

        this.playerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).getPetsFromUUIDs(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "This pet doesn't have an owner");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays db fail if database fails")
    void displaysDbFailWhenDbFails() {
        when(this.dbWrapper.getPetsFromUUIDs("MockHorseId", "MockPlayerId")).thenReturn(null);

        this.playerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, times(1)).getPetsFromUUIDs(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not get pet data");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays db fail if database can't find the pet in the database")
    void displaysDbFailCantFindPet() {
        when(this.dbWrapper.getPetsFromUUIDs("MockHorseId", "MockPlayerId")).thenReturn(new ArrayList<>());

        this.playerInteractPetExamine.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, times(1)).getPetsFromUUIDs(anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not get pet data");
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }
}
