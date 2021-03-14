package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.MobDamageManager;
import com.maxwellwheeler.plugins.tppets.listeners.EntityDamageListener;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;

import static org.mockito.Mockito.*;

public class TPPEntityDamageListenerTest {
    private Player owner;
    private Player guest;
    private Player stranger;
    private Zombie mob;
    private Minecart environment;
    private BlockProjectileSource blockProjectileSource;
    private Horse horse;
    private EntityDamageListener entityDamageListener;
    private TPPets tpPets;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.owner = MockFactory.getMockPlayer("MockOwnerId", "MockOwnerName", null, null, new String[]{});
        this.guest = MockFactory.getMockPlayer("MockGuestId", "MockGuestName", null, null, new String[]{});
        this.stranger = MockFactory.getMockPlayer("MockStrangerId", "MockStrangerName", null, null, new String[]{});
        this.mob = MockFactory.getMockEntity("MockZombieId", Zombie.class);
        this.environment = MockFactory.getMockEntity("MockMinecartId", org.bukkit.entity.Minecart.class);
        this.blockProjectileSource = mock(BlockProjectileSource.class);
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.owner);

        DBWrapper dbWrapper = mock(DBWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(dbWrapper, logWrapper, false, false, false);

        this.entityDamageListener = new EntityDamageListener(this.tpPets);

        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.isAllowedToPet("MockHorseId", "MockGuestId")).thenReturn(true);
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);
    }

    private Projectile getMockProjectile(ProjectileSource damager) {
        Projectile projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(damager);
        return projectile;
    }

    private EntityDamageByEntityEvent getEntityDamageByEntityEvent(Entity damager, Entity damaged) {
        EntityDamageByEntityEvent entityDamageByEntityEvent = mock(EntityDamageByEntityEvent.class);
        when(entityDamageByEntityEvent.getEntity()).thenReturn(damaged);
        when(entityDamageByEntityEvent.getDamager()).thenReturn(damager);
        return entityDamageByEntityEvent;
    }

    private EntityDamageEvent getEntityDamageEvent(EntityDamageEvent.DamageCause damageCause, Entity damaged) {
        EntityDamageEvent entityDamageEvent = mock(EntityDamageEvent.class);
        when(entityDamageEvent.getEntity()).thenReturn(damaged);
        when(entityDamageEvent.getCause()).thenReturn(damageCause);
        return entityDamageEvent;
    }

    // EntityDamageByEntityEvent

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent entity from being damaged if it's not a tracked type by TPPets")
    void entityDamageByEntityEventAllowsUntrackedEntityDamage() {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.owner, villager);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    // EntityDamageByEntityEvent - OwnerDamage

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents owner from damaging pet with OwnerDamage prevented")
    void entityDamageByEntityEventPreventsOwnerDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.owner, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents owner from damaging pet with a bow and arrow with OwnerDamage prevented")
    void entityDamageByEntityEventPreventsProjectileOwnerDamage() {
        Projectile arrow = getMockProjectile(this.owner);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent owner from damaging pet without OwnerDamage prevented")
    void entityDamageByEntityEventDoesntPreventOwnerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("GuestDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.owner, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent owner from damaging pet with a bow and arrow without OwnerDamage prevented")
    void entityDamageByEntityEventDoesntPreventProjectileOwnerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("GuestDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.owner);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent owner from damaging pet if owner has tppets.bypassprotection")
    void entityDamageByEntityEventDoesntPreventOwnerDamageWithPerms() {
        when(this.owner.hasPermission("tppets.bypassprotection")).thenReturn(true);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.owner, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent owner from damaging pet with bow and arrow if owner has tppets.bypassprotection")
    void entityDamageByEntityEventDoesntPreventProjectileOwnerDamageWithPerms() {
        when(this.owner.hasPermission("tppets.bypassprotection")).thenReturn(true);

        Projectile arrow = getMockProjectile(this.owner);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    // EntityDamageByEntityEvent - GuestDamage

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents guest from damaging pet with GuestDamage prevented")
    void entityDamageByEntityEventPreventsGuestDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.guest, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents guest from damaging pet with a bow and arrow with GuestDamage prevented")
    void entityDamageByEntityEventPreventsProjectileGuestDamage() {
        Projectile arrow = getMockProjectile(this.guest);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent guest from damaging pet without GuestDamage prevented")
    void entityDamageByEntityEventDoesntPreventGuestDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.guest, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent guest from damaging pet with a bow and arrow without GuestDamage prevented")
    void entityDamageByEntityEventDoesntPreventProjectileGuestDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.guest);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent guest from damaging pet if guest has tppets.bypassprotection")
    void entityDamageByEntityEventDoesntPreventGuestDamageWithPerms() {
        when(this.guest.hasPermission("tppets.bypassprotection")).thenReturn(true);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.guest, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent guest from damaging pet with bow and arrow if guest has tppets.bypassprotection")
    void entityDamageByEntityEventDoesntPreventProjectileGuestDamageWithPerms() {
        when(this.guest.hasPermission("tppets.bypassprotection")).thenReturn(true);

        Projectile arrow = getMockProjectile(this.guest);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    // EntityDamageByEntityEvent - StrangerDamage

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents stranger from damaging pet with StrangerDamage prevented")
    void entityDamageByEntityEventPreventsStrangerDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.stranger, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents stranger from damaging pet with a bow and arrow with StrangerDamage prevented")
    void entityDamageByEntityEventPreventsProjectileStrangerDamage() {
        Projectile arrow = getMockProjectile(this.stranger);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent stranger from damaging pet without StrangerDamage prevented")
    void entityDamageByEntityEventDoesntPreventStrangerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.stranger, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent stranger from damaging pet with a bow and arrow without StrangerDamage prevented")
    void entityDamageByEntityEventDoesntPreventProjectileStrangerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.stranger);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent stranger from damaging pet if stranger has tppets.bypassprotection")
    void entityDamageByEntityEventDoesntPreventStrangerDamageWithPerms() {
        when(this.stranger.hasPermission("tppets.bypassprotection")).thenReturn(true);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.stranger, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent stranger from damaging pet with bow and arrow if stranger has tppets.bypassprotection")
    void entityDamageByEntityEventDoesntPreventProjectileStrangerDamageWithPerms() {
        when(this.stranger.hasPermission("tppets.bypassprotection")).thenReturn(true);

        Projectile arrow = getMockProjectile(this.stranger);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    // EntityDamageByEntityEvent - MobDamage

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents mob from damaging pet with MobDamage prevented")
    void entityDamageByEntityEventPreventsMobDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.mob, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents mob from damaging pet with a bow and arrow with MobDamage prevented")
    void entityDamageByEntityEventPreventsProjectileMobDamage() {
        Projectile arrow = getMockProjectile(this.mob);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent mob from damaging pet without MobDamage prevented")
    void entityDamageByEntityEventDoesntPreventMobDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "EnvironmentalDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.mob, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent mob from damaging pet with a bow and arrow without MobDamage prevented")
    void entityDamageByEntityEventDoesntPreventProjectileMobDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "EnvironmentalDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.mob);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    // EntityDamageByEntityEvent - EnvironmentalDamage

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents environment entity from damaging pet with EnvironmentalDamage prevented")
    void entityDamageByEntityEventPreventsEnvironmentalEntityDamage() {
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.environment, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents environment entity from damaging pet with a bow and arrow with EnvironmentalDamage prevented")
    void entityDamageByEntityEventPreventsProjectileEnvironmentalEntityDamage() {
        Projectile arrow = getMockProjectile(this.blockProjectileSource);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent environment entity from damaging pet without EnvironmentalDamage prevented")
    void entityDamageByEntityEventDoesntPreventEnvironmentalEntityDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(this.environment, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent environment entity from damaging pet with a bow and arrow without EnvironmentalDamage prevented")
    void entityDamageByEntityEventDoesntPreventProjectileEnvironmentalEntityDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        Projectile arrow = getMockProjectile(this.blockProjectileSource);
        EntityDamageByEntityEvent entityDamageByEntityEvent = getEntityDamageByEntityEvent(arrow, this.horse);

        this.entityDamageListener.onEntityDamageByEntityEvent(entityDamageByEntityEvent);

        verify(entityDamageByEntityEvent, never()).setCancelled(anyBoolean());
    }

    // EntityDamageEvent

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents block explosion damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsBlockExplosionType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent block explosion damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventBlockExplosionType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents contact damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsContactType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CONTACT, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent contact damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventContactType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CONTACT, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents cramming damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsCrammingType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CRAMMING, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent cramming damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventCrammingType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CRAMMING, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents custom damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsCustomType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CUSTOM, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent custom damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventCustomType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CUSTOM, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents dragon breath damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsDragonBreathType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DRAGON_BREATH, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent dragon breath damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventDragonBreathType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DRAGON_BREATH, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents drowning damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsDrowningType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DROWNING, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent drowning damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventDrowningType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DROWNING, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents dryout damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsDryoutType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DRYOUT, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent dryout damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventDryoutType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DRYOUT, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents fall damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsFallType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FALL, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent fall damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventFallType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FALL, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents falling block damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsFallingBlockType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FALLING_BLOCK, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent falling block damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventFallingBlockType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FALLING_BLOCK, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents fire damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsFireType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FIRE, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent fire damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventFireType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FIRE, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents fire tick damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsFireTickType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FIRE_TICK, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent fire tick damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventFireTickType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FIRE_TICK, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents fly into wall damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsFlyIntoWallType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FLY_INTO_WALL, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent fly into wall damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventFlyIntoWallType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FLY_INTO_WALL, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents hot floor damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsHotFloorType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.HOT_FLOOR, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent hot floor damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventHotFloorType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.HOT_FLOOR, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents lava damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsLavaType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.LAVA, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent lava damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventLavaType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.LAVA, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents lightning damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsLightningType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.LIGHTNING, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent lightning damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventLightningType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.LIGHTNING, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents melting damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsMeltingType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.MELTING, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent melting damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventMeltingType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.MELTING, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents poison damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsPoisonType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.POISON, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent poison damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventPoisonType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.POISON, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents starvation damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsStarvationType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.STARVATION, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent starvation damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventStarvationType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.STARVATION, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents suffocation damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsSuffocationType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.SUFFOCATION, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent suffocation damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventSuffocationType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.SUFFOCATION, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents thorns damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsThornsType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.THORNS, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent thorns damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventThornsType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.THORNS, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Prevents wither damage if EnvironmentalDamage prevented")
    void entityDamageEventPreventsWitherType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.WITHER, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent wither damage without EnvironmentalDamage prevented")
    void entityDamageEventDoesntPreventWitherType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.WITHER, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent entity attack")
    void entityDamageEventDoesntPreventEntityAttackType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.ENTITY_ATTACK, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent entity explosion")
    void entityDamageEventDoesntPreventEntityExplosionType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent entity sweep attack")
    void entityDamageEventDoesntPreventEntitySweepAttackType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent magic")
    void entityDamageEventDoesntPreventMagicType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.MAGIC, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent projectile")
    void entityDamageEventDoesntPreventProjectile() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.PROJECTILE, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent suicide")
    void entityDamageEventDoesntPreventSuicide() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.SUICIDE, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }

    @Test
    @DisplayName("EntityDamageByEntityEvent - Doesn't prevent void")
    void entityDamageEventDoesntPreventVoid() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.VOID, this.horse);

        this.entityDamageListener.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
    }
}
