package com.maxwellwheeler.plugins.tppets.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;

public class CommandNoPets extends RegionCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player && we != null) {
            Player pl = (Player) sender;
            Selection playerSelection = we.getSelection(pl);
            if (playerSelection != null && playerSelection instanceof CuboidSelection && args.length == 2) {
                Location minPoint = playerSelection.getMinimumPoint();
                Location maxPoint = playerSelection.getMaximumPoint();
                ProtectedRegion pr = new ProtectedRegion(args[0], args[1], pl.getWorld().getName(), minPoint.getBlockX(), minPoint.getBlockY(), minPoint.getBlockZ(), maxPoint.getBlockX(), maxPoint.getBlockY(), maxPoint.getBlockZ());
                pr.writeToConfig(thisPlugin);
                thisPlugin.addProtectedRegion(pr);
                return true;
            }
        }
        return false;
    }
}
