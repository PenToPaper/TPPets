package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.GuestManager;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerPetAccess;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.block.Chest;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.LlamaInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Hashtable;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPListenerPetAccessInventoryClickTest {
    private Player player;
    private Player guest;
    private LogWrapper logWrapper;
    private Inventory inventory;
    private TPPets tpPets;
    private ListenerPetAccess petInventoryProtector;
    private SQLWrapper sqlWrapper;

    @BeforeEach
    public void beforeEach() {
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.guest = MockFactory.getMockPlayer("MockGuestId", "MockGuestName", null, null, new String[]{});
        Donkey donkey = MockFactory.getTamedMockEntity("MockPetId", Donkey.class, this.player);
        this.inventory = mock(LlamaInventory.class);
        when(this.inventory.getHolder()).thenReturn(donkey);

        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, false);

        when(this.player.hasPermission("tppets.mountother")).thenReturn(false);

        this.petInventoryProtector = new ListenerPetAccess(this.tpPets);
    }

    InventoryClickEvent getInventoryClickEvent(Player sender) {
        InventoryClickEvent inventoryClickEvent = mock(InventoryClickEvent.class);
        when(inventoryClickEvent.getInventory()).thenReturn(this.inventory);
        when(inventoryClickEvent.getWhoClicked()).thenReturn(sender);

        return inventoryClickEvent;
    }

    @Test
    @DisplayName("Restricts inventory click events when player is not allowed to pet")
    void inventoryClickEventRestrictedPlayerNotAllowedToPet() {
        InventoryClickEvent inventoryClickEvent = getInventoryClickEvent(this.guest);

        this.petInventoryProtector.onInventoryClick(inventoryClickEvent);

        verify(this.guest, times(1)).sendMessage(ChatColor.RED + "You don't have permission to do that");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("MockGuestName - inventory - INSUFFICIENT_PERMISSIONS");
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
    void inventoryClickEventGuestOwnsPerms() {
        InventoryClickEvent inventoryClickEvent = getInventoryClickEvent(this.player);

        this.petInventoryProtector.onInventoryClick(inventoryClickEvent);

        verify(this.player, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
        verify(inventoryClickEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't restrict inventory click events when player is explicitly allowed to the pet")
    void inventoryClickEventGuestExplicitlyAllowed() throws SQLException {
        when(this.sqlWrapper.getAllGuests()).thenReturn(new Hashtable<>());
        GuestManager guestManager = new GuestManager(this.sqlWrapper);
        guestManager.addGuest("MockPetId", "MockGuestId");

        when(this.tpPets.getGuestManager()).thenReturn(guestManager);

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
}
