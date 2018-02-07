package com.maxwellwheeler.plugins.tppets.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.region.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

public class CommandLF extends RegionCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player && we != null) {
            Player pl = (Player) sender;
            Selection playerSelection = we.getSelection(pl);
            if (playerSelection != null && playerSelection instanceof CuboidSelection) {
                Location minPoint = playerSelection.getMinimumPoint();
                Location maxPoint = playerSelection.getMaximumPoint();
                LostAndFoundRegion lfr = new LostAndFoundRegion(pl.getWorld().getName(), minPoint.getBlockX(), minPoint.getBlockY(), minPoint.getBlockZ(), maxPoint.getBlockX(), maxPoint.getBlockY(), maxPoint.getBlockZ());
                lfr.writeToConfig(thisPlugin);
                thisPlugin.addLostAndFoundRegion(lfr);
                return true;
            }
        }
        return false;
    }
}