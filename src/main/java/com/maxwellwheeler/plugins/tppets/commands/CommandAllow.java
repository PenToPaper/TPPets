package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandAllow {
    TPPets thisPlugin;
    public CommandAllow(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    public void processCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && ArgValidator.validateArgs(args, 2) && ArgValidator.validateUsername(args[0]) && ArgValidator.softValidatePetName(args[1])) {
            String playerUUID = ((Player) sender).getUniqueId().toString();
            String petUUID = thisPlugin.getDatabase().getPetUUIDByName(playerUUID, args[1]);
            if (thisPlugin.getDatabase().insertAllowedPlayer(petUUID, playerUUID)) {
                thisPlugin.getLogger().info("Player " + sender.getName() + " allowed " + args[0] + " to use their pet " + args[1]);
                sender.sendMessage(ChatColor.WHITE + args[0] + ChatColor.BLUE + " has been allowed to use " + ChatColor.WHITE + args[0]);
            } else {
                thisPlugin.getLogger().info("Error allowing player " + args[0] + " to pet " + args[1]);
                sender.sendMessage(ChatColor.RED + "Can't allow player to your pet");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Syntax error! /tpp allow [username] [animal name]");
        }
    }
}
