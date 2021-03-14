package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.PetInventoryProtector;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.block.Chest;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.LlamaInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class TPPPetInventoryProtectorTest {
    private Player player;
    private Player guest;
    private Donkey donkey;
    private LogWrapper logWrapper;
    private Inventory inventory;
    private TPPets thisPlugin;
    private PetInventoryProtector petInventoryProtector;

    @BeforeEach
    public void beforeEach() {
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.guest = MockFactory.getMockPlayer("MockGuestId", "MockGuestName", null, null, new String[]{});
        this.donkey = MockFactory.getTamedMockEntity("MockPetId", Donkey.class, this.player);
        this.inventory = mock(LlamaInventory.class);
        when(this.inventory.getHolder()).thenReturn(this.donkey);

        DBWrapper dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.thisPlugin = MockFactory.getMockPlugin(dbWrapper, this.logWrapper, true, false, false);

        when(this.player.hasPermission("tppets.mountother")).thenReturn(false);

        this.petInventoryProtector = new PetInventoryProtector(this.thisPlugin);
    }

    InventoryClickEvent getInventoryClickEvent(Player sender) {
        InventoryClickEvent inventoryClickEvent = mock(InventoryClickEvent.class);
        when(inventoryClickEvent.getInventory()).thenReturn(this.inventory);
        when(inventoryClickEvent.getWhoClicked()).thenReturn(sender);

        return inventoryClickEvent;
    }

    InventoryOpenEvent getInventoryOpenEvent(Player sender) {
        InventoryOpenEvent inventoryOpenEvent = mock(InventoryOpenEvent.class);
        when(inventoryOpenEvent.getInventory()).thenReturn(this.inventory);
        when(inventoryOpenEvent.getPlayer()).thenReturn(sender);

        return inventoryOpenEvent;
    }

    @Test
    @DisplayName("Restricts inventory click events when player is not allowed to pet")
    void inventoryClickEventRestrictedPlayerNotAllowedToPet() {
        InventoryClickEvent inventoryClickEvent = getInventoryClickEvent(this.guest);

        this.petInventoryProtector.onInventoryClick(inventoryClickEvent);

        verify(this.guest, times(1)).sendMessage(ChatColor.RED + "You don't have permission to do that");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("Player with UUID MockGuestId was denied permission to access pet MockPetId");
        verify(inventoryClickEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't restrict inventory click events when player has tppets.mountother")
    void inventoryClickEventAllowedMountOtherPerms() {
        when(this.guest.hasPermission("tppets.mountother")).thenReturn(true);

        InventoryClickEvent inventoryClickEvent = getInventoryClickEvent(this.guest);

        this.petInventoryProtector.onInventoryClick(inventoryClickEvent);

        verify(this.guest, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryClickEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't restrict inventory click events when player owns the pet")
    void inventoryClickEventAllowedPlayerOwnsPerms() {
        InventoryClickEvent inventoryClickEvent = getInventoryClickEvent(this.player);

        this.petInventoryProtector.onInventoryClick(inventoryClickEvent);

        verify(this.player, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryClickEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't restrict inventory click events when player is explicitly allowed to the pet")
    void inventoryClickEventAllowedPlayerExplicitlyAllowed() {
        when(this.thisPlugin.isAllowedToPet("MockPetId", "MockGuestId")).thenReturn(true);

        InventoryClickEvent inventoryClickEvent = getInventoryClickEvent(this.guest);

        this.petInventoryProtector.onInventoryClick(inventoryClickEvent);

        verify(this.guest, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryClickEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't restrict inventory click events when inventory clicked is not a pet")
    void inventoryClickEventAllowedClickedChest() {
        Chest chest = mock(Chest.class);
        when(this.inventory.getHolder()).thenReturn(chest);

        InventoryClickEvent inventoryClickEvent = getInventoryClickEvent(this.guest);

        this.petInventoryProtector.onInventoryClick(inventoryClickEvent);

        verify(this.guest, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryClickEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't restrict inventory click events when inventory clicked is not a tracked pet type")
    void inventoryClickEventAllowedUntrackedPetType() {
        AbstractHorse horse = mock(AbstractHorse.class);
        when(this.inventory.getHolder()).thenReturn(horse);

        InventoryClickEvent inventoryClickEvent = getInventoryClickEvent(this.guest);

        this.petInventoryProtector.onInventoryClick(inventoryClickEvent);

        verify(this.guest, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryClickEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Restricts inventory open events when player is not allowed to pet")
    void inventoryOpenEventRestrictedPlayerNotAllowedToPet() {
        InventoryOpenEvent inventoryOpenEvent = getInventoryOpenEvent(this.guest);

        this.petInventoryProtector.onInventoryOpen(inventoryOpenEvent);

        verify(this.guest, times(1)).sendMessage(ChatColor.RED + "You don't have permission to do that");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("Player with UUID MockGuestId was denied permission to access pet MockPetId");
        verify(inventoryOpenEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't restrict inventory open events when player has tppets.mountother")
    void inventoryOpenEventAllowedMountOtherPerms() {
        when(this.guest.hasPermission("tppets.mountother")).thenReturn(true);

        InventoryOpenEvent inventoryOpenEvent = getInventoryOpenEvent(this.guest);

        this.petInventoryProtector.onInventoryOpen(inventoryOpenEvent);

        verify(this.guest, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryOpenEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't restrict inventory open events when player owns the pet")
    void inventoryOpenEventAllowedPlayerOwnsPerms() {
        InventoryOpenEvent inventoryOpenEvent = getInventoryOpenEvent(this.player);

        this.petInventoryProtector.onInventoryOpen(inventoryOpenEvent);

        verify(this.player, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryOpenEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't restrict inventory open events when player is explicitly allowed to the pet")
    void inventoryOpenEventAllowedPlayerExplicitlyAllowed() {
        when(this.thisPlugin.isAllowedToPet("MockPetId", "MockGuestId")).thenReturn(true);

        InventoryOpenEvent inventoryOpenEvent = getInventoryOpenEvent(this.player);

        this.petInventoryProtector.onInventoryOpen(inventoryOpenEvent);

        verify(this.guest, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryOpenEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't restrict inventory open events when inventory clicked is not a pet")
    void inventoryOpenEventAllowedClickedChest() {
        Chest chest = mock(Chest.class);
        when(this.inventory.getHolder()).thenReturn(chest);

        InventoryOpenEvent inventoryOpenEvent = getInventoryOpenEvent(this.player);

        this.petInventoryProtector.onInventoryOpen(inventoryOpenEvent);

        verify(this.guest, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryOpenEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't restrict inventory open events when inventory clicked is not a tracked pet type")
    void inventoryOpenEventAllowedUntrackedPetType() {
        AbstractHorse horse = mock(AbstractHorse.class);
        when(this.inventory.getHolder()).thenReturn(horse);

        InventoryOpenEvent inventoryOpenEvent = getInventoryOpenEvent(this.player);

        this.petInventoryProtector.onInventoryOpen(inventoryOpenEvent);

        verify(this.guest, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryOpenEvent, never()).setCancelled(anyBoolean());
    }


}
