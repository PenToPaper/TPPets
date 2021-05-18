package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerEntityDeath;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.mockito.Mockito.*;

public class TPPListenerEntityDeathTest {
    private Horse horse;
    private SQLWrapper sqlWrapper;
    private ListenerEntityDeath listenerEntityDeath;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        Player player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", null, null, new String[]{});
        this.horse = MockFactory.getTamedMockEntity("MockHorseId", org.bukkit.entity.Horse.class, player);

        this.sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, this.logWrapper, false, false);

        this.listenerEntityDeath = new ListenerEntityDeath(tpPets);

        when(this.sqlWrapper.removePet("MockHorseId")).thenReturn(true);
    }

    private EntityDeathEvent getEntityDeathEvent(LivingEntity entity) {
        EntityDeathEvent entityDeathEvent = mock(EntityDeathEvent.class);
        when(entityDeathEvent.getEntity()).thenReturn(entity);
        return entityDeathEvent;
    }

    @Test
    @DisplayName("Deletes valid pet on entity death event")
    void deletesValidPet() throws SQLException {
        EntityDeathEvent entityDeathEvent = getEntityDeathEvent(this.horse);

        this.listenerEntityDeath.onEntityDeathEvent(entityDeathEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockHorseId");
        verify(this.logWrapper, times(1)).logUpdatedPet("Pet MockHorseId has died. Removed from database.");
    }

    @Test
    @DisplayName("Doesn't log pet death if removing pet unsuccessful")
    void doesNotLogPetDeathIfRemovingUnsuccessful() throws SQLException {
        when(this.sqlWrapper.removePet("MockHorseId")).thenReturn(false);
        EntityDeathEvent entityDeathEvent = getEntityDeathEvent(this.horse);

        this.listenerEntityDeath.onEntityDeathEvent(entityDeathEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockHorseId");
        verify(this.logWrapper, never()).logUpdatedPet(anyString());
    }

    @Test
    @DisplayName("Doesn't delete invalid pet on its death")
    void cannotDeleteInvalidPet() throws SQLException {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        EntityDeathEvent entityDeathEvent = getEntityDeathEvent(villager);

        this.listenerEntityDeath.onEntityDeathEvent(entityDeathEvent);

        verify(this.sqlWrapper, never()).removePet(anyString());
        verify(this.logWrapper, never()).logUpdatedPet(anyString());
    }

    @Test
    @DisplayName("Logs SQLExceptions when deleting pet")
    void logsSQLExceptionsOnPetDeletion() throws SQLException {
        when(this.sqlWrapper.removePet("MockHorseId")).thenThrow(new SQLException());
        EntityDeathEvent entityDeathEvent = getEntityDeathEvent(this.horse);

        this.listenerEntityDeath.onEntityDeathEvent(entityDeathEvent);

        verify(this.sqlWrapper, times(1)).removePet("MockHorseId");
        verify(this.logWrapper, times(1)).logErrors("SQL Error - removing pet after death");
    }
}
