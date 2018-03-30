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

class CommandPermissions {
    private TPPets thisPlugin;
    public CommandPermissions(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    // Desired syntax: /tpp allow from:PlayerName [Allowed Player Name] [Pet Name]
    // Desired syntax: /tpp allow [Allowed Player Name] [Pet Name]
    @SuppressWarnings("deprecation")
    public void allowPlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player tempPlayer = (Player) sender;
            if (ArgValidator.validateArgsLength(args, 3) && ArgValidator.validateUsername(args[1]) && ArgValidator.softValidatePetName(args[2]) && tempPlayer.hasPermission("tppets.allowother")) {
                String playerFor = ArgValidator.isForSomeoneElse(args[0]);
                if (playerFor != null) {
                    // Syntax received: /tpp allow from:PlayerName [Allowed Player Name] [Pet Name]
                    OfflinePlayer from = Bukkit.getOfflinePlayer(playerFor);
                    if (from != null) {
                        if (addAllowedPlayer(from, args[1], args[2])) {
                            thisPlugin.getLogger().info(tempPlayer.getName() + " allowed " + args[1] + " to use " + playerFor + "'s pet named " + args[2]);
                            tempPlayer.sendMessage(ChatColor.WHITE + args[1] + ChatColor.BLUE + " has been allowed to use " + ChatColor.WHITE + playerFor + ChatColor.BLUE + "'s pet " + ChatColor.WHITE + args[2]);
                        }
                    }
                }
            } else if (ArgValidator.validateArgsLength(args, 2) && ArgValidator.validateUsername(args[0]) && ArgValidator.softValidatePetName(args[1])) {
                // Syntax received: /tpp allow [Allowed Player Name] [Pet Name]
                if (addAllowedPlayer(tempPlayer, args[0], args[1])) {
                    thisPlugin.getLogger().info(tempPlayer.getName() + " allowed " + args[0] + " to use their pet named " + args[1]);
                    tempPlayer.sendMessage(ChatColor.WHITE + args[0] + ChatColor.BLUE + " has been allowed to use your pet " + ChatColor.WHITE + args[1]);
                }
            } else {
                tempPlayer.sendMessage(ChatColor.RED + "Syntax error! /tpp allow [username] [animal name]");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Could not process command");
        }
    }

    @SuppressWarnings("deprecation")
    private boolean addAllowedPlayer(OfflinePlayer from, String addedPlayerName, String petName) {
        OfflinePlayer addedPlayer = Bukkit.getOfflinePlayer(addedPlayerName);
        if (addedPlayer != null) {
            String addedPlayerUUID = UUIDUtils.trimUUID(addedPlayer.getUniqueId());
            String petUUID = thisPlugin.getDatabase().getPetUUIDByName(from.getUniqueId().toString(), petName);
            if (addedPlayerUUID != null && petUUID != null && !petUUID.equals("") && thisPlugin.getDatabase().insertAllowedPlayer(petUUID, addedPlayerUUID)) {
                thisPlugin.getAllowedPlayers().get(petUUID).add(addedPlayerUUID);
                return true;
            }
        }
        return false;
    }

