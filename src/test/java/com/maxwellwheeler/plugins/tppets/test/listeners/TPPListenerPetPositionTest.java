package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.ListenerPetPosition;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TPPListenerPetPositionTest {
    private Entity[] entities;
    private SQLWrapper sqlWrapper;
    private ArgumentCaptor<Entity> entityCaptor;
    private ChunkUnloadEvent chunkUnloadEvent;
    private ListenerPetPosition listenerPetPosition;

    @BeforeEach
    public void beforeEach() {
        OfflinePlayer petOwner = mock(OfflinePlayer.class);
        Chunk chunk = mock(Chunk.class);
        Horse entity1 = MockFactory.getTamedMockEntity("MockEntityId1", Horse.class, petOwner);
        Wolf entity2 = MockFactory.getTamedMockEntity("MockEntityId2", Wolf.class, petOwner);
        Villager entity3 = MockFactory.getMockEntity("MockEntityId3", Villager.class);
        this.entities = new Entity[]{entity1, entity2, entity3};
        this.entityCaptor = ArgumentCaptor.forClass(Entity.class);
        this.sqlWrapper = mock(SQLWrapper.class);

        LogWrapper logWrapper = mock(LogWrapper.class);
        TPPets tpPets = MockFactory.getMockPlugin(this.sqlWrapper, logWrapper, true, false, true);

        this.listenerPetPosition = new ListenerPetPosition(tpPets);
        this.chunkUnloadEvent = mock(ChunkUnloadEvent.class);

        when(chunk.getEntities()).thenReturn(this.entities);
        when(this.chunkUnloadEvent.getChunk()).thenReturn(chunk);
    }

    @Test
    @DisplayName("Registers pet positions in the database on chunk unload events")
    void listsSpecificLostAndFoundRegions() throws SQLException {
        this.listenerPetPosition.onChunkUnload(this.chunkUnloadEvent);

        verify(this.sqlWrapper, times(3)).insertOrUpdatePetLocation(this.entityCaptor.capture());
        List<Entity> entitiesUpdated = this.entityCaptor.getAllValues();
        assertEquals(this.entities[0], entitiesUpdated.get(0));
        assertEquals(this.entities[1], entitiesUpdated.get(1));
        assertEquals(this.entities[2], entitiesUpdated.get(2));
    }
}
