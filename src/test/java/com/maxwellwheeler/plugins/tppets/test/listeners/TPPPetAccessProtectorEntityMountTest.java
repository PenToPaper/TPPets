package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.PetAccessProtector;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spigotmc.event.entity.EntityMountEvent;

import static org.mockito.Mockito.*;

public class TPPPetAccessProtectorEntityMountTest {
    private Player admin;
    private Player owner;
    private Player guest;
    private Player stranger;
    private Horse horse;
    private LogWrapper logWrapper;
    private PetAccessProtector petAccessProtector;

    @BeforeEach
    public void beforeEach() {
        this.admin = MockFactory.getMockPlayer("MockAdminId", "MockAdminName", null, null, new String[]{"tppets.mountother"});
        this.owner = MockFactory.getMockPlayer("MockOwnerId", "MockOwnerName", null, null, new String[]{});
        this.guest = MockFactory.getMockPlayer("MockGuestId", "MockGuestName", null, null, new String[]{});
        this.stranger = MockFactory.getMockPlayer("MockStrangerId", "MockStrangerName", null, null, new String[]{});
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", org.bukkit.entity.Horse.class, this.owner);
        DBWrapper dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(dbWrapper, this.logWrapper, false, false, false);
        this.petAccessProtector = new PetAccessProtector(tpPets);

        when(tpPets.isAllowedToPet("MockHorseId", "MockGuestId")).thenReturn(true);
    }

    EntityMountEvent getEntityMountEvent(Player player, Horse pet) {
        EntityMountEvent entityMountEvent = mock(EntityMountEvent.class);
        when(entityMountEvent.getEntity()).thenReturn(player);
        when(entityMountEvent.getMount()).thenReturn(pet);
        when(entityMountEvent.isCancelled()).thenReturn(false);
        return entityMountEvent;
    }

    @Test
    @DisplayName("Cancels mount when stranger attempts")
    void cannotMountWhenStrangerAttempts() {
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.stranger, this.horse);

        this.petAccessProtector.entityMountProtect(entityMountEvent);

        verify(entityMountEvent, times(1)).setCancelled(true);
        verify(this.stranger, times(1)).sendMessage(ChatColor.RED + "You don't have permission to do that");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("Player with UUID MockStrangerId was denied permission to mount pet MockHorseId");
    }

    @Test
    @DisplayName("Doesn't cancel mount if it's already cancelled")
    void cannotCancelMountIfAlreadyCancelled() {
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.stranger, this.horse);
        when(entityMountEvent.isCancelled()).thenReturn(true);

        this.petAccessProtector.entityMountProtect(entityMountEvent);

        verify(entityMountEvent, never()).setCancelled(anyBoolean());
        verify(this.stranger, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Doesn't cancel mount if it's being mounted by non-player")
    void cannotCancelMountIfMounterNotPlayer() {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.stranger, this.horse);
        when(entityMountEvent.getEntity()).thenReturn(villager);

        this.petAccessProtector.entityMountProtect(entityMountEvent);

        verify(entityMountEvent, never()).setCancelled(anyBoolean());
        verify(this.stranger, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Doesn't cancel mount if it's not tamed")
    void cannotCancelMountIfMountNotTamed() {
        this.horse = MockFactory.getMockEntity("MockHorseId", org.bukkit.entity.Horse.class);
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.stranger, this.horse);

        this.petAccessProtector.entityMountProtect(entityMountEvent);

        verify(entityMountEvent, never()).setCancelled(anyBoolean());
        verify(this.stranger, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Doesn't cancel mount if player has tppets.mountother")
    void cannotCancelMountIfMountedByAdmin() {
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.admin, this.horse);

        this.petAccessProtector.entityMountProtect(entityMountEvent);

        verify(entityMountEvent, never()).setCancelled(anyBoolean());
        verify(this.admin, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Doesn't cancel mount if player is the pet's owner")
    void cannotCancelMountIfMountedByOwner() {
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.owner, this.horse);

        this.petAccessProtector.entityMountProtect(entityMountEvent);

        verify(entityMountEvent, never()).setCancelled(anyBoolean());
        verify(this.owner, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
    }

    @Test
    @DisplayName("Doesn't cancel mount if player is allowed to the pet")
    void cannotCancelMountIfMountedByGuest() {
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.guest, this.horse);

        this.petAccessProtector.entityMountProtect(entityMountEvent);

        verify(entityMountEvent, never()).setCancelled(anyBoolean());
        verify(this.guest, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logUnsuccessfulAction(anyString());
    }
}
