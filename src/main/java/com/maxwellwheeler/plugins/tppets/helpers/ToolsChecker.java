package com.maxwellwheeler.plugins.tppets.helpers;

import org.bukkit.Material;

import java.util.Hashtable;
import java.util.List;

/**
 * Basic object to check if tools are valid, used mostly in {@link com.maxwellwheeler.plugins.tppets.listeners.TPPetsPlayerListener}
 * @author GatheringExp
 */
public class ToolsChecker {
    /**
     * Checks if the given material is in a list of materials, provided by the customTools Hashtable
     * @param matsIndex customTools Hashtable
     * @param key The key of the Hashtable
     * @param mat The material to check
     * @return True if valid, false if not
     */
    public static boolean isInList(Hashtable<String, List<Material>> matsIndex, String key, Material mat) {
        return matsIndex.containsKey(key) && matsIndex.get(key).contains(mat);
    }
}
