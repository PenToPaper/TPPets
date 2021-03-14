package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.ToolsManager;
import com.maxwellwheeler.plugins.tppets.listeners.PlayerInteractPetRelease;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class TPPPlayerInteractPetReleaseTest {
    private PlayerInteractEntityEvent playerInteractEntityEvent;
    private PlayerInteractPetRelease playerInteractPetRelease;
    private Horse horse;
    private Player player;
    private ToolsManager toolsManager;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() {
        this.playerInteractEntityEvent = mock(PlayerInteractEntityEvent.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.toolsManager = mock(ToolsManager.class);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        TPPets tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, false, false, false);
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.player);

        EquipmentSlot playerHand = EquipmentSlot.HAND;
        this.playerInteractPetRelease = new PlayerInteractPetRelease(tpPets);

        PlayerInventory playerInventory = mock(PlayerInventory.class);
        ItemStack itemStack = mock(ItemStack.class);

        when(this.playerInteractEntityEvent.getHand()).thenReturn(playerHand);
        when(this.playerInteractEntityEvent.getPlayer()).thenReturn(this.player);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(this.horse);
        when(this.player.isSneaking()).thenReturn(true);
        when(this.player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getItemInMainHand()).thenReturn(itemStack);
        when(itemStack.getType()).thenReturn(Material.SHEARS);
        when(tpPets.getToolsManager()).thenReturn(this.toolsManager);
        when(this.toolsManager.isMaterialValidTool("untame_pets", Material.SHEARS)).thenReturn(true);
        when(this.dbWrapper.deletePet(this.horse)).thenReturn(true);
    }

    @Test
    @DisplayName("Releases pet")
    void releasesPet() {
        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, times(1)).deletePet(this.horse);
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("Player MockPlayerName untamed entity MockHorseId");
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Releases pet with tppets.untameother")
    void releasesPetUntameOther() {
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(this.horse.getOwner()).thenReturn(offlinePlayer);
        when(this.player.hasPermission("tppets.untameother")).thenReturn(true);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, times(1)).deletePet(this.horse);
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("Player MockPlayerName untamed entity MockHorseId");
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Releases standable pet")
    void releasesStandablePet() {
        Wolf wolf = MockFactory.getTamedMockEntity("MockWolfId", Wolf.class, this.player);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(wolf);
        when(this.dbWrapper.deletePet(wolf)).thenReturn(true);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, times(1)).deletePet(wolf);
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("Player MockPlayerName untamed entity MockWolfId");
        verify(wolf, times(1)).setSitting(false);
        verify(wolf, times(1)).setOwner(null);
        verify(wolf, times(1)).setTamed(false);
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Releases skeleton horse pet without setting tamed = false")
    void releasesSkeletonHorsePet() {
        SkeletonHorse skeletonHorse = MockFactory.getTamedMockEntity("MockSkeletonHorseId", SkeletonHorse.class, this.player);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(skeletonHorse);
        when(this.dbWrapper.deletePet(skeletonHorse)).thenReturn(true);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, times(1)).deletePet(skeletonHorse);
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("Player MockPlayerName untamed entity MockSkeletonHorseId");
        verify(skeletonHorse, times(1)).setOwner(null);
        verify(skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Releases zombie horse pet without setting tamed = false")
    void releasesZombieHorsePet() {
        ZombieHorse zombieHorse = MockFactory.getTamedMockEntity("MockZombieHorseId", ZombieHorse.class, this.player);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(zombieHorse);
        when(this.dbWrapper.deletePet(zombieHorse)).thenReturn(true);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, times(1)).deletePet(zombieHorse);
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("Player MockPlayerName untamed entity MockZombieHorseId");
        verify(zombieHorse, times(1)).setOwner(null);
        verify(zombieHorse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't attempt to release pet if clicking with offhand")
    void doesNotAttemptToReleaseWhenClickingWithOffhand() {
        when(this.playerInteractEntityEvent.getHand()).thenReturn(EquipmentSlot.OFF_HAND);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).deletePet(any());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't attempt to release pet if player isn't sneaking")
    void doesNotAttemptToReleaseWhenNotSneaking() {
        when(this.player.isSneaking()).thenReturn(false);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).deletePet(any());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't attempt to release pet if player isn't clicking with the correct material")
    void doesNotAttemptToReleaseInvalidMaterial() {
        when(this.toolsManager.isMaterialValidTool("untame_pets", Material.SHEARS)).thenReturn(false);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).deletePet(any());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet type is not tracked by TPPets")
    void displaysNoOwnerInvalidPetType() {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", Villager.class);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(villager);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).deletePet(any());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "This pet doesn't have an owner");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet is not tamed")
    void displaysNoOwnerNotOwned() {
        when(this.horse.isTamed()).thenReturn(false);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).deletePet(any());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "This pet doesn't have an owner");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet has no owner")
    void displaysNoOwnerNoOwner() {
        when(this.horse.getOwner()).thenReturn(null);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).deletePet(any());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "This pet doesn't have an owner");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays insufficient permissions if player isn't the owner of the pet and isn't an admin")
    void displaysInsufficientPermissionsIfNotOwnerAndNotAdmin() {
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(this.horse.getOwner()).thenReturn(offlinePlayer);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, never()).deletePet(any());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "You don't have permission to do that");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays db fail if database fails")
    void displaysDbFailWhenDbFails() {
        when(this.dbWrapper.deletePet(this.horse)).thenReturn(false);

        this.playerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.dbWrapper, times(1)).deletePet(this.horse);
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not release pet");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }
}
