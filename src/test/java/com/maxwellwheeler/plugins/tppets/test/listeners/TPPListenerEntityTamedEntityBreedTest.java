package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerEntityTamed;
import com.maxwellwheeler.plugins.tppets.storage.PetLimitChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.mockito.Mockito.*;

public class TPPListenerEntityTamedEntityBreedTest {
    private Player owner;
    private Horse horse;
    private TPPets tpPets;
    private SQLWrapper sqlWrapper;
    private ListenerEntityTamed listenerEntityTamed;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.owner = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.owner);
        this.sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.sqlWrapper, logWrapper, false, false);
        PetLimitChecker petIndex = new PetLimitChecker(this.tpPets, 1, 1, 1, 1, 1, 1, 1, 1);

        this.listenerEntityTamed = new ListenerEntityTamed(this.tpPets);

        when(this.tpPets.getPetIndex()).thenReturn(petIndex);
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(0);
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE)).thenReturn(0);
        when(this.sqlWrapper.generateUniquePetName("MockPlayerId", PetType.Pets.HORSE)).thenReturn("MockPetName");
        when(this.sqlWrapper.insertPet(this.horse, "MockPlayerId", "MockPetName")).thenReturn(true);
    }

    private EntityBreedEvent getEntityBreedEvent() {
        EntityBreedEvent entityBreedEvent = mock(EntityBreedEvent.class);
        when(entityBreedEvent.isCancelled()).thenReturn(false);
        when(entityBreedEvent.getEntity()).thenReturn(this.horse);
        return entityBreedEvent;
    }

    @Test
    @DisplayName("Allows taming when within limits")
    void allowsEntityBreeding() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + " [new name]");
        verify(this.sqlWrapper, times(1)).getNumPets("MockPlayerId");
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE);
        verify(this.sqlWrapper, times(1)).insertPet(this.horse, "MockPlayerId", "MockPetName");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows bypassing total tame limit with online permission")
    void allowsEntityBreedingOnlineBypassingTotalLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(10);
        when(this.owner.hasPermission("tppets.bypasslimit")).thenReturn(true);

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + " [new name]");
        verify(this.sqlWrapper, never()).getNumPets("MockPlayerId");
        verify(this.sqlWrapper, never()).getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE);
        verify(this.sqlWrapper, times(1)).insertPet(this.horse, "MockPlayerId", "MockPetName");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows bypassing total tame limit with offline permission")
    void allowsEntityBreedingOfflineBypassingTotalLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();

        OfflinePlayer offlineOwner = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
        when(this.horse.getOwner()).thenReturn(offlineOwner);

        World world = mock(World.class);
        when(world.getName()).thenReturn("MockWorldName");
        when(this.horse.getWorld()).thenReturn(world);
        when(this.tpPets.getVaultEnabled()).thenReturn(true);
        Permission permission = mock(Permission.class);
        when(permission.playerHas("MockWorldName", offlineOwner, "tppets.bypasslimit")).thenReturn(true);
        when(this.tpPets.getPerms()).thenReturn(permission);
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(10);

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.sqlWrapper, never()).getNumPets("MockPlayerId");
        verify(this.sqlWrapper, never()).getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE);
        verify(this.sqlWrapper, times(1)).insertPet(this.horse, "MockPlayerId", "MockPetName");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows bypassing specific tame limit with offline permission")
    void allowsEntityBreedingOfflineBypassingSpecificLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();

        OfflinePlayer offlineOwner = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
        when(this.horse.getOwner()).thenReturn(offlineOwner);

        World world = mock(World.class);
        when(world.getName()).thenReturn("MockWorldName");
        when(this.horse.getWorld()).thenReturn(world);
        when(this.tpPets.getVaultEnabled()).thenReturn(true);
        Permission permission = mock(Permission.class);
        when(permission.playerHas("MockWorldName", offlineOwner, "tppets.bypasslimit")).thenReturn(true);
        when(this.tpPets.getPerms()).thenReturn(permission);

        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE)).thenReturn(10);

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.sqlWrapper, never()).getNumPets("MockPlayerId");
        verify(this.sqlWrapper, never()).getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE);
        verify(this.sqlWrapper, times(1)).insertPet(this.horse, "MockPlayerId", "MockPetName");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows bypassing horse tame limit with online permission")
    void allowsEntityBreedingOnlineBypassingSpecificLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE)).thenReturn(10);
        when(this.owner.hasPermission("tppets.bypasslimit")).thenReturn(true);

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockPetName" + ChatColor.BLUE + " [new name]");
        verify(this.sqlWrapper, never()).getNumPets("MockPlayerId");
        verify(this.sqlWrapper, never()).getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE);
        verify(this.sqlWrapper, times(1)).insertPet(this.horse, "MockPlayerId", "MockPetName");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't process if event is already cancelled")
    void cannotProcessEntityBreedingIfEventCancelled() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(entityBreedEvent.isCancelled()).thenReturn(true);

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.sqlWrapper, never()).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't process if owner is not an OfflinePlayer")
    void cannotProcessEntityBreedingIfOwnerNotOfflinePlayer() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        AnimalTamer animalTamer = mock(AnimalTamer.class);
        when(this.horse.getOwner()).thenReturn(animalTamer);

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.sqlWrapper, never()).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't process if entity is not Tameable")
    void cannotProcessEntityBreedingIfEntityNotTameable() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        LivingEntity entity = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Villager.class);
        when(entityBreedEvent.getEntity()).thenReturn(entity);

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.sqlWrapper, never()).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Reports failure to meet total limit")
    void reportsEntityBreedingTotalLimitExceeded() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenReturn(1);

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for total pets! Limit: 1");
        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports failure to meet pet limit")
    void reportsEntityBreedingSpecificLimitExceeded() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE)).thenReturn(1);

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for this pet type! Horse Limit: 1");
        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when getting total limit")
    void reportsEntityBreedingDbFailGettingTotalLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.sqlWrapper.getNumPets("MockPlayerId")).thenThrow(new SQLException());

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, never()).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when getting specific limit")
    void reportsEntityBreedingDbFailGettingSpecificLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.sqlWrapper.getNumPetsByPetType("MockPlayerId", PetType.Pets.HORSE)).thenThrow(new SQLException());

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when generating new pet name")
    void reportsEntityBreedingDbFailGeneratingPetName() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.sqlWrapper.generateUniquePetName("MockPlayerId", PetType.Pets.HORSE)).thenThrow(new SQLException());

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when inserting new pet into database")
    void reportsEntityBreedingDbFailInsertingPet() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.sqlWrapper.insertPet(this.horse, "MockPlayerId", "MockPetName")).thenThrow(new SQLException());

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, times(1)).insertPet(any(Entity.class), anyString(), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Stands up sitting pets")
    void entityBreedingStandsSittingPets() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        Wolf wolf = MockFactory.getTamedMockEntity("MockWolfId", Wolf.class, this.owner);
        when(entityBreedEvent.getEntity()).thenReturn(wolf);

        // Causes error and for pet to untame
        when(this.sqlWrapper.generateUniquePetName("MockPlayerId", PetType.Pets.DOG)).thenReturn("MockPetName");
        when(this.sqlWrapper.insertPet(wolf, "MockPlayerId", "MockPetName")).thenThrow(new SQLException());

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, times(1)).insertPet(any(Entity.class), anyString(), anyString());
        verify(wolf, times(1)).setOwner(null);
        verify(wolf, times(1)).setTamed(false);
        verify(wolf, times(1)).setSitting(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't untame skeleton horses")
    void entityBreedingDoesntUntameSkeletonHorse() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        SkeletonHorse skeletonHorse = MockFactory.getTamedMockEntity("MockHorseId", SkeletonHorse.class, this.owner);
        when(entityBreedEvent.getEntity()).thenReturn(skeletonHorse);

        // Causes error and for pet to untame
        when(this.sqlWrapper.insertPet(skeletonHorse, "MockPlayerId", "MockPetName")).thenThrow(new SQLException());

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, times(1)).insertPet(any(Entity.class), anyString(), anyString());
        verify(skeletonHorse, times(1)).setOwner(null);
        verify(skeletonHorse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't untame zombie horses")
    void entityBreedingDoesntUntameZombieHorse() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        ZombieHorse zombieHorse = MockFactory.getTamedMockEntity("MockHorseId", ZombieHorse.class, this.owner);
        when(entityBreedEvent.getEntity()).thenReturn(zombieHorse);

        // Causes error and for pet to untame
        when(this.sqlWrapper.insertPet(zombieHorse, "MockPlayerId", "MockPetName")).thenThrow(new SQLException());

        this.listenerEntityTamed.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.sqlWrapper, times(1)).getNumPets(anyString());
        verify(this.sqlWrapper, times(1)).getNumPetsByPetType(anyString(), any(PetType.Pets.class));
        verify(this.sqlWrapper, times(1)).insertPet(any(Entity.class), anyString(), anyString());
        verify(zombieHorse, times(1)).setOwner(null);
        verify(zombieHorse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }
}