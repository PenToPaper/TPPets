package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.MobDamageManager;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TPPMobDamageManagerTest {
    private TPPets tpPets;
    private Player owner;
    private Player guest;
    private Player stranger;
    private Villager villager;
    private Minecart minecart;
    private BlockProjectileSource blockProjectileSource;
    private Horse horse;

    @BeforeEach
    public void beforeEach() {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false, false);

        this.owner = MockFactory.getMockPlayer("MockOwnerId", "MockOwnerName", null, null, new String[]{});
        this.guest = MockFactory.getMockPlayer("MockGuestId", "MockGuestName", null, null, new String[]{});
        this.stranger = MockFactory.getMockPlayer("MockStrangerId", "MockStrangerName", null, null, new String[]{});
        this.villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        this.minecart = MockFactory.getMockEntity("MockMinecartId", org.bukkit.entity.Minecart.class);
        this.blockProjectileSource = mock(BlockProjectileSource.class);
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", org.bukkit.entity.Horse.class, this.owner);

        when(this.tpPets.isAllowedToPet("MockHorseId", "MockGuestId")).thenReturn(true);
    }

    private Projectile getProjectileFromSource(ProjectileSource source) {
        Projectile projectile = mock(Projectile.class);
        when(projectile.getShooter()).thenReturn(source);
        return projectile;
    }

    @Test
    @DisplayName("mobDamageManager prevents only owner damage when enabled")
    void mobDamageManagerPreventsOwnerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Collections.singletonList("OwnerDamage"));

        assertTrue(mobDamageManager.isPreventedEntityDamage(this.owner, this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.owner), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.guest, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.guest), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.stranger, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.stranger), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.villager, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.villager), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.minecart, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.blockProjectileSource), this.horse));
    }

    @Test
    @DisplayName("mobDamageManager prevents only guest damage when enabled")
    void mobDamageManagerPreventsGuestDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Collections.singletonList("GuestDamage"));

        assertFalse(mobDamageManager.isPreventedEntityDamage(this.owner, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.owner), this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(this.guest, this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.guest), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.stranger, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.stranger), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.villager, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.villager), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.minecart, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.blockProjectileSource), this.horse));
    }

    @Test
    @DisplayName("mobDamageManager prevents only stranger damage when enabled")
    void mobDamageManagerPreventsStrangerDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Collections.singletonList("StrangerDamage"));

        assertFalse(mobDamageManager.isPreventedEntityDamage(this.owner, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.owner), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.guest, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.guest), this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(this.stranger, this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.stranger), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.villager, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.villager), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.minecart, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.blockProjectileSource), this.horse));
    }

    @Test
    @DisplayName("mobDamageManager prevents only mob damage when enabled")
    void mobDamageManagerPreventsMobDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Collections.singletonList("MobDamage"));

        assertFalse(mobDamageManager.isPreventedEntityDamage(this.owner, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.owner), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.guest, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.guest), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.stranger, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.stranger), this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(this.villager, this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.villager), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.minecart, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.blockProjectileSource), this.horse));
    }

    @Test
    @DisplayName("mobDamageManager prevents only mob damage when enabled")
    void mobDamageManagerPreventsEntityEnvironmentalDamage() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Collections.singletonList("EnvironmentalDamage"));

        assertFalse(mobDamageManager.isPreventedEntityDamage(this.owner, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.owner), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.guest, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.guest), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.stranger, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.stranger), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.villager, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.villager), this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(this.minecart, this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.blockProjectileSource), this.horse));
    }

    @Test
    @DisplayName("mobDamageManager does not prevent damage from players with tppets.bypassprotection")
    void mobDamageManagerAllowsWithPermission() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));

        when(this.owner.hasPermission("tppets.bypassprotection")).thenReturn(true);
        when(this.guest.hasPermission("tppets.bypassprotection")).thenReturn(true);
        when(this.stranger.hasPermission("tppets.bypassprotection")).thenReturn(true);

        assertFalse(mobDamageManager.isPreventedEntityDamage(this.owner, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.owner), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.guest, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.guest), this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(this.stranger, this.horse));
        assertFalse(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.stranger), this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(this.villager, this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.villager), this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(this.minecart, this.horse));
        assertTrue(mobDamageManager.isPreventedEntityDamage(getProjectileFromSource(this.blockProjectileSource), this.horse));
    }

    @Test
    @DisplayName("mobDamageManager prevents environmental damage when enabled")
    void mobDamageManagerPreventsEnvironmentalDamage() {
        EntityDamageEvent.DamageCause[] damageCauses = new EntityDamageEvent.DamageCause[]{
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
            EntityDamageEvent.DamageCause.CONTACT,
            EntityDamageEvent.DamageCause.CRAMMING,
            EntityDamageEvent.DamageCause.CUSTOM,
            EntityDamageEvent.DamageCause.DRAGON_BREATH,
            EntityDamageEvent.DamageCause.DROWNING,
            EntityDamageEvent.DamageCause.DRYOUT,
            EntityDamageEvent.DamageCause.FALL,
            EntityDamageEvent.DamageCause.FALLING_BLOCK,
            EntityDamageEvent.DamageCause.FIRE,
            EntityDamageEvent.DamageCause.FIRE_TICK,
            EntityDamageEvent.DamageCause.FLY_INTO_WALL,
            EntityDamageEvent.DamageCause.HOT_FLOOR,
            EntityDamageEvent.DamageCause.LAVA,
            EntityDamageEvent.DamageCause.LIGHTNING,
            EntityDamageEvent.DamageCause.MELTING,
            EntityDamageEvent.DamageCause.POISON,
            EntityDamageEvent.DamageCause.STARVATION,
            EntityDamageEvent.DamageCause.SUFFOCATION,
            EntityDamageEvent.DamageCause.THORNS,
            EntityDamageEvent.DamageCause.WITHER
        };

        EntityDamageEvent.DamageCause[] invalidDamageCauses = new EntityDamageEvent.DamageCause[]{
                EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
                EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,
                EntityDamageEvent.DamageCause.MAGIC,
                EntityDamageEvent.DamageCause.PROJECTILE,
                EntityDamageEvent.DamageCause.SUICIDE,
                EntityDamageEvent.DamageCause.VOID,
        };


        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Collections.singletonList("EnvironmentalDamage"));

        for (EntityDamageEvent.DamageCause damageCause : damageCauses) {
            assertTrue(mobDamageManager.isPreventedEnvironmentalDamage(damageCause));
        }

        for (EntityDamageEvent.DamageCause damageCause : invalidDamageCauses) {
            assertFalse(mobDamageManager.isPreventedEnvironmentalDamage(damageCause));
        }
    }

    @Test
    @DisplayName("mobDamageManager allows environmental damage when disabled")
    void mobDamageManagerAllowsEnvironmentalDamage() {
        EntityDamageEvent.DamageCause[] damageCauses = new EntityDamageEvent.DamageCause[]{
                EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
                EntityDamageEvent.DamageCause.CONTACT,
                EntityDamageEvent.DamageCause.CRAMMING,
                EntityDamageEvent.DamageCause.CUSTOM,
                EntityDamageEvent.DamageCause.DRAGON_BREATH,
                EntityDamageEvent.DamageCause.DROWNING,
                EntityDamageEvent.DamageCause.DRYOUT,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                EntityDamageEvent.DamageCause.ENTITY_EXPLOSION,
                EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK,
                EntityDamageEvent.DamageCause.FALL,
                EntityDamageEvent.DamageCause.FALLING_BLOCK,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.FLY_INTO_WALL,
                EntityDamageEvent.DamageCause.HOT_FLOOR,
                EntityDamageEvent.DamageCause.LAVA,
                EntityDamageEvent.DamageCause.LIGHTNING,
                EntityDamageEvent.DamageCause.MAGIC,
                EntityDamageEvent.DamageCause.MELTING,
                EntityDamageEvent.DamageCause.POISON,
                EntityDamageEvent.DamageCause.PROJECTILE,
                EntityDamageEvent.DamageCause.STARVATION,
                EntityDamageEvent.DamageCause.SUFFOCATION,
                EntityDamageEvent.DamageCause.SUICIDE,
                EntityDamageEvent.DamageCause.THORNS,
                EntityDamageEvent.DamageCause.VOID,
                EntityDamageEvent.DamageCause.WITHER
        };

        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, new ArrayList<>());

        for (EntityDamageEvent.DamageCause damageCause : damageCauses) {
            assertFalse(mobDamageManager.isPreventedEnvironmentalDamage(damageCause));
        }
    }
}

