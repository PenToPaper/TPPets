package com.maxwellwheeler.plugins.tppets.test.listeners;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.listeners.PlayerMoveProtectedRegionScanner;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class TPPPlayerMoveProtectedRegionScannerTest {
    private ProtectedRegion protectedRegion;
    private Player player;
    private Location playerLocation;
    private List<Entity> nearbyEntities;
    private List<Location> nearbyEntityLocations;
    private DBWrapper dbWrapper;
    private LogWrapper logWrapper;
    private ArgumentCaptor<Entity> entityCaptor;
    private TPPets tpPets;
    private PlayerMoveEvent playerMoveEvent;
    private PlayerMoveProtectedRegionScanner playerMoveProtectedRegionScanner;

    @BeforeEach
    public void beforeEach() {
        LostAndFoundRegion lostAndFoundRegion = mock(LostAndFoundRegion.class);
        when(lostAndFoundRegion.getRegionName()).thenReturn("LostAndFoundRegionName");
        World world = mock(World.class);

        this.playerLocation = MockFactory.getMockLocation(world, 101, 202, 303);
        this.entityCaptor = ArgumentCaptor.forClass(Entity.class);
        this.dbWrapper = mock(DBWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.playerMoveEvent = mock(PlayerMoveEvent.class);
        this.tpPets = MockFactory.getMockPlugin(this.dbWrapper, this.logWrapper, true, false, true);
        this.protectedRegion = MockFactory.getProtectedRegion("ProtectedRegionName", "Enter Message", "MockWorldName", world, 100, 200, 300, 400, 500, 600, "LostAndFoundRegionName", lostAndFoundRegion);
        this.player = MockFactory.getMockPlayer("MockPlayerId", "MockPlayerName", world, this.playerLocation, new String[]{});
        this.playerMoveProtectedRegionScanner = new PlayerMoveProtectedRegionScanner(this.tpPets);

        this.nearbyEntityLocations = new ArrayList<>();
        Location entityInPrLocation = MockFactory.getMockLocation(world, 102, 203, 304);
        this.nearbyEntityLocations.add(entityInPrLocation);
        Location entityOutPrLocation = MockFactory.getMockLocation(world, 99, 199, 299);
        this.nearbyEntityLocations.add(entityOutPrLocation);
        Location entityWrongTypeLocation = MockFactory.getMockLocation(world, 102, 203, 304);
        this.nearbyEntityLocations.add(entityWrongTypeLocation);

        this.nearbyEntities = new ArrayList<>();
        Entity entity1 = MockFactory.getTamedMockEntity("MockInPrPet", org.bukkit.entity.Wolf.class, this.player);
        when(entity1.getLocation()).thenReturn(this.nearbyEntityLocations.get(0));
        this.nearbyEntities.add(entity1);
        Entity entity2 = MockFactory.getTamedMockEntity("MockOutPrPet", org.bukkit.entity.Wolf.class, this.player);
        when(entity2.getLocation()).thenReturn(this.nearbyEntityLocations.get(1));
        this.nearbyEntities.add(entity2);
        Entity entity3 = MockFactory.getMockEntity("MockVillagerId", org.bukkit.entity.Villager.class);
        when(entity3.getLocation()).thenReturn(this.nearbyEntityLocations.get(2));
        this.nearbyEntities.add(entity3);

        when(this.playerMoveEvent.getTo()).thenReturn(this.playerLocation);
        when(this.tpPets.getProtectedRegionWithin(this.playerLocation)).thenReturn(this.protectedRegion);
        when(this.playerMoveEvent.getPlayer()).thenReturn(this.player);
        when(this.player.getNearbyEntities(10, 10, 10)).thenReturn(this.nearbyEntities);
        when(this.protectedRegion.isInRegion(this.nearbyEntityLocations.get(0))).thenReturn(true);
        when(this.protectedRegion.isInRegion(this.nearbyEntityLocations.get(1))).thenReturn(false);
        when(this.tpPets.getVaultEnabled()).thenReturn(false);
        when(this.protectedRegion.tpToLostRegion(entity1)).thenReturn(true);
        when(world.getName()).thenReturn("MockWorldName");
    }

    @Test
    @DisplayName("Teleports pets away from protected regions when they're inside it and not allowed to be there")
    void teleportsScannedPetsAwayWhenNotAllowedToBeInPr() {
        this.playerMoveProtectedRegionScanner.onPlayerMove(this.playerMoveEvent);

        verify(this.protectedRegion, times(1)).tpToLostRegion(this.nearbyEntities.get(0));
        verify(this.logWrapper, times(1)).logSuccessfulAction("Teleported pet with UUID MockInPrPet away from ProtectedRegionName to LostAndFoundRegionName");
        verify(this.dbWrapper, times(1)).updateOrInsertPet(this.nearbyEntities.get(0));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(1));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(1));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(2));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(2));
    }

    @Test
    @DisplayName("Doesn't teleport pets away when player is not in the protected region")
    void cannotTeleportScannedPetsAwayWhenPlayerNotInPr() {
        when(this.tpPets.getProtectedRegionWithin(this.playerLocation)).thenReturn(null);

        this.playerMoveProtectedRegionScanner.onPlayerMove(this.playerMoveEvent);

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(0));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(0));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(1));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(1));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(2));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(2));
    }

    @Test
    @DisplayName("Doesn't teleport pets away when player is in a protected region without a lost and found region")
    void cannotTeleportScannedPetsAwayWhenPlayerNotInPrWithLfr() {
        when(this.protectedRegion.getLfReference()).thenReturn(null);

        this.playerMoveProtectedRegionScanner.onPlayerMove(this.playerMoveEvent);

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(0));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(0));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(1));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(1));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(2));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(2));
    }

    @Test
    @DisplayName("Doesn't teleport pets away when player is in a protected region without a world")
    void cannotTeleportScannedPetsAwayWhenPlayerNotInPrWithWorld() {
        when(this.protectedRegion.getWorld()).thenReturn(null);

        this.playerMoveProtectedRegionScanner.onPlayerMove(this.playerMoveEvent);

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(0));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(0));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(1));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(1));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(2));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(2));
    }

    @Test
    @DisplayName("Doesn't teleport pets away when they're not in the protected region")
    void cannotTeleportScannedPetsAwayWhenPetsNotInPr() {
        when(this.protectedRegion.isInRegion(this.nearbyEntityLocations.get(0))).thenReturn(false);

        this.playerMoveProtectedRegionScanner.onPlayerMove(this.playerMoveEvent);

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(0));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(0));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(1));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(1));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(2));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(2));
    }

    @Test
    @DisplayName("Doesn't teleport pets away when they're owned by an online player with permission")
    void cannotTeleportScannedPetsAwayWhenOnlineOwnerHasPermission() {
        when(this.player.hasPermission("tppets.tpanywhere")).thenReturn(true);

        this.playerMoveProtectedRegionScanner.onPlayerMove(this.playerMoveEvent);

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(0));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(0));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(1));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(1));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(2));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(2));
    }

    @Test
    @DisplayName("Doesn't teleport pets away when they're owned by an offline player with vault with permission")
    void cannotTeleportScannedPetsAwayWhenOfflineOwnerHasPermission() {
        Permission permission = mock(Permission.class);
        when(permission.playerHas("MockWorldName", this.player, "tppets.tpanywhere")).thenReturn(true);
        when(this.tpPets.getVaultEnabled()).thenReturn(true);
        when(this.tpPets.getPerms()).thenReturn(permission);

        this.playerMoveProtectedRegionScanner.onPlayerMove(this.playerMoveEvent);

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(0));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(0));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(1));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(1));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(2));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(2));
    }

    @Test
    @DisplayName("Doesn't log actions or update pet in db if there was an error teleporting pet away")
    void cannotLogActionsWithTeleportationError() {
        when(this.protectedRegion.tpToLostRegion(this.nearbyEntities.get(0))).thenReturn(false);

        this.playerMoveProtectedRegionScanner.onPlayerMove(this.playerMoveEvent);

        verify(this.protectedRegion, times(1)).tpToLostRegion(this.nearbyEntities.get(0));
        verify(this.logWrapper, never()).logSuccessfulAction(anyString());
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(0));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(1));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(1));

        verify(this.protectedRegion, never()).tpToLostRegion(this.nearbyEntities.get(2));
        verify(this.dbWrapper, never()).updateOrInsertPet(this.nearbyEntities.get(2));
    }
}
