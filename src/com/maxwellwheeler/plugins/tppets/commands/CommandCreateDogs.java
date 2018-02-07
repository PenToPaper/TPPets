package com.maxwellwheeler.plugins.tppets.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

public class CommandCreateDogs implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            for (int i = 0; i < Integer.parseInt(args[0]); i++) {
                Player pl = (Player) sender;
                Entity ent = pl.getWorld().spawnEntity(pl.getLocation(), EntityType.WOLF);
                Tameable tmb = (Tameable) ent;
                tmb.setOwner(pl);
            }
        }
        // TODO Auto-generated method stub
        return false;
    }
    
}
