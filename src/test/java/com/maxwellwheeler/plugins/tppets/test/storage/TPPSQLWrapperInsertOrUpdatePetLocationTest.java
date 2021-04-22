package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Villager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperInsertOrUpdatePetLocationTest {
    private Horse horse;
    private SQLWrapper mockSQLWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false, false);
        Connection connection = mock(Connection.class);

        OfflinePlayer owner = MockFactory.getMockOfflinePlayer("Mock-Owner-Id", "MockOwnerName");
        this.horse = MockFactory.getTamedMockEntity("Mock-Pet-Id", org.bukkit.entity.Horse.class, owner);

        this.mockSQLWrapper = mock(SQLWrapper.class, Mockito.withSettings()
                .useConstructor(tpPets)
                .defaultAnswer(Mockito.CALLS_REAL_METHODS)
        );
        when(this.mockSQLWrapper.getConnection()).thenReturn(connection);
        doReturn(new PetStorage("MockPetId", 7, 10, 20, 30, "MockWorldName", "MockOwnerId", "MockPetName", "MockEffectivePetName")).when(this.mockSQLWrapper).getSpecificPet("Mock-Pet-Id");
        doReturn(true).when(this.mockSQLWrapper).updatePetLocation(this.horse);
        doReturn(true).when(this.mockSQLWrapper).insertPet(this.horse, "Mock-Owner-Id", "MockPetName");
        doReturn("MockPetName").when(this.mockSQLWrapper).generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE);
    }

    @Test
    @DisplayName("insertOrUpdatePetLocation updates pet location if already in database")
    void insertOrUpdateUpdates() throws SQLException {
        assertTrue(this.mockSQLWrapper.insertOrUpdatePetLocation(this.horse));

        verify(this.mockSQLWrapper, times(1)).getSpecificPet("Mock-Pet-Id");
        verify(this.mockSQLWrapper, times(1)).updatePetLocation(this.horse);
        verify(this.mockSQLWrapper, never()).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.mockSQLWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
    }

    @Test
    @DisplayName("insertOrUpdatePetLocation returns false if update returns false")
    void insertOrUpdateUpdatesReturnsFalse() throws SQLException {
        doReturn(false).when(this.mockSQLWrapper).updatePetLocation(this.horse);

        assertFalse(this.mockSQLWrapper.insertOrUpdatePetLocation(this.horse));

        verify(this.mockSQLWrapper, times(1)).getSpecificPet("Mock-Pet-Id");
        verify(this.mockSQLWrapper, times(1)).updatePetLocation(this.horse);
        verify(this.mockSQLWrapper, never()).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.mockSQLWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
    }

    @Test
    @DisplayName("insertOrUpdatePetLocation inserts pet location if not in database")
    void insertOrUpdateInserts() throws SQLException {
        doReturn(null).when(this.mockSQLWrapper).getSpecificPet("Mock-Pet-Id");

        assertTrue(this.mockSQLWrapper.insertOrUpdatePetLocation(this.horse));

        verify(this.mockSQLWrapper, times(1)).getSpecificPet("Mock-Pet-Id");
        verify(this.mockSQLWrapper, never()).updatePetLocation(any(Entity.class));
        verify(this.mockSQLWrapper, times(1)).generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE);
        verify(this.mockSQLWrapper, times(1)).insertPet(this.horse, "Mock-Owner-Id", "MockPetName");
    }

    @Test
    @DisplayName("insertOrUpdatePetLocation returns false if insert returns false")
    void insertOrUpdateInsertsReturnsFalse() throws SQLException {
        doReturn(null).when(this.mockSQLWrapper).getSpecificPet("Mock-Pet-Id");
        doReturn(false).when(this.mockSQLWrapper).insertPet(this.horse, "Mock-Owner-Id", "MockPetName");

        assertFalse(this.mockSQLWrapper.insertOrUpdatePetLocation(this.horse));

        verify(this.mockSQLWrapper, times(1)).getSpecificPet("Mock-Pet-Id");
        verify(this.mockSQLWrapper, never()).updatePetLocation(any(Entity.class));
        verify(this.mockSQLWrapper, times(1)).generateUniquePetName("Mock-Owner-Id", PetType.Pets.HORSE);
        verify(this.mockSQLWrapper, times(1)).insertPet(this.horse, "Mock-Owner-Id", "MockPetName");
    }

    @Test
    @DisplayName("insertOrUpdatePetLocation returns false if pet not of valid type")
    void insertOrUpdateReturnsFalseInvalidType() throws SQLException {
        Villager villager = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);

        assertFalse(this.mockSQLWrapper.insertOrUpdatePetLocation(villager));

        verify(this.mockSQLWrapper, never()).getSpecificPet(anyString());
        verify(this.mockSQLWrapper, never()).updatePetLocation(any(Entity.class));
        verify(this.mockSQLWrapper, never()).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.mockSQLWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
    }

    @Test
    @DisplayName("insertOrUpdatePetLocation returns false if pet not tamed")
    void insertOrUpdateReturnsFalseNotTamed() throws SQLException {
        when(this.horse.isTamed()).thenReturn(false);

        assertFalse(this.mockSQLWrapper.insertOrUpdatePetLocation(this.horse));

        verify(this.mockSQLWrapper, never()).getSpecificPet(anyString());
        verify(this.mockSQLWrapper, never()).updatePetLocation(any(Entity.class));
        verify(this.mockSQLWrapper, never()).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.mockSQLWrapper, never()).insertPet(any(Entity.class), anyString(), anyString());
    }
}