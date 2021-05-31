package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.GuestManager;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.helpers.MobDamageManager;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerEntityDamage;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.entity.*;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.BlockProjectileSource;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPListenerEntityDamagePotionSplashEventTest {
    private Horse horse;
    private Player owner;
    private Player guest;
    private Player stranger;
    private Zombie mob;

    private ListenerEntityDamage listenerEntityDamage;
    private TPPets tpPets;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.owner = MockFactory.getMockPlayer("MockOwnerId", "MockOwnerName", null, null, new String[]{});
        this.guest = MockFactory.getMockPlayer("MockGuestId", "MockGuestName", null, null, new String[]{});
        this.stranger = MockFactory.getMockPlayer("MockStrangerId", "MockStrangerName", null, null, new String[]{});
        this.mob = MockFactory.getMockEntity("MockZombieId", Zombie.class);
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", Horse.class, this.owner);

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

    private ThrownPotion getThrownPotion(PotionEffectType potionEffectType, ProjectileSource damager) {
        ThrownPotion thrownPotion = mock(ThrownPotion.class);
        PotionEffect potionEffect = new PotionEffect(potionEffectType, 1, 1);
        when(thrownPotion.getEffects()).thenReturn(Collections.singletonList(potionEffect));
        when(thrownPotion.getShooter()).thenReturn(damager);
        return thrownPotion;
    }

    private PotionSplashEvent getPotionSplashEvent(LivingEntity damaged, ThrownPotion thrownPotion) {
        PotionSplashEvent potionSplashEvent = mock(PotionSplashEvent.class);
        when(potionSplashEvent.getPotion()).thenReturn(thrownPotion);
        when(potionSplashEvent.getAffectedEntities()).thenReturn(Collections.singletonList(damaged));
        return potionSplashEvent;
    }

    @Test
    @DisplayName("Doesn't prevent entity from being damaged if it's not a poison potion")
    void allowsNonPoisonPotion() {
        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.HARM, this.owner);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent entity from being damaged if it's not thrown by a living entity")
    void allowsEnvironmentalThrows() {
        BlockProjectileSource blockProjectileSource = mock(BlockProjectileSource.class);
        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, blockProjectileSource);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Doesn't prevent entity from being damaged if it does not hit a living entity")
    void allowsPotionsThatDoNotHitTrackedPets() {
        Villager villager = mock(Villager.class);
        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.owner);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(villager, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    // OwnerDamage

    @Test
    @DisplayName("Prevents owner from poisoning pet")
    void preventsOwnerPoisoningPet() {
        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.owner);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, times(1)).setCancelled(true);
        verify(this.logWrapper, times(1)).logPreventedDamage("Prevented POISON SPLASH from damaging MockHorseId");
    }

    @Test
    @DisplayName("Does not prevent owner from poisoning pet without OwnerDamage prevented")
    void allowsOwnerPoisoningPetWithoutOwnerDamagePrevented() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("GuestDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.owner);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Does not prevent owner from poisoning pet if owner has tppets.bypassprotection")
    void allowsOwnerPoisoningPetWithPerms() {
        when(this.owner.hasPermission("tppets.bypassprotection")).thenReturn(true);

        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.owner);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    // GuestDamage

    @Test
    @DisplayName("Prevents guest from poisoning pet")
    void preventsGuestPoisoningPet() {
        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.guest);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, times(1)).setCancelled(true);
        verify(this.logWrapper, times(1)).logPreventedDamage("Prevented POISON SPLASH from damaging MockHorseId");
    }

    @Test
    @DisplayName("Does not prevent guest from poisoning pet without GuestDamage prevented")
    void allowsGuestPoisoningPetWithoutGuestDamagePrevented() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "StrangerDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.guest);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Does not prevent guest from poisoning pet if guest has tppets.bypassprotection")
    void allowsGuestPoisoningPetWithPerms() {
        when(this.guest.hasPermission("tppets.bypassprotection")).thenReturn(true);

        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.guest);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    // StrangerDamage

    @Test
    @DisplayName("Prevents stranger from poisoning pet")
    void preventsStrangerPoisoningPet() {
        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.stranger);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, times(1)).setCancelled(true);
        verify(this.logWrapper, times(1)).logPreventedDamage("Prevented POISON SPLASH from damaging MockHorseId");
    }

    @Test
    @DisplayName("Does not prevent stranger from poisoning pet without StrangerDamage prevented")
    void allowsStrangerPoisoningPetWithoutStrangerDamagePrevented() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "EnvironmentalDamage", "MobDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.stranger);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    @Test
    @DisplayName("Does not prevent stranger from poisoning pet if stranger has tppets.bypassprotection")
    void allowsStrangerPoisoningPetWithPerms() {
        when(this.stranger.hasPermission("tppets.bypassprotection")).thenReturn(true);

        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.stranger);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }

    // MobDamage

    @Test
    @DisplayName("Prevents mob from poisoning pet")
    void preventsMobPoisoningPet() {
        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.mob);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, times(1)).setCancelled(true);
        verify(this.logWrapper, times(1)).logPreventedDamage("Prevented POISON SPLASH from damaging MockHorseId");
    }

    @Test
    @DisplayName("Does not prevent mob from poisoning pet without MobDamage prevented")
    void allowsMobPoisoningPetWithoutMobDamagePrevented() {
        MobDamageManager mobDamageManager = new MobDamageManager(this.tpPets, Arrays.asList("OwnerDamage", "GuestDamage", "StrangerDamage", "EnvironmentalDamage"));
        when(this.tpPets.getMobDamageManager()).thenReturn(mobDamageManager);

        ThrownPotion thrownPotion = getThrownPotion(PotionEffectType.POISON, this.mob);
        PotionSplashEvent potionSplashEvent = getPotionSplashEvent(this.horse, thrownPotion);

        this.listenerEntityDamage.onPotionSplashEvent(potionSplashEvent);

        verify(potionSplashEvent, never()).setCancelled(anyBoolean());
        verify(this.logWrapper, never()).logPreventedDamage(anyString());
    }
}
