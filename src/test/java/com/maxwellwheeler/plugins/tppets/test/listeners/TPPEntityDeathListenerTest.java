package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.EntityDeathListener;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class TPPEntityDeathListenerTest {
    private Horse horse;
    private DBWrapper dbWrapper;
    private EntityDeathListener entityDeathListener;

    @BeforeEach
    public void beforeEach() {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", org.bukkit.entity.Horse.class, player);

        this.dbWrapper = mock(DBWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.dbWrapper, logWrapper, false, false, false);

        this.entityDeathListener = new EntityDeathListener(tpPets);
    }

    private EntityDeathEvent getEntityDeathEvent(LivingEntity entity) {
        EntityDeathEvent entityDeathEvent = mock(EntityDeathEvent.class);
        when(entityDeathEvent.getEntity()).thenReturn(entity);
        return entityDeathEvent;
    }

    @Test
    @DisplayName("Deletes valid pet on entity death event")
    void deletesValidPet() {
        EntityDeathEvent entityDeathEvent = getEntityDeathEvent(this.horse);

        this.entityDeathListener.onEntityDeathEvent(entityDeathEvent);

        verify(this.dbWrapper, times(1)).deletePet(this.horse);
    }

    @Test
    @DisplayName("Doesn't delete invalid pet on its death")
    void cannotDeleteInvalidPet() {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        EntityDeathEvent entityDeathEvent = getEntityDeathEvent(villager);

        this.entityDeathListener.onEntityDeathEvent(entityDeathEvent);

        verify(this.dbWrapper, never()).deletePet(any(Entity.class));
    }
}
