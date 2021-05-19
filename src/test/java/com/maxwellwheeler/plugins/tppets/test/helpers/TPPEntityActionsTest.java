package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class TPPEntityActionsTest {
    private Wolf wolf;
    private Horse horse;
    private SkeletonHorse skeletonHorse;
    private Villager villager;

    @BeforeEach
    public void beforeEach() {
        this.wolf = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Wolf.class);
        this.villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        this.horse = MockFactory.getMockEntity("MockHorseId", org.bukkit.entity.Horse.class);
        this.skeletonHorse = MockFactory.getMockEntity("MockSkeletonHorseId", org.bukkit.entity.SkeletonHorse.class);
    }

    @Test
    @DisplayName("setSitting calls the setSitting method on entity if sittable")
    void setSittingSittable() {
        EntityActions.setSitting(this.wolf);
        verify(this.wolf, times(1)).setSitting(true);
    }

    @Test
    @DisplayName("setSitting does nothing to entity if not sittable")
    void setSittingNotSittable() {
        EntityActions.setSitting(this.villager);
    }

    @Test
    @DisplayName("setStanding calls the setSitting method on entity if sittable")
    void setStandingSittable() {
        EntityActions.setStanding(this.wolf);
        verify(this.wolf, times(1)).setSitting(false);
    }

    @Test
    @DisplayName("setSitting does nothing to entity if not sittable")
    void setStandingNotSittable() {
        EntityActions.setStanding(this.villager);
    }

    @Test
    @DisplayName("releasePetEntity Wolf")
    void releasePetEntityWolf() {
        EntityActions.releasePetEntity(this.wolf);
        verify(this.wolf, times(1)).setSitting(false);
        verify(this.wolf, times(1)).setOwner(null);
        verify(this.wolf, times(1)).setTamed(false);
    }

    @Test
    @DisplayName("releasePetEntity Horse")
    void releasePetEntityHorse() {
        EntityActions.releasePetEntity(this.horse);
        verify(this.horse, times(1)).setOwner(null);
        verify(this.horse, times(1)).setTamed(false);
    }

    @Test
    @DisplayName("releasePetEntity Skeleton Horse")
    void releasePetEntitySkeletonHorse() {
        EntityActions.releasePetEntity(this.skeletonHorse);
        verify(this.skeletonHorse, times(1)).setOwner(null);
        verify(this.skeletonHorse, never()).setTamed(anyBoolean());
    }
}
