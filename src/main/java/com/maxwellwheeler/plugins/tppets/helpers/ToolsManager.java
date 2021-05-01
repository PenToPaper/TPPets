package com.maxwellwheeler.plugins.tppets.helpers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class ToolsManager {
    private final Hashtable<String, List<Material>> customTools;

    public ToolsManager(ConfigurationSection toolsConfig) {
        this.customTools = new Hashtable<>();
        initializeCustomToolsFromConfig(toolsConfig);
    }

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

    public boolean isMaterialValidTool(String toolType, Material material) {
        return this.customTools.containsKey(toolType) && this.customTools.get(toolType).contains(material);
    }
}
