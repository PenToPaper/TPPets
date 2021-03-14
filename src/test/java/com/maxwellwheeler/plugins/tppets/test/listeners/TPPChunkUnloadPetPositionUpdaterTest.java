package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.ChunkUnloadPetPositionUpdater;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TPPChunkUnloadPetPositionUpdaterTest {
    private Chunk chunk;
    private Entity[] entities;
    private TPPets tpPets;
    private DBWrapper dbWrapper;
    private ArgumentCaptor<Entity> entityCaptor;
    private ChunkUnloadEvent chunkUnloadEvent;
    private ChunkUnloadPetPositionUpdater chunkUnloadPetPositionUpdater;

    @BeforeEach
    public void beforeEach() {
        OfflinePlayer petOwner = mock(OfflinePlayer.class);
        this.chunk = mock(Chunk.class);
        Horse entity1 = MockFactory.getTamedMockEntity("MockEntityId1", Horse.class, petOwner);
        Wolf entity2 = MockFactory.getTamedMockEntity("MockEntityId2", Wolf.class, petOwner);
        Villager entity3 = MockFactory.getMockEntity("MockEntityId3", Villager.class);
        this.entities = new Entity[]{entity1, entity2, entity3};
        this.entityCaptor = ArgumentCaptor.forClass(Entity.class);
        this.dbWrapper = mock(DBWrapper.class);

        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, logWrapper, true, false, true);

        this.chunkUnloadPetPositionUpdater = new ChunkUnloadPetPositionUpdater(this.tpPets);
        this.chunkUnloadEvent = mock(ChunkUnloadEvent.class);

        when(this.chunk.getEntities()).thenReturn(this.entities);
        when(this.chunkUnloadEvent.getChunk()).thenReturn(this.chunk);
    }

    @Test
    @DisplayName("Registers pet positions in the database on chunk unload events")
    void listsSpecificLostAndFoundRegions() {
        this.chunkUnloadPetPositionUpdater.onChunkUnload(this.chunkUnloadEvent);

        verify(this.dbWrapper, times(2)).updateOrInsertPet(this.entityCaptor.capture());
        List<Entity> entitiesUpdated = this.entityCaptor.getAllValues();
        assertEquals(this.entities[0], entitiesUpdated.get(0));
        assertEquals(this.entities[1], entitiesUpdated.get(1));
    }
}
