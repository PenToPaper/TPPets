package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.GuestManager;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.MobDamageManager;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerEntityDamage;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

public class TPPListenerEntityDamageEntityDamageByEntityTest {
    private Player owner;
    private Player guest;
    private Player stranger;
    private Zombie mob;
    private Minecart environment;
    private BlockProjectileSource blockProjectileSource;
    private Horse horse;
    private ListenerEntityDamage listenerEntityDamage;
    private TPPets tpPets;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.owner = MockFactory.getMockPlayer("MockOwnerId", "MockOwnerName", null, null, new String[]{});
        when(this.owner.getType()).thenReturn(EntityType.PLAYER);
        this.guest = MockFactory.getMockPlayer("MockGuestId", "MockGuestName", null, null, new String[]{});
        when(this.guest.getType()).thenReturn(EntityType.PLAYER);
        this.stranger = MockFactory.getMockPlayer("MockStrangerId", "MockStrangerName", null, null, new String[]{});
        when(this.stranger.getType()).thenReturn(EntityType.PLAYER);
        this.mob = MockFactory.getMockEntity("MockZombieId", Zombie.class);
        when(this.mob.getType()).thenReturn(EntityType.ZOMBIE);
        this.environment = MockFactory.getMockEntity("MockMinecartId", org.bukkit.entity.Minecart.class);
        when(this.environment.getType()).thenReturn(EntityType.MINECART);
        this.blockProjectileSource = mock(BlockProjectileSource.class);
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.owner);
        when(this.horse.getType()).thenReturn(EntityType.HORSE);

        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, this.logWrapper, false, false);

        this.listenerEntityDamage = new ListenerEntityDamage(this.tpPets);

        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        when(sqlWrapper.getAllGuests()).thenReturn(new Hashtable<>());
        GuestManager guestManager = new GuestManager(sqlWrapper);
        guestManager.addGuest("MockHorseId", "MockGuestId");

        when(this.tpPets.getGuestManager()).thenReturn(guestManager);
    }

    private Projectile getMockProjectile(ProjectileSource damager) {
        Projectile projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(damager);
        when(projectile.getType()).thenReturn(EntityType.ARROW);
        return projectile;
    }

    private EntityDamageByEntityEvent getEntityDamageByEntityEvent(Entity damager, Entity damaged) {
        EntityDamageByEntityEvent entityDamageByEntityEvent = mock(EntityDamageByEntityEvent.class);
        when(entityDamageByEntityEvent.getEntity()).thenReturn(damaged);
        when(entityDamageByEntityEvent.getDamager()).thenReturn(damager);
        return entityDamageByEntityEvent;
    }

    private void verifyLoggedPreventedDamage(EntityType entityType, String petId) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logPreventedDamage(logCaptor.capture());
        assertEquals("Prevented " + entityType + " from damaging " + petId, logCaptor.getValue());

    }

    @Test
    @DisplayName("Doesn't prevent entity from being damaged if it's not a tracked type by TPPets")
    void allowsUntrackedEntityDamage() {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.owner, villager);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    // OwnerDamage

    @Test
    @DisplayName("Prevents owner from damaging pet with OwnerDamage prevented")
    void preventsOwnerDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.owner, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.PLAYER, "MockHorseId");
    }

    @Test
    @DisplayName("Prevents owner from damaging pet with a bow and arrow with OwnerDamage prevented")
    void preventsProjectileOwnerDamage() {
        Projectile arrow = getMockProjectile(this.owner);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.ARROW, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent owner from damaging pet without OwnerDamage prevented")
    void doesntPreventOwnerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("GuestDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.owner, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent owner from damaging pet with a bow and arrow without OwnerDamage prevented")
    void doesntPreventProjectileOwnerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("GuestDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.owner);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent owner from damaging pet if owner has tppets.bypassprotection")
    void doesntPreventOwnerDamageWithPerms() {
        when(this.owner.hasPermission("tppets.bypassprotection")).thenReturn(true);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.owner, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent owner from damaging pet with bow and arrow if owner has tppets.bypassprotection")
    void doesntPreventProjectileOwnerDamageWithPerms() {
        when(this.owner.hasPermission("tppets.bypassprotection")).thenReturn(true);

        Projectile arrow = getMockProjectile(this.owner);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    // GuestDamage

    @Test
    @DisplayName("Prevents guest from damaging pet with GuestDamage prevented")
    void preventsGuestDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.guest, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.PLAYER, "MockHorseId");
    }

    @Test
    @DisplayName("Prevents guest from damaging pet with a bow and arrow with GuestDamage prevented")
    void preventsProjectileGuestDamage() {
        Projectile arrow = getMockProjectile(this.guest);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.ARROW, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent guest from damaging pet without GuestDamage prevented")
    void doesntPreventGuestDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.guest, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent guest from damaging pet with a bow and arrow without GuestDamage prevented")
    void doesntPreventProjectileGuestDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.guest);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent guest from damaging pet if guest has tppets.bypassprotection")
    void doesntPreventGuestDamageWithPerms() {
        when(this.guest.hasPermission("tppets.bypassprotection")).thenReturn(true);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.guest, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent guest from damaging pet with bow and arrow if guest has tppets.bypassprotection")
    void doesntPreventProjectileGuestDamageWithPerms() {
        when(this.guest.hasPermission("tppets.bypassprotection")).thenReturn(true);

        Projectile arrow = getMockProjectile(this.guest);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    // StrangerDamage

    @Test
    @DisplayName("Prevents stranger from damaging pet with StrangerDamage prevented")
    void preventsStrangerDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.stranger, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.PLAYER, "MockHorseId");
    }

    @Test
    @DisplayName("Prevents stranger from damaging pet with a bow and arrow with StrangerDamage prevented")
    void preventsProjectileStrangerDamage() {
        Projectile arrow = getMockProjectile(this.stranger);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.ARROW, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent stranger from damaging pet without StrangerDamage prevented")
    void doesntPreventStrangerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.stranger, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent stranger from damaging pet with a bow and arrow without StrangerDamage prevented")
    void doesntPreventProjectileStrangerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.stranger);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent stranger from damaging pet if stranger has tppets.bypassprotection")
    void doesntPreventStrangerDamageWithPerms() {
        when(this.stranger.hasPermission("tppets.bypassprotection")).thenReturn(true);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.stranger, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent stranger from damaging pet with bow and arrow if stranger has tppets.bypassprotection")
    void doesntPreventProjectileStrangerDamageWithPerms() {
        when(this.stranger.hasPermission("tppets.bypassprotection")).thenReturn(true);

        Projectile arrow = getMockProjectile(this.stranger);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    // MobDamage

    @Test
    @DisplayName("Prevents mob from damaging pet with MobDamage prevented")
    void preventsMobDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.mob, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.ZOMBIE, "MockHorseId");
    }

    @Test
    @DisplayName("Prevents mob from damaging pet with a bow and arrow with MobDamage prevented")
    void preventsProjectileMobDamage() {
        Projectile arrow = getMockProjectile(this.mob);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.ARROW, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent mob from damaging pet without MobDamage prevented")
    void doesntPreventMobDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "EnvironmentalDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.mob, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent mob from damaging pet with a bow and arrow without MobDamage prevented")
    void doesntPreventProjectileMobDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "EnvironmentalDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.mob);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    // EnvironmentalDamage

    @Test
    @DisplayName("Prevents environment entity from damaging pet with EnvironmentalDamage prevented")
    void preventsEnvironmentalEntityDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.environment, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.MINECART, "MockHorseId");
    }

    @Test
    @DisplayName("Prevents environment entity from damaging pet with a bow and arrow with EnvironmentalDamage prevented")
    void preventsProjectileEnvironmentalEntityDamage() {
        Projectile arrow = getMockProjectile(this.blockProjectileSource);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityType.ARROW, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent environment entity from damaging pet without EnvironmentalDamage prevented")
    void doesntPreventEnvironmentalEntityDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.environment, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent environment entity from damaging pet with a bow and arrow without EnvironmentalDamage prevented")
    void doesntPreventProjectileEnvironmentalEntityDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.blockProjectileSource);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.listenerEntityDamage.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }
}
