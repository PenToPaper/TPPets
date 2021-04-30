package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TPPEntityActionsTest {
    private Wolf wolf;
    private Villager villager;

    @BeforeEach
    public void beforeEach() {
        this.wolf = MockFactory.getMockEntity("MockPetId", org.bukkit.entity.Wolf.class);
        this.villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
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
}
