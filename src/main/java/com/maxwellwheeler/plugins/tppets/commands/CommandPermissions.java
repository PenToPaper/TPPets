package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class CommandPermissions {
    TPPets thisPlugin;
    public CommandPermissions(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    @SuppressWarnings("deprecation")
    public void allowPlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player && ArgValidator.validateArgsLength(args, 2) && ArgValidator.validateUsername(args[0]) && ArgValidator.softValidatePetName(args[1])) {
            String playerUUID = UUIDUtils.trimUUID(Bukkit.getOfflinePlayer(args[0]).getUniqueId());
            String petUUID = thisPlugin.getDatabase().getPetUUIDByName(((Player) sender).getUniqueId().toString(), args[1]);
            if (playerUUID != null && petUUID != null && !petUUID.equals("") && thisPlugin.getDatabase().insertAllowedPlayer(petUUID, playerUUID)) {
                thisPlugin.getAllowedPlayers().get(petUUID).add(playerUUID);
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

    @SuppressWarnings("deprecation")
    public void removePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player && ArgValidator.validateArgsLength(args, 2) && ArgValidator.validateUsername(args[0]) && ArgValidator.softValidatePetName(args[1])) {
            String playerUUID = UUIDUtils.trimUUID(Bukkit.getOfflinePlayer(args[0]).getUniqueId());
            String petUUID = thisPlugin.getDatabase().getPetUUIDByName(((Player) sender).getUniqueId().toString(), args[1]);
            if (playerUUID != null && petUUID != null && !petUUID.equals("") && thisPlugin.getDatabase().deleteAllowedPlayer(petUUID, playerUUID)) {
                thisPlugin.getAllowedPlayers().get(petUUID).remove(playerUUID);
                thisPlugin.getLogger().info("Player " + sender.getName() + " disallowed " + args[0] + " to use their pet " + args[1]);
                sender.sendMessage(ChatColor.WHITE + args[0] + ChatColor.BLUE + " is no longer allowed to use " + ChatColor.WHITE + args[0]);
            } else {
                thisPlugin.getLogger().info("Error disallowing player " + args[0] + " from pet " + args[1]);
                sender.sendMessage(ChatColor.RED + "Can't revoke permission.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Syntax error! /tpp remove [username] [animal name]");
        }
    }

    public void listPlayers(CommandSender sender, String[] args) {
        if (sender instanceof Player && ArgValidator.validateArgsLength(args, 1) && ArgValidator.softValidatePetName(args[0])) {
            String petUUID = thisPlugin.getDatabase().getPetUUIDByName(((Player) sender).getUniqueId().toString(), args[0]);
            if (petUUID != null && !petUUID.equals("")) {
                List<String> playerUUIDs = thisPlugin.getDatabase().getAllowedPlayers(petUUID);
                sender.sendMessage(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE +  args[0] + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------");
                for (String playerUUID : playerUUIDs) {
                    OfflinePlayer offlinePlTemp = Bukkit.getOfflinePlayer(UUID.fromString(UUIDUtils.unTrimUUID(playerUUID)));
                    if (offlinePlTemp != null) {
                        sender.sendMessage(ChatColor.WHITE + offlinePlTemp.getName());
                    }
                }
                sender.sendMessage(ChatColor.GRAY + "-------------------------------------------");
            } else {
                sender.sendMessage(ChatColor.RED + "Can't find pet with name" + ChatColor.WHITE + args[0]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You must be a player!");
        }
    }
}
