package com.maxwellwheeler.plugins.tppets.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

public class RegionCommand {
    protected TPPets thisPlugin;
    protected WorldEditPlugin we;
    
    public RegionCommand() {
        thisPlugin = (TPPets) Bukkit.getServer().getPluginManager().getPlugin("TPPets");
        we = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
    }
    
    protected Location[] getWePoints(Player pl) {
        Selection playerSelection = we.getSelection(pl);
        Location[] ret = null;
        if (playerSelection != null && playerSelection instanceof CuboidSelection) {
            ret = new Location[] {playerSelection.getMinimumPoint(), playerSelection.getMaximumPoint()};
        }
        return ret;
    }
    
    protected String getLocationString(Location lc) {
        return Integer.toString(lc.getBlockX()) + ", " + Integer.toString(lc.getBlockY()) + ", " + Integer.toString(lc.getBlockZ());
    }
    
    protected boolean validateArgs(String[] args, int length) {
        if (args.length >= length) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
