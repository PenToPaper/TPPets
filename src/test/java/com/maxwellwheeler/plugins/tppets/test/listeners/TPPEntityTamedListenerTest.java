package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.EntityTamedListener;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetLimitChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class TPPEntityTamedListenerTest {
    private Player owner;
    private Horse horse;
    private PetLimitChecker petIndex;
    private TPPets tpPets;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private EntityTamedListener entityTamedListener;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.owner = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.horse = (Horse) MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.owner);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, false, false, false);
        this.petIndex = new PetLimitChecker(this.tpPets, 1, 1, 1, 1, 1,1, 1, 1);

        this.entityTamedListener = new EntityTamedListener(this.tpPets);

        when(this.tpPets.getPetIndex()).thenReturn(this.petIndex);
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

    private EntityBreedEvent getEntityBreedEvent() {
        EntityBreedEvent entityBreedEvent = mock(EntityBreedEvent.class);
        when(entityBreedEvent.isCancelled()).thenReturn(false);
        when(entityBreedEvent.getEntity()).thenReturn(this.horse);
        return entityBreedEvent;
    }

    @Test
    @DisplayName("EntityTameEvent - Allows taming when within limits")
    void allowsEntityTameEvent() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.dbWrapper, times(1)).getNumPets("MockPlayerId");
        verify(this.dbWrapper, times(1)).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityTameEvent - Allows bypassing total tame limit with online permission")
    void allowsEntityTameEventOnlineBypassingTotalLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenReturn(10);
        when(this.owner.hasPermission("tppets.bypasslimit")).thenReturn(true);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityTameEvent - Allows bypassing total tame limit with offline permission")
    void allowsEntityTameEventOfflineBypassingTotalLimit() throws SQLException {
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

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityTameEvent - Allows bypassing specific tame limit with offline permission")
    void allowsEntityTameEventOfflineBypassingSpecificLimit() throws SQLException {
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

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityTameEvent - Allows bypassing horse tame limit with online permission")
    void allowsEntityTameEventOnlineBypassingSpecificLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenReturn(10);
        when(this.owner.hasPermission("tppets.bypasslimit")).thenReturn(true);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityTameEvent - Doesn't process if event is already cancelled")
    void cannotProcessEntityTameEventIfEventCancelled() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(entityTameEvent.isCancelled()).thenReturn(true);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityTameEvent - Doesn't process if owner is not an OfflinePlayer")
    void cannotProcessEntityTameEventIfOwnerNotOfflinePlayer() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        AnimalTamer animalTamer = mock(AnimalTamer.class);
        when(entityTameEvent.getOwner()).thenReturn(animalTamer);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityTameEvent - Doesn't process if entity is not Tameable")
    void cannotProcessEntityTameEventIfEntityNotTameable() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        Entity entity = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Villager.class);
        when(entityTameEvent.getEntity()).thenReturn((LivingEntity) entity);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityTameEvent - Reports failure to meet total limit")
    void reportsEntityTameEventTotalLimitExceeded() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenReturn(1);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for total pets! Limit: 1");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityTameEvent - Reports failure to meet pet limit")
    void reportsEntityTameEventSpecificLimitExceeded() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenReturn(1);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for this pet type! Horse Limit: 1");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityTameEvent - Reports database failure when getting total limit")
    void reportsEntityTameEventDbFailGettingTotalLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenThrow(new SQLException());

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityTameEvent - Reports database failure when getting specific limit")
    void reportsEntityTameEventDbFailGettingSpecificLimit() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenThrow(new SQLException());

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityTameEvent - Reports database failure when inserting new pet into database")
    void reportsEntityTameEventDbFailInsertingPet() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        when(this.dbWrapper.insertPet(this.horse, "MockPlayerId")).thenReturn(null);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityTameEvent - Stands up sitting pets")
    void entityTameEventStandsSittingPets() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        Wolf wolf = (Wolf) MockFactory.getTamedMockEntity("MockWolfId", Wolf.class, this.owner);
        when(entityTameEvent.getEntity()).thenReturn(wolf);

        // Causes error and for pet to untame
        when(this.dbWrapper.insertPet(wolf, "MockPlayerId")).thenReturn(null);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

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
    @DisplayName("EntityTameEvent - Doesn't untame skeleton horses")
    void entityTameEventDoesntUntameSkeletonHorse() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        SkeletonHorse skeletonHorse = (SkeletonHorse) MockFactory.getTamedMockEntity("MockHorseId", SkeletonHorse.class, this.owner);
        when(entityTameEvent.getEntity()).thenReturn(skeletonHorse);

        // Causes error and for pet to untame
        when(this.dbWrapper.insertPet(skeletonHorse, "MockPlayerId")).thenReturn(null);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(skeletonHorse, times(1)).setOwner(null);
        verify(skeletonHorse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityTameEvent - Doesn't untame zombie horses")
    void entityTameEventDoesntUntameZombieHorse() throws SQLException {
        EntityTameEvent entityTameEvent = getEntityTameEvent();
        ZombieHorse zombieHorse = (ZombieHorse) MockFactory.getTamedMockEntity("MockHorseId", ZombieHorse.class, this.owner);
        when(entityTameEvent.getEntity()).thenReturn(zombieHorse);

        // Causes error and for pet to untame
        when(this.dbWrapper.insertPet(zombieHorse, "MockPlayerId")).thenReturn(null);

        this.entityTamedListener.onEntityTameEvent(entityTameEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(zombieHorse, times(1)).setOwner(null);
        verify(zombieHorse, never()).setTamed(anyBoolean());
        verify(entityTameEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityBreedEvent - Allows taming when within limits")
    void allowsEntityBreedEvent() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.dbWrapper, times(1)).getNumPets("MockPlayerId");
        verify(this.dbWrapper, times(1)).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityBreedEvent - Allows bypassing total tame limit with online permission")
    void allowsEntityBreedEventOnlineBypassingTotalLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenReturn(10);
        when(this.owner.hasPermission("tppets.bypasslimit")).thenReturn(true);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityBreedEvent - Allows bypassing total tame limit with offline permission")
    void allowsEntityBreedEventOfflineBypassingTotalLimit() throws SQLException {
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
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenReturn(10);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityBreedEvent - Allows bypassing specific tame limit with offline permission")
    void allowsEntityBreedEventOfflineBypassingSpecificLimit() throws SQLException {
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

        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenReturn(10);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityBreedEvent - Allows bypassing horse tame limit with online permission")
    void allowsEntityBreedEventOnlineBypassingSpecificLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenReturn(10);
        when(this.owner.hasPermission("tppets.bypasslimit")).thenReturn(true);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.BLUE + "You've tamed a pet! Its current name is " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + ". You can rename it with /tpp rename " + ChatColor.WHITE + "MockHorseName" + ChatColor.BLUE + " [new name]");
        verify(this.dbWrapper, never()).getNumPets("MockPlayerId");
        verify(this.dbWrapper, never()).getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE);
        verify(this.dbWrapper, times(1)).insertPet(this.horse, "MockPlayerId");
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityBreedEvent - Doesn't process if event is already cancelled")
    void cannotProcessEntityBreedEventIfEventCancelled() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(entityBreedEvent.isCancelled()).thenReturn(true);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityBreedEvent - Doesn't process if owner is not an OfflinePlayer")
    void cannotProcessEntityBreedEventIfOwnerNotOfflinePlayer() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        AnimalTamer animalTamer = mock(AnimalTamer.class);
        when(this.horse.getOwner()).thenReturn(animalTamer);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityBreedEvent - Doesn't process if entity is not Tameable")
    void cannotProcessEntityBreedEventIfEntityNotTameable() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        Entity entity = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Villager.class);
        when(entityBreedEvent.getEntity()).thenReturn((LivingEntity) entity);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, never()).sendMessage(anyString());
        verify(this.dbWrapper, never()).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, never()).setOwner(any(AnimalTamer.class));
        verify(this.horse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityBreedEvent - Reports failure to meet total limit")
    void reportsEntityBreedEventTotalLimitExceeded() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenReturn(1);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for total pets! Limit: 1");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityBreedEvent - Reports failure to meet pet limit")
    void reportsEntityBreedEventSpecificLimitExceeded() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenReturn(1);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "You've exceeded the limit for this pet type! Horse Limit: 1");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityBreedEvent - Reports database failure when getting total limit")
    void reportsEntityBreedEventDbFailGettingTotalLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.dbWrapper.getNumPets("MockPlayerId")).thenThrow(new SQLException());

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, never()).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityBreedEvent - Reports database failure when getting specific limit")
    void reportsEntityBreedEventDbFailGettingSpecificLimit() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.dbWrapper.getNumPetsByPT("MockPlayerId", PetType.Pets.HORSE)).thenThrow(new SQLException());

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, never()).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityBreedEvent - Reports database failure when inserting new pet into database")
    void reportsEntityBreedEventDbFailInsertingPet() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        when(this.dbWrapper.insertPet(this.horse, "MockPlayerId")).thenReturn(null);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityBreedEvent - Stands up sitting pets")
    void entityBreedEventStandsSittingPets() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        Wolf wolf = (Wolf) MockFactory.getTamedMockEntity("MockWolfId", Wolf.class, this.owner);
        when(entityBreedEvent.getEntity()).thenReturn(wolf);

        // Causes error and for pet to untame
        when(this.dbWrapper.insertPet(wolf, "MockPlayerId")).thenReturn(null);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(wolf, times(1)).setOwner(null);
        verify(wolf, times(1)).setTamed(false);
        verify(wolf, times(1)).setSitting(false);
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityBreedEvent - Doesn't untame skeleton horses")
    void entityBreedEventDoesntUntameSkeletonHorse() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        SkeletonHorse skeletonHorse = (SkeletonHorse) MockFactory.getTamedMockEntity("MockHorseId", SkeletonHorse.class, this.owner);
        when(entityBreedEvent.getEntity()).thenReturn(skeletonHorse);

        // Causes error and for pet to untame
        when(this.dbWrapper.insertPet(skeletonHorse, "MockPlayerId")).thenReturn(null);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(skeletonHorse, times(1)).setOwner(null);
        verify(skeletonHorse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityBreedEvent - Doesn't untame zombie horses")
    void entityBreedEventDoesntUntameZombieHorse() throws SQLException {
        EntityBreedEvent entityBreedEvent = getEntityBreedEvent();
        ZombieHorse zombieHorse = (ZombieHorse) MockFactory.getTamedMockEntity("MockHorseId", ZombieHorse.class, this.owner);
        when(entityBreedEvent.getEntity()).thenReturn(zombieHorse);

        // Causes error and for pet to untame
        when(this.dbWrapper.insertPet(zombieHorse, "MockPlayerId")).thenReturn(null);

        this.entityTamedListener.onEntityBreedEvent(entityBreedEvent);

        verify(this.owner, times(1)).sendMessage(ChatColor.RED + "Could not tame this pet");
        verify(this.dbWrapper, times(1)).getNumPets(anyString());
        verify(this.dbWrapper, times(1)).getNumPetsByPT(anyString(), any(PetType.Pets.class));
        verify(this.dbWrapper, times(1)).insertPet(any(Entity.class), anyString());
        verify(zombieHorse, times(1)).setOwner(null);
        verify(zombieHorse, never()).setTamed(anyBoolean());
        verify(entityBreedEvent, times(1)).setCancelled(true);
    }
}