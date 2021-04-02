package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerEntityTamed;
import com.maxwellwheeler.plugins.tppets.storage.PetLimitChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.spigotmc.event.entity.EntityMountEvent;

import java.sql.SQLException;

import static org.mockito.Mockito.*;

public class TPPListenerEntityTamedEntityMountTest {
    private Player player;
    private SkeletonHorse skeletonHorse;
    private ListenerEntityTamed listenerEntityTamed;
    private SQLWrapper sqlWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.skeletonHorse = MockFactory.getMockEntity("MockHorseId", org.bukkit.entity.SkeletonHorse.class);
        this.sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, logWrapper, false, false, false);

        PetLimitChecker petIndex = new PetLimitChecker(tpPets, 1, 1, 1, 1, 1, 1, 1, 1);
        when(tpPets.getPetIndex()).thenReturn(petIndex);
        when(tpPets.getVaultEnabled()).thenReturn(false);
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(0);
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE)).thenReturn(0);
        when(this.sqlWrapper.generateUniquePetName("MockPlayerId", PetType.Pets.HORSE)).thenReturn("MockHorseName");
        when(this.sqlWrapper.insertPet(this.skeletonHorse, "MockPlayerId", "MockHorseName")).thenReturn(true);

        this.listenerEntityTamed = new ListenerEntityTamed(tpPets);
    }

    private EntityMountEvent getEntityMountEvent(Entity mounting, Entity mount) {
        EntityMountEvent entityMountEvent = mock(EntityMountEvent.class);
        when(entityMountEvent.getEntity()).thenReturn(mounting);
        when(entityMountEvent.getMount()).thenReturn(mount);
        when(entityMountEvent.isCancelled()).thenReturn(false);
        return entityMountEvent;
    }

    @Test
    @DisplayName("Allows skeleton horse taming")
    void allowsSkeletonHorseTaming() throws SQLException {
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, times(1)).insertPet(any(Entity.class), anyString(), "MockHorseName");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.skeletonHorse, times(1)).setTamed(true);
        verify(this.skeletonHorse, times(1)).setOwner(this.player);
    }

    @Test
    @DisplayName("Allows zombie horse taming")
    void allowsZombieHorseTaming() throws SQLException {
        ZombieHorse zombieHorse = MockFactory.getMockEntity("MockZombieHorse", org.bukkit.entity.ZombieHorse.class);
        when(this.sqlWrapper.insertPet(zombieHorse, "MockPlayerId", "MockHorseName")).thenReturn(true);
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, zombieHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, times(1)).insertPet(any(Entity.class), anyString(), "MockHorseName");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(zombieHorse, times(1)).setTamed(true);
        verify(zombieHorse, times(1)).setOwner(this.player);
    }

    @Test
    @DisplayName("Doesn't allow special horse taming if event is already cancelled")
    void doesNotAllowSpecialHorseTamingIfEventCancelled() throws SQLException {
        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);
        when(entityMountEvent.isCancelled()).thenReturn(true);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, never()).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.skeletonHorse, never()).setOwner(any(AnimalTamer.class));
        verify(entityMountEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't allow special horse taming if mounting entity isn't a player")
    void doesNotAllowSpecialHorseTamingIfEntityNotPlayer() throws SQLException {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        EntityMountEvent entityMountEvent = getEntityMountEvent(villager, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, never()).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(villager, never()).sendMessage(anyString());
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.skeletonHorse, never()).setOwner(any(AnimalTamer.class));
        verify(entityMountEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't allow special horse taming if mounting entity isn't a player")
    void doesNotAllowSpecialHorseTamingIfNotSkeletonHorse() throws SQLException {
        Horse horse = MockFactory.getMockEntity("MockHorseId", org.bukkit.entity.Horse.class);
        EntityMountEvent entityMountEvent = getEntityMountEvent(horse, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, never()).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(horse, never()).sendMessage(anyString());
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.skeletonHorse, never()).setOwner(any(AnimalTamer.class));
        verify(entityMountEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't allow special horse taming if horse is already tamed")
    void doesNotAllowSpecialHorseTamingIfHorseAlreadyTamed() throws SQLException {
        when(this.skeletonHorse.isTamed()).thenReturn(true);
        when(this.skeletonHorse.getOwner()).thenReturn(this.player);

        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, never()).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.player, never()).sendMessage(anyString());
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.skeletonHorse, never()).setOwner(any(AnimalTamer.class));
        verify(entityMountEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows bypassing total tame limit with online permission")
    void allowsEntityTamingOnlineBypassingTotalLimit() throws SQLException {
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(10);
        when(this.player.hasPermission("tppets.bypasslimit")).thenReturn(true);

        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, never()).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, times(1)).insertPet(any(Entity.class), anyString(), "MockHorseName");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.skeletonHorse, times(1)).setTamed(true);
        verify(this.skeletonHorse, times(1)).setOwner(this.player);
    }

    // NOTE: Doesn't need to allow offline permissions since only online players can mount

    @Test
    @DisplayName("Allows bypassing horse tame limit with online permission")
    void allowsEntityTamingOnlineBypassingSpecificLimit() throws SQLException {
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE)).thenReturn(10);
        when(this.player.hasPermission("tppets.bypasslimit")).thenReturn(true);

        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, never()).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, times(1)).insertPet(any(Entity.class), anyString(), "MockHorseName");
        verify(this.player, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.skeletonHorse, times(1)).setTamed(true);
        verify(this.skeletonHorse, times(1)).setOwner(this.player);
    }

    @Test
    @DisplayName("Reports failure to meet total limit")
    void reportsEntityTamingTotalLimitExceeded() throws SQLException {
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(10);

        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for total pets! Limit: 1");
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.skeletonHorse, never()).setOwner(this.player);
        verify(entityMountEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports failure to meet specific limit")
    void reportsEntityTamingSpecificLimitExceeded() throws SQLException {
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE)).thenReturn(10);

        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for this pet type! Horse Limit: 1");
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.skeletonHorse, never()).setOwner(this.player);
        verify(entityMountEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when getting total limit")
    void reportsEntityTamingDbFailGettingTotalLimit() throws SQLException {
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenThrow(new SQLException());

        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.skeletonHorse, never()).setOwner(this.player);
        verify(entityMountEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when getting specific limit")
    void reportsEntityTamingDbFailGettingSpecificLimit() throws SQLException {
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE)).thenThrow(new SQLException());

        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.skeletonHorse, never()).setOwner(this.player);
        verify(entityMountEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when inserting new pet into database")
    void reportsEntityTamingDbFailInsertingPet() throws SQLException {
        when(this.sqlWrapper.insertPet(this.skeletonHorse, "MockPlayerId", "MockHorseName")).thenThrow(new SQLException());

        EntityMountEvent entityMountEvent = getEntityMountEvent(this.player, this.skeletonHorse);

        this.listenerEntityTamed.entityMountTameSpecialHorse(entityMountEvent);

        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, times(1)).insertPet(any(Entity.class), anyString(), "MockHorseName");
        verify(this.player, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
        verify(this.skeletonHorse, never()).setOwner(this.player);
        verify(entityMountEvent, times(1)).setCancelled(true);
    }
}
