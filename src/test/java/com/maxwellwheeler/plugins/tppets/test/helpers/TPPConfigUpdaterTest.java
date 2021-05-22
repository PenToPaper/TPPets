package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ConfigUpdater;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class TPPConfigUpdaterTest {
    private TPPets tpPets;
    private FileConfiguration fileConfiguration;
    private ConfigUpdater configUpdater;

    @Captor
    private ArgumentCaptor<List<String>> argumentCaptorStringList;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() {
        MockitoAnnotations.openMocks(this);

        this.logWrapper = mock(LogWrapper.class);
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, this.logWrapper, false, false);
        this.fileConfiguration = mock(FileConfiguration.class);

        when(this.tpPets.getConfig()).thenReturn(this.fileConfiguration);

        when(this.fileConfiguration.getInt("schema_version", 1)).thenReturn(1);

        List<String> protectList = Arrays.asList("PlayerDamage", "OwnerDamage", "EnvironmentalDamage", "MobDamage");
        when(this.fileConfiguration.getStringList("protect_pets_from")).thenReturn(protectList);

        List<String> releaseTools = Arrays.asList("APPLE", "BELL");
        when(this.fileConfiguration.getStringList("tools.untame_pets")).thenReturn(releaseTools);

        this.configUpdater = mock(ConfigUpdater.class, withSettings().useConstructor(this.tpPets).defaultAnswer(CALLS_REAL_METHODS));
    }

    public void verifyLoggedPluginInfo(int startVersion, int endVersion) {
        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(this.logWrapper, times(1)).logPluginInfo(logCaptor.capture());
        assertEquals("Updated config version from version " + startVersion + " to " + endVersion, logCaptor.getValue());
    }

    void assertContainsInAnyOrder(List<String> actual, String... expected) {
        for (String expectedString : expected) {
            assertTrue(actual.contains(expectedString));
        }
    }

    @Test
    @DisplayName("Updates from 1 run proper modifications and cascade to update 4")
    void oneToFour() {
        this.configUpdater.update();

        // 1 to 2
        verify(this.fileConfiguration, times(1)).set("horse_limit", -1);
        verify(this.fileConfiguration, times(1)).set("mule_limit", -1);
        verify(this.fileConfiguration, times(1)).set("llama_limit", -1);
        verify(this.fileConfiguration, times(1)).set("donkey_limit", -1);
        verify(this.fileConfiguration, times(1)).set("tools.untame_pets", new String[]{"SHEARS"});
        verify(this.fileConfiguration, times(1)).set("tools.get_owner", new String[]{"BONE"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.horses", new String[]{"horse"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.mules", new String[]{"mule"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.llamas", new String[]{"llama"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.donkeys", new String[]{"donkey"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.rename", new String[]{"setname"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.allow", new String[]{"add"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.remove", new String[]{"take"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.list", new String[]{"show"});

        // 2 to 3
        verify(this.fileConfiguration, times(1)).set("storage_limit", 5);
        verify(this.fileConfiguration, times(1)).set("command_aliases.store", new String[]{"move", "stable"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.storage", new String[]{"setstable"});
        verify(this.fileConfiguration, times(1)).set("logging.updated_pets", true);
        verify(this.fileConfiguration, times(1)).set("logging.successful_actions", true);
        verify(this.fileConfiguration, times(1)).set("logging.unsuccessful_actions", true);
        verify(this.fileConfiguration, times(1)).set("logging.prevented_damage", true);
        verify(this.fileConfiguration, times(1)).set("logging.errors", true);

        // 3 to 4
        verify(this.fileConfiguration, times(1)).set("tools.select_region", new String[]{"BLAZE_ROD"});
        verify(this.fileConfiguration, times(1)).set("tools.release_pets", new String[]{"APPLE", "BELL"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.tp", new String[]{"teleport", "find", "get"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.allowed", new String[]{"permitted", "guests", "guest", "g"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.all", new String[]{"findall", "getall"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.position1", new String[]{"1", "pos1", "startpos", "start"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.position2", new String[]{"2", "pos2", "endpos", "end"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.clear", new String[]{"wipe"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.release", new String[]{"untame"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.serverstorage", new String[]{"defstorage", "sstorage", "serverstable"});

        verify(this.fileConfiguration, times(1)).set("command_aliases.dogs", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.cats", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.birds", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.horses", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.mules", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.llamas", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.donkeys", null);

        verify(this.fileConfiguration, times(1)).set(eq("protect_pets_from"), this.argumentCaptorStringList.capture());
        List<String> protectPetsFromList = this.argumentCaptorStringList.getValue();
        assertContainsInAnyOrder(protectPetsFromList, "GuestDamage", "StrangerDamage", "OwnerDamage", "EnvironmentalDamage", "MobDamage");

        verifyLoggedPluginInfo(1, 4);
    }

    @Test
    @DisplayName("Updates from 2 run proper modifications and cascade to update 4")
    void twoToFour() {
        when(this.fileConfiguration.getInt("schema_version", 1)).thenReturn(2);
        this.configUpdater = mock(ConfigUpdater.class, withSettings().useConstructor(this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        this.configUpdater.update();

        // 1 to 2
        verify(this.fileConfiguration, never()).set("horse_limit", -1);
        verify(this.fileConfiguration, never()).set("mule_limit", -1);
        verify(this.fileConfiguration, never()).set("llama_limit", -1);
        verify(this.fileConfiguration, never()).set("donkey_limit", -1);
        verify(this.fileConfiguration, never()).set("tools.untame_pets", new String[]{"SHEARS"});
        verify(this.fileConfiguration, never()).set("tools.get_owner", new String[]{"BONE"});
        verify(this.fileConfiguration, never()).set("command_aliases.horses", new String[]{"horse"});
        verify(this.fileConfiguration, never()).set("command_aliases.mules", new String[]{"mule"});
        verify(this.fileConfiguration, never()).set("command_aliases.llamas", new String[]{"llama"});
        verify(this.fileConfiguration, never()).set("command_aliases.donkeys", new String[]{"donkey"});
        verify(this.fileConfiguration, never()).set("command_aliases.rename", new String[]{"setname"});
        verify(this.fileConfiguration, never()).set("command_aliases.allow", new String[]{"add"});
        verify(this.fileConfiguration, never()).set("command_aliases.remove", new String[]{"take"});
        verify(this.fileConfiguration, never()).set("command_aliases.list", new String[]{"show"});

        // 2 to 3
        verify(this.fileConfiguration, times(1)).set("storage_limit", 5);
        verify(this.fileConfiguration, times(1)).set("command_aliases.store", new String[]{"move", "stable"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.storage", new String[]{"setstable"});
        verify(this.fileConfiguration, times(1)).set("logging.updated_pets", true);
        verify(this.fileConfiguration, times(1)).set("logging.successful_actions", true);
        verify(this.fileConfiguration, times(1)).set("logging.unsuccessful_actions", true);
        verify(this.fileConfiguration, times(1)).set("logging.prevented_damage", true);
        verify(this.fileConfiguration, times(1)).set("logging.errors", true);

        // 3 to 4
        verify(this.fileConfiguration, times(1)).set("tools.select_region", new String[]{"BLAZE_ROD"});
        verify(this.fileConfiguration, times(1)).set("tools.release_pets", new String[]{"APPLE", "BELL"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.tp", new String[]{"teleport", "find", "get"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.allowed", new String[]{"permitted", "guests", "guest", "g"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.all", new String[]{"findall", "getall"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.position1", new String[]{"1", "pos1", "startpos", "start"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.position2", new String[]{"2", "pos2", "endpos", "end"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.clear", new String[]{"wipe"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.release", new String[]{"untame"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.serverstorage", new String[]{"defstorage", "sstorage", "serverstable"});

        verify(this.fileConfiguration, times(1)).set("command_aliases.dogs", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.cats", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.birds", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.horses", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.mules", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.llamas", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.donkeys", null);

        verify(this.fileConfiguration, times(1)).set(eq("protect_pets_from"), this.argumentCaptorStringList.capture());
        List<String> protectPetsFromList = this.argumentCaptorStringList.getValue();
        assertContainsInAnyOrder(protectPetsFromList, "GuestDamage", "StrangerDamage", "OwnerDamage", "EnvironmentalDamage", "MobDamage");

        verifyLoggedPluginInfo(2, 4);
    }

    @Test
    @DisplayName("Updates from 3 run proper modifications and cascade to update 4")
    void threeToFour() {
        when(this.fileConfiguration.getInt("schema_version", 1)).thenReturn(3);
        this.configUpdater = mock(ConfigUpdater.class, withSettings().useConstructor(this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        this.configUpdater.update();

        // 1 to 2
        verify(this.fileConfiguration, never()).set("horse_limit", -1);
        verify(this.fileConfiguration, never()).set("mule_limit", -1);
        verify(this.fileConfiguration, never()).set("llama_limit", -1);
        verify(this.fileConfiguration, never()).set("donkey_limit", -1);
        verify(this.fileConfiguration, never()).set("tools.untame_pets", new String[]{"SHEARS"});
        verify(this.fileConfiguration, never()).set("tools.get_owner", new String[]{"BONE"});
        verify(this.fileConfiguration, never()).set("command_aliases.horses", new String[]{"horse"});
        verify(this.fileConfiguration, never()).set("command_aliases.mules", new String[]{"mule"});
        verify(this.fileConfiguration, never()).set("command_aliases.llamas", new String[]{"llama"});
        verify(this.fileConfiguration, never()).set("command_aliases.donkeys", new String[]{"donkey"});
        verify(this.fileConfiguration, never()).set("command_aliases.rename", new String[]{"setname"});
        verify(this.fileConfiguration, never()).set("command_aliases.allow", new String[]{"add"});
        verify(this.fileConfiguration, never()).set("command_aliases.remove", new String[]{"take"});
        verify(this.fileConfiguration, never()).set("command_aliases.list", new String[]{"show"});

        // 2 to 3
        verify(this.fileConfiguration, never()).set("storage_limit", 5);
        verify(this.fileConfiguration, never()).set("command_aliases.store", new String[]{"move", "stable"});
        verify(this.fileConfiguration, never()).set("command_aliases.storage", new String[]{"setstable"});
        verify(this.fileConfiguration, never()).set("logging.updated_pets", true);
        verify(this.fileConfiguration, never()).set("logging.successful_actions", true);
        verify(this.fileConfiguration, never()).set("logging.unsuccessful_actions", true);
        verify(this.fileConfiguration, never()).set("logging.prevented_damage", true);
        verify(this.fileConfiguration, never()).set("logging.errors", true);

        // 3 to 4
        verify(this.fileConfiguration, times(1)).set("tools.select_region", new String[]{"BLAZE_ROD"});
        verify(this.fileConfiguration, times(1)).set("tools.release_pets", new String[]{"APPLE", "BELL"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.tp", new String[]{"teleport", "find", "get"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.allowed", new String[]{"permitted", "guests", "guest", "g"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.all", new String[]{"findall", "getall"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.position1", new String[]{"1", "pos1", "startpos", "start"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.position2", new String[]{"2", "pos2", "endpos", "end"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.clear", new String[]{"wipe"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.release", new String[]{"untame"});
        verify(this.fileConfiguration, times(1)).set("command_aliases.serverstorage", new String[]{"defstorage", "sstorage", "serverstable"});

        verify(this.fileConfiguration, times(1)).set("command_aliases.dogs", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.cats", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.birds", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.horses", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.mules", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.llamas", null);
        verify(this.fileConfiguration, times(1)).set("command_aliases.donkeys", null);

        verify(this.fileConfiguration, times(1)).set(eq("protect_pets_from"), this.argumentCaptorStringList.capture());
        List<String> protectPetsFromList = this.argumentCaptorStringList.getValue();
        assertContainsInAnyOrder(protectPetsFromList, "GuestDamage", "StrangerDamage", "OwnerDamage", "EnvironmentalDamage", "MobDamage");

        verifyLoggedPluginInfo(3, 4);
    }

    @Test
    @DisplayName("Updates from 3 to 4 assign protect_pets_from based on previous value of OwnerDamage")
    void threeToFourOwnerDamage() {
        when(this.fileConfiguration.getInt("schema_version", 1)).thenReturn(3);
        when(this.fileConfiguration.getStringList("protect_pets_from")).thenReturn(Collections.singletonList("OwnerDamage"));

        this.configUpdater = mock(ConfigUpdater.class, withSettings().useConstructor(this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        this.configUpdater.update();

        verify(this.fileConfiguration, times(1)).set(eq("protect_pets_from"), this.argumentCaptorStringList.capture());
        List<String> protectPetsFromList = this.argumentCaptorStringList.getValue();
        assertContainsInAnyOrder(protectPetsFromList, "OwnerDamage");
        assertEquals(1, protectPetsFromList.size());
    }

    @Test
    @DisplayName("Updates from 3 to 4 assign protect_pets_from based on previous value of EnvironmentalDamage")
    void threeToFourEnvironmentalDamage() {
        when(this.fileConfiguration.getInt("schema_version", 1)).thenReturn(3);
        when(this.fileConfiguration.getStringList("protect_pets_from")).thenReturn(Collections.singletonList("EnvironmentalDamage"));

        this.configUpdater = mock(ConfigUpdater.class, withSettings().useConstructor(this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        this.configUpdater.update();

        verify(this.fileConfiguration, times(1)).set(eq("protect_pets_from"), this.argumentCaptorStringList.capture());
        List<String> protectPetsFromList = this.argumentCaptorStringList.getValue();
        assertContainsInAnyOrder(protectPetsFromList, "EnvironmentalDamage");
        assertEquals(1, protectPetsFromList.size());
    }

    @Test
    @DisplayName("Updates from 3 to 4 assign protect_pets_from based on previous value of MobDamage")
    void threeToFourMobDamage() {
        when(this.fileConfiguration.getInt("schema_version", 1)).thenReturn(3);
        when(this.fileConfiguration.getStringList("protect_pets_from")).thenReturn(Collections.singletonList("MobDamage"));

        this.configUpdater = mock(ConfigUpdater.class, withSettings().useConstructor(this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        this.configUpdater.update();

        verify(this.fileConfiguration, times(1)).set(eq("protect_pets_from"), this.argumentCaptorStringList.capture());
        List<String> protectPetsFromList = this.argumentCaptorStringList.getValue();
        assertContainsInAnyOrder(protectPetsFromList, "MobDamage");
        assertEquals(1, protectPetsFromList.size());
    }

    @Test
    @DisplayName("Updates from 3 to 4 assign protect_pets_from based on previous value of PlayerDamage")
    void threeToFourPlayerDamage() {
        when(this.fileConfiguration.getInt("schema_version", 1)).thenReturn(3);
        when(this.fileConfiguration.getStringList("protect_pets_from")).thenReturn(Collections.singletonList("PlayerDamage"));

        this.configUpdater = mock(ConfigUpdater.class, withSettings().useConstructor(this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        this.configUpdater.update();

        verify(this.fileConfiguration, times(1)).set(eq("protect_pets_from"), this.argumentCaptorStringList.capture());
        List<String> protectPetsFromList = this.argumentCaptorStringList.getValue();
        assertContainsInAnyOrder(protectPetsFromList, "StrangerDamage", "GuestDamage");
        assertEquals(2, protectPetsFromList.size());
    }

    @Test
    @DisplayName("Updates from 3 to 4 assign protect_pets_from if value missing entirely")
    void threeToFourNoProtectPetsPrevValue() {
        when(this.fileConfiguration.getInt("schema_version", 1)).thenReturn(3);
        when(this.fileConfiguration.getStringList("protect_pets_from")).thenReturn(new ArrayList<>());

        this.configUpdater = mock(ConfigUpdater.class, withSettings().useConstructor(this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        this.configUpdater.update();

        verify(this.fileConfiguration, times(1)).set("protect_pets_from", new String[]{"GuestDamage", "StrangerDamage", "OwnerDamage", "EnvironmentalDamage", "MobDamage"});
    }

    @Test
    @DisplayName("Updates from 3 to 4 assign tools.release_pets if value missing entirely")
    void threeToFourNoReleaseToolPrevValue() {
        when(this.fileConfiguration.getInt("schema_version", 1)).thenReturn(3);
        when(this.fileConfiguration.getStringList("tools.untame_pets")).thenReturn(new ArrayList<>());

        this.configUpdater = mock(ConfigUpdater.class, withSettings().useConstructor(this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        this.configUpdater.update();

        verify(this.fileConfiguration, times(1)).set("tools.release_pets", new String[]{"SHEARS"});
    }
}
