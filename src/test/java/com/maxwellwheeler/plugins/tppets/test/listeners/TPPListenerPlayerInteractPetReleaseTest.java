package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.ToolsManager;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerPlayerInteractPetRelease;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
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

import java.sql.SQLException;

import static org.mockito.Mockito.*;

public class TPPListenerPlayerInteractPetReleaseTest {
    private PlayerInteractEntityEvent playerInteractEntityEvent;
    private ListenerPlayerInteractPetRelease listenerPlayerInteractPetRelease;
    private Horse horse;
    private Player player;
    private ToolsManager toolsManager;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private TPPets tpPets;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.playerInteractEntityEvent = mock(PlayerInteractEntityEvent.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.toolsManager = mock(ToolsManager.class);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, false);
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.player);

        EquipmentSlot playerHand = EquipmentSlot.HAND;
        this.listenerPlayerInteractPetRelease = new ListenerPlayerInteractPetRelease(this.tpPets);

        PlayerInventory playerInventory = mock(PlayerInventory.class);
        ItemStack itemStack = mock(ItemStack.class);

        when(this.tpPets.getAllowUntamingPets()).thenReturn(true);
        when(this.playerInteractEntityEvent.getHand()).thenReturn(playerHand);
        when(this.playerInteractEntityEvent.getPlayer()).thenReturn(this.player);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(this.horse);
        when(this.player.isSneaking()).thenReturn(true);
        when(this.player.getInventory()).thenReturn(playerInventory);
        when(playerInventory.getItemInMainHand()).thenReturn(itemStack);
        when(itemStack.getType()).thenReturn(Material.SHEARS);
        when(this.tpPets.getToolsManager()).thenReturn(this.toolsManager);
        when(this.toolsManager.isMaterialValidTool("release_pets", Material.SHEARS)).thenReturn(true);
        when(this.sqlWrapper.removePet("MockHorseId")).thenReturn(true);
    }

    @Test
    @DisplayName("Releases pet")
    void releasesPet() throws SQLException {
        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockHorseId");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - release tool - MockHorseId");
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Releases pet with tppets.releaseother")
    void releasesPetUntameOther() throws SQLException {
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(this.horse.getOwner()).thenReturn(offlinePlayer);
        when(this.player.hasPermission("tppets.releaseother")).thenReturn(true);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockHorseId");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - release tool - MockHorseId");
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Releases standable pet")
    void releasesStandablePet() throws SQLException {
        Wolf wolf = MockFactory.getTamedMockEntity("MockWolfId", Wolf.class, this.player);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(wolf);
        when(this.sqlWrapper.removePet("MockWolfId")).thenReturn(true);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockWolfId");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - release tool - MockWolfId");
        verify(wolf, times(1)).setSitting(false);
        verify(wolf, times(1)).setOwner(null);
        verify(wolf, times(1)).setTamed(false);
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Releases skeleton horse pet without setting tamed = false")
    void releasesSkeletonHorsePet() throws SQLException {
        SkeletonHorse skeletonHorse = MockFactory.getTamedMockEntity("MockSkeletonHorseId", SkeletonHorse.class, this.player);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(skeletonHorse);
        when(this.sqlWrapper.removePet("MockSkeletonHorseId")).thenReturn(true);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockSkeletonHorseId");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - release tool - MockSkeletonHorseId");
        verify(skeletonHorse, times(1)).setOwner(null);
        verify(skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Releases zombie horse pet without setting tamed = false")
    void releasesZombieHorsePet() throws SQLException {
        ZombieHorse zombieHorse = MockFactory.getTamedMockEntity("MockZombieHorseId", ZombieHorse.class, this.player);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(zombieHorse);
        when(this.sqlWrapper.removePet("MockZombieHorseId")).thenReturn(true);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockZombieHorseId");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - release tool - MockZombieHorseId");
        verify(zombieHorse, times(1)).setOwner(null);
        verify(zombieHorse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't attempt to release pet if clicking with offhand")
    void doesNotAttemptToReleaseWhenClickingWithOffhand() throws SQLException {
        when(this.playerInteractEntityEvent.getHand()).thenReturn(EquipmentSlot.OFF_HAND);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).removePet(any());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't attempt to release pet if player isn't sneaking")
    void doesNotAttemptToReleaseWhenNotSneaking() throws SQLException {
        when(this.player.isSneaking()).thenReturn(false);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).removePet(any());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't attempt to release pet if player isn't clicking with the correct material")
    void doesNotAttemptToReleaseInvalidMaterial() throws SQLException {
        when(this.toolsManager.isMaterialValidTool("release_pets", Material.SHEARS)).thenReturn(false);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).removePet(any());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't allow releasing pets if config option set")
    void doesNotReleasePetsNoConfigOption() throws SQLException {
        when(this.tpPets.getAllowUntamingPets()).thenReturn(false);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).removePet("MockHorseId");
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "You can't release pets");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("MockPlayerName - release tool - NOT_ENABLED");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows overriding config option with tppets.releaseother")
    void releasesOverridingConfigOption() throws SQLException {
        when(this.player.hasPermission("tppets.releaseother")).thenReturn(true);
        when(this.tpPets.getAllowUntamingPets()).thenReturn(false);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockHorseId");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "Pet released!");
        verify(this.logWrapper, times(1)).logSuccessfulAction("MockPlayerName - release tool - MockHorseId");
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(this.playerInteractEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Displays no owner if pet type is not tracked by TPPets")
    void displaysNoOwnerInvalidPetType() throws SQLException {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", Villager.class);
        when(this.playerInteractEntityEvent.getRightClicked()).thenReturn(villager);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).removePet(any());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "This pet doesn't have an owner");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("MockPlayerName - release tool - NO_OWNER");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet is not tamed")
    void displaysNoOwnerNotOwned() throws SQLException {
        when(this.horse.isTamed()).thenReturn(false);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).removePet(any());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "This pet doesn't have an owner");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("MockPlayerName - release tool - NO_OWNER");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays no owner if pet has no owner")
    void displaysNoOwnerNoOwner() throws SQLException {
        when(this.horse.getOwner()).thenReturn(null);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).removePet(any());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "This pet doesn't have an owner");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("MockPlayerName - release tool - NO_OWNER");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays insufficient permissions if player isn't the owner of the pet and isn't an admin")
    void displaysInsufficientPermissionsIfNotOwnerAndNotAdmin() throws SQLException {
        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(this.horse.getOwner()).thenReturn(offlinePlayer);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, never()).removePet(any());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "You don't have permission to do that");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("MockPlayerName - release tool - INSUFFICIENT_PERMISSIONS");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Displays db fail if database fails")
    void displaysDbFailWhenDbFails() throws SQLException {
        when(this.sqlWrapper.removePet("MockHorseId")).thenReturn(false);

        this.listenerPlayerInteractPetRelease.onPlayerInteractEntity(this.playerInteractEntityEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockHorseId");
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not release pet");
        verify(this.logWrapper, times(1)).logUnsuccessfulAction("MockPlayerName - release tool - DB_FAIL");
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.horse, never()).setOwner(any());
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(this.playerInteractEntityEvent, never()).setCancelled(anyBoolean());
    }
}
