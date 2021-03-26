package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerEntityTamed;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetLimitChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityTameEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TPPListenerEntityTamedEntityTamedEventTest {
    private Player owner;
    private Horse horse;
    private TPPets tpPets;
    private DBWrapper dbWrapper;
    private ListenerEntityTamed listenerEntityTamed;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.owner = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.owner);
        this.dbWrapper = mock(DBWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, logWrapper, false, false, false);
        PetLimitChecker petIndex = new PetLimitChecker(this.tpPets, 1, 1, 1, 1, 1, 1, 1, 1);

        this.listenerEntityTamed = new ListenerEntityTamed(this.tpPets);

        when(this.tpPets.getPetIndex()).thenReturn(petIndex);
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenReturn(0);
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenReturn(0);
        when(this.dbWrapper.insertPet(this.horse, "MockPlayerId")).thenReturn("MockHorseName");
    }

    private EntityTameEvent getEntityTameEvent() {
        EntityTameEvent entityTameEvent = mock(EntityTameEvent.class);
        when(entityTameEvent.isCancelled()).thenReturn(false);
        when(entityTameEvent.getOwner()).thenReturn(this.owner);
        when(entityTameEvent.getEntity()).thenReturn(this.horse);
        return entityTameEvent;
    }

    @Test
    @DisplayName("Allows taming when within limits")
    void allowsEntityTaming() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.dbWrapper, times(1)).getNumPets("MockPlayerId");
        verify(this.dbWrapper, times(1)).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows bypassing total tame limit with online permission")
    void allowsEntityTamingOnlineBypassingTotalLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenReturn(10);
        when(this.owner.hasPermission("tppets.bypasslimit")).thenReturn(true);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows bypassing total tame limit with offline permission")
    void allowsEntityTamingOfflineBypassingTotalLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();

        OfflinePlayer offlineOwner = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
        when(this.horse.getOwner()).thenReturn(offlineOwner);
        when(entityTameEvent.getOwner()).thenReturn(offlineOwner);

        World world = mock(World.class);
        when(world.getName()).thenReturn("MockWorldName");
        when(this.horse.getWorld()).thenReturn(world);
        when(this.tpPets.getVaultEnabled()).thenReturn(true);
        Permission permission = mock(Permission.class);
        when(permission.playerHas("MockWorldName", offlineOwner, "tppets.bypasslimit")).thenReturn(true);
        when(this.tpPets.getPerms()).thenReturn(permission);
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenReturn(10);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows bypassing specific tame limit with offline permission")
    void allowsEntityTamingOfflineBypassingSpecificLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();

        OfflinePlayer offlineOwner = MockFactory.getMockOfflinePlayer("MockPlayerId", "MockPlayerName");
        when(this.horse.getOwner()).thenReturn(offlineOwner);
        when(entityTameEvent.getOwner()).thenReturn(offlineOwner);

        World world = mock(World.class);
        when(world.getName()).thenReturn("MockWorldName");
        when(this.horse.getWorld()).thenReturn(world);
        when(this.tpPets.getVaultEnabled()).thenReturn(true);
        Permission permission = mock(Permission.class);
        when(permission.playerHas("MockWorldName", offlineOwner, "tppets.bypasslimit")).thenReturn(true);
        when(this.tpPets.getPerms()).thenReturn(permission);

        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenReturn(10);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Allows bypassing horse tame limit with online permission")
    void allowsEntityTamingOnlineBypassingSpecificLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenReturn(10);
        when(this.owner.hasPermission("tppets.bypasslimit")).thenReturn(true);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't process if event is already cancelled")
    void cannotProcessEntityTamingIfEventCancelled() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(entityTameEvent.isCancelled()).thenReturn(true);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't process if owner is not an OfflinePlayer")
    void cannotProcessEntityTamingIfOwnerNotOfflinePlayer() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        AnimalTamer animalTamer = mock(AnimalTamer.class);
        when(entityTameEvent.getOwner()).thenReturn(animalTamer);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Doesn't process if entity is not Tameable")
    void cannotProcessEntityTamingIfEntityNotTameable() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        Entity entity = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Villager.class);
        when(entityTameEvent.getEntity()).thenReturn((LivingEntity) entity);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("Reports failure to meet total limit")
    void reportsEntityTamingTotalLimitExceeded() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenReturn(1);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for total pets! Limit: 1");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports failure to meet pet limit")
    void reportsEntityTamingSpecificLimitExceeded() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenReturn(1);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for this pet type! Horse Limit: 1");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when getting total limit")
    void reportsEntityTamingDbFailGettingTotalLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenThrow(new SQLException());

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when getting specific limit")
    void reportsEntityTamingDbFailGettingSpecificLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenThrow(new SQLException());

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Reports database failure when inserting new pet into database")
    void reportsEntityTamingDbFailInsertingPet() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.insertPet(this.horse, "MockPlayerId")).thenReturn(null);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Stands up sitting pets")
    void entityTamingStandsSittingPets() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        Wolf wolf = MockFactory.getTamedMockEntity("MockWolfId", Wolf.class, this.owner);
        when(entityTameEvent.getEntity()).thenReturn(wolf);

        // Causes error and for pet to untame
        when(this.dbWrapper.insertPet(wolf, "MockPlayerId")).thenReturn(null);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(wolf, times(1)).setOwner(null);
        verify(wolf, times(1)).setTamed(false);
        verify(wolf, times(1)).setSitting(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't untame skeleton horses")
    void entityTamingDoesntUntameSkeletonHorse() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        SkeletonHorse skeletonHorse = MockFactory.getTamedMockEntity("MockHorseId", SkeletonHorse.class, this.owner);
        when(entityTameEvent.getEntity()).thenReturn(skeletonHorse);

        // Causes error and for pet to untame
        when(this.dbWrapper.insertPet(skeletonHorse, "MockPlayerId")).thenReturn(null);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(skeletonHorse, times(1)).setOwner(null);
        verify(skeletonHorse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("Doesn't untame zombie horses")
    void entityTamingDoesntUntameZombieHorse() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        ZombieHorse zombieHorse = MockFactory.getTamedMockEntity("MockHorseId", ZombieHorse.class, this.owner);
        when(entityTameEvent.getEntity()).thenReturn(zombieHorse);

        // Causes error and for pet to untame
        when(this.dbWrapper.insertPet(zombieHorse, "MockPlayerId")).thenReturn(null);

        this.listenerEntityTamed.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(zombieHorse, times(1)).setOwner(null);
        verify(zombieHorse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, times(1)).setCancelled(true);
    }
}