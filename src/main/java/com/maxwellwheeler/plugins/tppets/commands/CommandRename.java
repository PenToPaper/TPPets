package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class CommandRename {
    private TPPets thisPlugin;
    public CommandRename(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    @SuppressWarnings("deprecation")
    public void processCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player playerTemp = (Player) sender;
            if (ArgValidator.validateArgsLength(args,2)) {
                String someoneElse = ArgValidator.isForSomeoneElse(args[0]);
                if (someoneElse != null && sender.hasPermission("tppets.renameall") && ArgValidator.validateArgsLength(args, 3) && ArgValidator.softValidatePetName(args[1]) && ArgValidator.softValidatePetName(args[2])) {
                    OfflinePlayer offlinePlayerTemp = Bukkit.getOfflinePlayer(someoneElse);
                    if (offlinePlayerTemp != null) {
                        renamePet(playerTemp, offlinePlayerTemp, args[1], args[2]);
                    } else {
                        playerTemp.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + someoneElse);
                    }
                } else if (ArgValidator.softValidatePetName(args[0]) && ArgValidator.softValidatePetName(args[1])) {
                    renamePet(playerTemp, playerTemp, args[0], args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Syntax error. Usage: /tpp rename [old name] [new name]");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Syntax error. Usage: /tpp rename [old name] [new name]");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Error: Sender is not a player.");
        }
    }

    private boolean renamePet(Player pl, OfflinePlayer commandAbout, String oldName, String newName) {
        String petUUIDByName = thisPlugin.getDatabase().getPetUUIDByName(commandAbout.getUniqueId().toString(), oldName);
        if (petUUIDByName.equals("") || petUUIDByName == null) {
            pl.sendMessage(ChatColor.RED + "Can't find pet named " + ChatColor.WHITE + oldName);
            return false;
        }
        if (!ArgValidator.validatePetName(thisPlugin.getDatabase(), commandAbout.getUniqueId().toString(), newName)) {
            pl.sendMessage(ChatColor.RED + "Can't rename pet to " + ChatColor.WHITE + newName);
            return false;
        }
        if (thisPlugin.getDatabase().renamePet(commandAbout.getUniqueId().toString(), oldName, newName)) {
            pl.sendMessage(ChatColor.BLUE + "Renamed pet " + ChatColor.WHITE + oldName + ChatColor.BLUE + " to " + ChatColor.WHITE + newName);
            return true;
        }
        pl.sendMessage(ChatColor.RED + "Unable to rename pet");
        return false;
    }
}
