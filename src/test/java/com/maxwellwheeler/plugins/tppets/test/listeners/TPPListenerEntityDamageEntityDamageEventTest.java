package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.GuestManager;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.MobDamageManager;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerEntityDamage;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TPPListenerEntityDamageEntityDamageEventTest {
    private Horse horse;
    private ListenerEntityDamage listenerEntityDamage;
    private TPPets tpPets;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        Player owner = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, owner);

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

    private EntityDamageEvent getEntityDamageEvent(EntityDamageEvent.DamageCause damageCause, Entity damaged) {
        EntityDamageEvent entityDamageEvent = mock(EntityDamageEvent.class);
        when(entityDamageEvent.getEntity()).thenReturn(damaged);
        when(entityDamageEvent.getCause()).thenReturn(damageCause);
        return entityDamageEvent;
    }

    private void verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause damageCause, String petId) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logPreventedDamage(logCaptor.capture());
        assertEquals("Prevented " + damageCause + " from damaging " + petId, logCaptor.getValue());
    }

    @Test
    @DisplayName("Prevents block explosion damage if EnvironmentalDamage prevented")
    void preventsBlockExplosionType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent block explosion damage without EnvironmentalDamage prevented")
    void doesntPreventBlockExplosionType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents contact damage if EnvironmentalDamage prevented")
    void preventsContactType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CONTACT, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.CONTACT, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent contact damage without EnvironmentalDamage prevented")
    void doesntPreventContactType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CONTACT, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents cramming damage if EnvironmentalDamage prevented")
    void preventsCrammingType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CRAMMING, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.CRAMMING, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent cramming damage without EnvironmentalDamage prevented")
    void doesntPreventCrammingType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CRAMMING, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents custom damage if EnvironmentalDamage prevented")
    void preventsCustomType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CUSTOM, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.CUSTOM, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent custom damage without EnvironmentalDamage prevented")
    void doesntPreventCustomType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.CUSTOM, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents dragon breath damage if EnvironmentalDamage prevented")
    void preventsDragonBreathType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DRAGON_BREATH, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.DRAGON_BREATH, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent dragon breath damage without EnvironmentalDamage prevented")
    void doesntPreventDragonBreathType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DRAGON_BREATH, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents drowning damage if EnvironmentalDamage prevented")
    void preventsDrowningType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DROWNING, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.DROWNING, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent drowning damage without EnvironmentalDamage prevented")
    void doesntPreventDrowningType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DROWNING, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents dryout damage if EnvironmentalDamage prevented")
    void preventsDryoutType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DRYOUT, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.DRYOUT, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent dryout damage without EnvironmentalDamage prevented")
    void doesntPreventDryoutType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.DRYOUT, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents fall damage if EnvironmentalDamage prevented")
    void preventsFallType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FALL, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.FALL, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent fall damage without EnvironmentalDamage prevented")
    void doesntPreventFallType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FALL, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents falling block damage if EnvironmentalDamage prevented")
    void preventsFallingBlockType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FALLING_BLOCK, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.FALLING_BLOCK, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent falling block damage without EnvironmentalDamage prevented")
    void doesntPreventFallingBlockType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FALLING_BLOCK, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents fire damage if EnvironmentalDamage prevented")
    void preventsFireType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FIRE, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.FIRE, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent fire damage without EnvironmentalDamage prevented")
    void doesntPreventFireType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FIRE, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents fire tick damage if EnvironmentalDamage prevented")
    void preventsFireTickType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FIRE_TICK, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.FIRE_TICK, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent fire tick damage without EnvironmentalDamage prevented")
    void doesntPreventFireTickType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FIRE_TICK, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents fly into wall damage if EnvironmentalDamage prevented")
    void preventsFlyIntoWallType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FLY_INTO_WALL, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.FLY_INTO_WALL, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent fly into wall damage without EnvironmentalDamage prevented")
    void doesntPreventFlyIntoWallType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.FLY_INTO_WALL, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents hot floor damage if EnvironmentalDamage prevented")
    void preventsHotFloorType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.HOT_FLOOR, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.HOT_FLOOR, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent hot floor damage without EnvironmentalDamage prevented")
    void doesntPreventHotFloorType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.HOT_FLOOR, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents lava damage if EnvironmentalDamage prevented")
    void preventsLavaType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.LAVA, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.LAVA, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent lava damage without EnvironmentalDamage prevented")
    void doesntPreventLavaType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.LAVA, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents lightning damage if EnvironmentalDamage prevented")
    void preventsLightningType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.LIGHTNING, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.LIGHTNING, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent lightning damage without EnvironmentalDamage prevented")
    void doesntPreventLightningType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.LIGHTNING, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents melting damage if EnvironmentalDamage prevented")
    void preventsMeltingType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.MELTING, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.MELTING, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent melting damage without EnvironmentalDamage prevented")
    void doesntPreventMeltingType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.MELTING, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents poison damage if EnvironmentalDamage prevented")
    void preventsPoisonType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.POISON, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.POISON, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent poison damage without EnvironmentalDamage prevented")
    void doesntPreventPoisonType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.POISON, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents starvation damage if EnvironmentalDamage prevented")
    void preventsStarvationType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.STARVATION, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.STARVATION, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent starvation damage without EnvironmentalDamage prevented")
    void doesntPreventStarvationType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.STARVATION, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents suffocation damage if EnvironmentalDamage prevented")
    void preventsSuffocationType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.SUFFOCATION, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.SUFFOCATION, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent suffocation damage without EnvironmentalDamage prevented")
    void doesntPreventSuffocationType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.SUFFOCATION, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents thorns damage if EnvironmentalDamage prevented")
    void preventsThornsType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.THORNS, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.THORNS, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent thorns damage without EnvironmentalDamage prevented")
    void doesntPreventThornsType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.THORNS, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Prevents wither damage if EnvironmentalDamage prevented")
    void preventsWitherType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.WITHER, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, times(1)).setCancelled(true);
        verifyLoggedPreventedDamage(EntityDamageEvent.DamageCause.WITHER, "MockHorseId");
    }

    @Test
    @DisplayName("Doesn't prevent wither damage without EnvironmentalDamage prevented")
    void doesntPreventWitherType() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.WITHER, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent entity attack")
    void doesntPreventEntityAttackType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.ENTITY_ATTACK, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent entity explosion")
    void doesntPreventEntityExplosionType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent entity sweep attack")
    void doesntPreventEntitySweepAttackType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent magic")
    void doesntPreventMagicType() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.MAGIC, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent projectile")
    void doesntPreventProjectile() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.PROJECTILE, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent suicide")
    void doesntPreventSuicide() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.SUICIDE, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent void")
    void doesntPreventVoid() {
        EntityDamageEvent entityDamageEvent = getEntityDamageEvent(EntityDamageEvent.DamageCause.VOID, this.horse);

        this.listenerEntityDamage.onEntityDamageEvent(entityDamageEvent);

        verify(entityDamageEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }
}
