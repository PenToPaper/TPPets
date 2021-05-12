package com.maxwellwheeler.plugins.tppets.test.helpers;

import com.maxwellwheeler.plugins.tppets.helpers.ToolsManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TPPToolsManagerTest {
    private ToolsManager toolsManager;
    private ConfigurationSection configurationSection;

    @BeforeEach
    public void beforeEach() {
        this.configurationSection = mock(ConfigurationSection.class);

        Set<String> toolsConfigKeys = new HashSet<>();
        toolsConfigKeys.add("release_pets");
        toolsConfigKeys.add("get_owner");

        List<String> releasePets = Arrays.asList("SHEARS", "APPLE");
        List<String> getOwner = Arrays.asList("BONE", "BAMBOO");

        when(this.configurationSection.getKeys(false)).thenReturn(toolsConfigKeys);
        when(this.configurationSection.getStringList("release_pets")).thenReturn(releasePets);
        when(this.configurationSection.getStringList("get_owner")).thenReturn(getOwner);

        this.toolsManager = new ToolsManager(this.configurationSection);
    }

    @Test
    @DisplayName("Initializes correct tool table")
    void initializesToolTable() {
        assertTrue(this.toolsManager.isMaterialValidTool("release_pets", Material.SHEARS));
        assertTrue(this.toolsManager.isMaterialValidTool("release_pets", Material.APPLE));
        assertFalse(this.toolsManager.isMaterialValidTool("release_pets", Material.BELL));
        assertTrue(this.toolsManager.isMaterialValidTool("get_owner", Material.BONE));
        assertTrue(this.toolsManager.isMaterialValidTool("get_owner", Material.BAMBOO));
        assertFalse(this.toolsManager.isMaterialValidTool("get_owner", Material.BELL));
    }

    @Test
    @DisplayName("Doesn't attempt to initialize invalid materials")
    void doesntInitializeInvalidMaterials() {
        List<String> releasePets = Arrays.asList("INVALIDMATERIAL", "OTHERINVALIDMATERIAL", "APPLE");

        when(this.configurationSection.getStringList("release_pets")).thenReturn(releasePets);

        this.toolsManager = new ToolsManager(this.configurationSection);

        assertTrue(this.toolsManager.isMaterialValidTool("release_pets", Material.APPLE));
    }

    @Test
    @DisplayName("Reurns false if no tool of type")
    void returnsFalseNoTool() {
        assertFalse(this.toolsManager.isMaterialValidTool("not_in_table", Material.SHEARS));
        assertFalse(this.toolsManager.isMaterialValidTool("not_in_table", Material.APPLE));
        assertFalse(this.toolsManager.isMaterialValidTool("not_in_table", Material.BONE));
    }
}