    // Desired syntax: /tpp remove from:PlayerName [Allowed Player Name] [Pet Name]
    // Desired syntax: /tpp remove [Allowed Player Name] [Pet Name]
    @SuppressWarnings("deprecation")
    public void removePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player tempPlayer = (Player) sender;
            if (ArgValidator.validateArgsLength(args, 3) && ArgValidator.validateUsername(args[1]) && ArgValidator.softValidatePetName(args[2]) && tempPlayer.hasPermission("tppets.allowother")) {
                String playerFor = ArgValidator.isForSomeoneElse(args[0]);
                if (playerFor != null) {
                    // Syntax received: /tpp remove from:PlayerName [Allowed Player Name] [Pet Name]
                    OfflinePlayer from = Bukkit.getOfflinePlayer(playerFor);
                    if (from != null) {
                        if (removeAllowedPlayer(from, args[1], args[2])) {
                            thisPlugin.getLogger().info(tempPlayer.getName() + " disallowed " + args[1] + " to use " + playerFor + "'s pet named " + args[2]);
                            tempPlayer.sendMessage(ChatColor.WHITE + args[1] + ChatColor.BLUE + " is no longer allowed to use " + ChatColor.WHITE + playerFor + ChatColor.BLUE + "'s pet " + ChatColor.WHITE + args[2]);
                        }
                    }
                }
            } else if (ArgValidator.validateArgsLength(args, 2) && ArgValidator.validateUsername(args[0]) && ArgValidator.softValidatePetName(args[1])) {
                // Syntax received: /tpp remove [Allowed Player Name] [Pet Name]
                if (removeAllowedPlayer(tempPlayer, args[0], args[1])) {
                    thisPlugin.getLogger().info(tempPlayer.getName() + " disallowed " + args[0] + " to use their pet named " + args[1]);
                    tempPlayer.sendMessage(ChatColor.WHITE + args[0] + ChatColor.BLUE + " is no longer allowed to use " + ChatColor.WHITE + args[1]);
                }
            } else {
                tempPlayer.sendMessage(ChatColor.RED + "Syntax error! /tpp remove [username] [animal name]");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Could not process command");
        }
    }

    @SuppressWarnings("deprecation")
    private boolean removeAllowedPlayer(OfflinePlayer from, String removedPlayerName, String petName) {
        OfflinePlayer removedPlayer = Bukkit.getOfflinePlayer(removedPlayerName);
        if (removedPlayer != null) {
            String removedPlayerUUID = UUIDUtils.trimUUID(removedPlayer.getUniqueId());
            String petUUID = thisPlugin.getDatabase().getPetUUIDByName(from.getUniqueId().toString(), petName);
            if (removedPlayerUUID != null && petUUID != null && !petUUID.equals("") && thisPlugin.getDatabase().deleteAllowedPlayer(petUUID, removedPlayerUUID)) {
                thisPlugin.getAllowedPlayers().get(petUUID).remove(removedPlayerUUID);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public void listPlayers(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            if (ArgValidator.validateArgsLength(args, 2) && ArgValidator.softValidatePetName(args[1]) && sender.hasPermission("tppets.allowother")) {
                String petOwnerName = ArgValidator.isForSomeoneElse(args[0]);
                if (petOwnerName != null) {
                    OfflinePlayer petOwner = Bukkit.getOfflinePlayer(petOwnerName);
                    if (!listAllowedPlayers((Player) sender, petOwner, args[1])) {
                        sender.sendMessage(ChatColor.RED + "Can't list pets from " + ChatColor.WHITE + petOwnerName);
                    }
                }
            } else if (ArgValidator.validateArgsLength(args, 1) && ArgValidator.softValidatePetName(args[0])) {
                if (!listAllowedPlayers((Player) sender, (Player) sender, args[0])) {
                    sender.sendMessage(ChatColor.RED + "Can't list pets");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Syntax error! Usage: /tpp list [pet name]");
            }
        }
    }

    private boolean listAllowedPlayers(Player reportTo, OfflinePlayer petOwner, String petName) {
        String petUUID = thisPlugin.getDatabase().getPetUUIDByName(petOwner.getUniqueId().toString(), petName);
        if (petUUID != null && !petUUID.equals("")) {
            List<String> playerUUIDs = thisPlugin.getDatabase().getAllowedPlayers(petUUID);
            reportTo.sendMessage(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE +  petOwner.getName() + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------");
            for (String playerUUID : playerUUIDs) {
                String untrimmedUUID = UUIDUtils.unTrimUUID(playerUUID);
                if (untrimmedUUID != null) {
                    OfflinePlayer offlinePlTemp = Bukkit.getOfflinePlayer(UUID.fromString(untrimmedUUID));
                    if (offlinePlTemp != null) {
                        reportTo.sendMessage(ChatColor.WHITE + offlinePlTemp.getName());
                    }
                }
            }
            reportTo.sendMessage(ChatColor.GRAY + "-------------------------------------------");
            return true;
        }
        return false;
    }
}
