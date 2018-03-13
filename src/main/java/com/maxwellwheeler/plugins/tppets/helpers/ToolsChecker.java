package com.maxwellwheeler.plugins.tppets.helpers;

import org.bukkit.Material;

import java.util.Hashtable;
import java.util.List;

public class ToolsChecker {
    public static boolean isInList(Hashtable<String, List<Material>> matsIndex, String key, Material mat) {
        return matsIndex.containsKey(key) && matsIndex.get(key).contains(mat);
    }
}
