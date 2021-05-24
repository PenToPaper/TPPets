package com.maxwellwheeler.plugins.tppets.helpers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Used to cache and compare custom configured tools.
 * @author GatheringExp
 */
public class ToolsManager {
    /** A hashtable of &lt;Tool Type, List&lt;Valid materials&gt;&gt;*/
    private final Hashtable<String, List<Material>> customTools;

    /**
     * Initializes the custom tools from a provided {@link ConfigurationSection}
     * @param toolsConfig The configuration section to parse for custom tools. Typically config's tools section.
     */
    public ToolsManager(ConfigurationSection toolsConfig) {
        this.customTools = new Hashtable<>();
        initializeCustomToolsFromConfig(toolsConfig);
    }

    /**
     * Populates {@link ToolsManager#customTools} with all custom tools in the provided configuration section. The tool
     * type does not have to be used by the plugin to be populated here.
     * @param toolsConfig The configuration section to parse for custom tools. Typically config's tools section.
     */
    private void initializeCustomToolsFromConfig(ConfigurationSection toolsConfig) {
        for (String toolUse : toolsConfig.getKeys(false)) {
            List<Material> toolMaterials = new ArrayList<>();
            for (String materialName : toolsConfig.getStringList(toolUse)) {
                Material material = Material.getMaterial(materialName);
                if (material != null) {
                    toolMaterials.add(material);
                }
            }
            this.customTools.put(toolUse, toolMaterials);
        }
    }

    /**
     * Determines if a given tool is a valid tool of an expected tool type.
     * @param toolType A string representing an expected tool type. Ex: release_pets.
     * @param material The material to evaluate against the expected tool type.
     * @return true if valid material, false if not.
     */
    public boolean isMaterialValidTool(String toolType, Material material) {
        return this.customTools.containsKey(toolType) && this.customTools.get(toolType).contains(material);
    }
}
