package com.maxwellwheeler.plugins.tppets.commands;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandTpForward implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player pl = (Player) sender;
            Location currentLocation = pl.getLocation();
            pl.teleport(new Location(currentLocation.getWorld(), currentLocation.getX() + Integer.parseInt(args[0]), 80, currentLocation.getZ()));
        }
        // TODO Auto-generated method stub
        return false;
    }

}
