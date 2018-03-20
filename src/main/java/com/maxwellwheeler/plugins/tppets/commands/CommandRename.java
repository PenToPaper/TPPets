package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.CheckArgs;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandRename {
    TPPets thisPlugin;
    public CommandRename(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    public void processCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player playerTemp = (Player) sender;
            if (CheckArgs.validateArgs(args,2)) {
                renamePet(playerTemp, args[0], args[1]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Error: Sender is not a player.");
        }
    }

    private boolean renamePet(Player pl, String oldName, String newName) {
        if (thisPlugin.getDatabase().renamePet(pl.getUniqueId().toString(), oldName, newName)) {
            pl.sendMessage(ChatColor.BLUE + "Renamed pet " + ChatColor.WHITE + oldName + ChatColor.BLUE + " to " + ChatColor.WHITE + newName);
            return true;
        } else {
            pl.sendMessage(ChatColor.RED + "Unable to rename pet");
            return false;
        }
    }
}
