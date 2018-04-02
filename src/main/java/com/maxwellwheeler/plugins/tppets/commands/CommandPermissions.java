package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class CommandPermissions {
    private TPPets thisPlugin;
    public CommandPermissions(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    private enum EditResult {SUCCESS, ALREADY_DONE, NO_PLAYER, FAILURE}

    // Desired syntax: /tpp allow from:PlayerName [Allowed Player Name] [Pet Name]
    // Desired syntax: /tpp allow [Allowed Player Name] [Pet Name]
    @SuppressWarnings("deprecation")
    public void allowPlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player tempPlayer = (Player) sender;
            if (ArgValidator.validateArgsLength(args, 3) && ArgValidator.validateUsername(args[1]) && ArgValidator.softValidatePetName(args[2])) {
                String playerFor = ArgValidator.isForSomeoneElse(args[0]);
                if (playerFor != null) {
                    if (!sender.hasPermission("tppets.allowother")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                        return;
                    }
                    // Syntax received: /tpp allow from:PlayerName [Allowed Player Name] [Pet Name]
                    OfflinePlayer from = Bukkit.getOfflinePlayer(playerFor);
                    if (from != null) {
                        if (!from.hasPlayedBefore()) {
                            tempPlayer.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + playerFor);
                            return;
                        }
                        EditResult res = addAllowedPlayer(from, args[1], args[2]);
                        switch (addAllowedPlayer(from, args[1], args[2])) {
                            case SUCCESS:
                                thisPlugin.getLogger().info(tempPlayer.getName() + " allowed " + args[1] + " to use " + playerFor + "'s pet named " + args[2]);
                                tempPlayer.sendMessage(ChatColor.WHITE + args[1] + ChatColor.BLUE + " has been allowed to use " + ChatColor.WHITE + playerFor + ChatColor.BLUE + "'s pet " + ChatColor.WHITE + args[2]);
                                break;
                            case ALREADY_DONE:
                                tempPlayer.sendMessage(ChatColor.WHITE + args[1] + ChatColor.BLUE + " is already allowed to use " + ChatColor.WHITE + playerFor + ChatColor.BLUE + "'s pet" + ChatColor.WHITE + args[2]);
                                break;
                            case NO_PLAYER:
                                tempPlayer.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + args[1]);
                                break;
                            default:
                                tempPlayer.sendMessage(ChatColor.RED + "Can't add player to pet");
                                break;
                        }
                    } else {
                        tempPlayer.sendMessage(ChatColor.RED + "Could not find player named " + args[0]);
                    }
                } else {
                    tempPlayer.sendMessage(ChatColor.RED + "Could not find player named " + args[0]);
                }
            } else if (ArgValidator.validateArgsLength(args, 2) && ArgValidator.validateUsername(args[0]) && ArgValidator.softValidatePetName(args[1])) {
                // Syntax received: /tpp allow [Allowed Player Name] [Pet Name]
                switch (addAllowedPlayer(tempPlayer, args[0], args[1])) {
                    case SUCCESS:
                        thisPlugin.getLogger().info(tempPlayer.getName() + " allowed " + args[0] + " to use their pet named " + args[1]);
                        tempPlayer.sendMessage(ChatColor.WHITE + args[0] + ChatColor.BLUE + " has been allowed to use your pet " + ChatColor.WHITE + args[1]);
                        break;
                    case ALREADY_DONE:
                        tempPlayer.sendMessage(ChatColor.WHITE + args[0] + ChatColor.BLUE + " is already allowed to use " + ChatColor.WHITE + args[1]);
                        break;
                    case NO_PLAYER:
                        tempPlayer.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + args[0]);
                        break;
                    default:
                        tempPlayer.sendMessage(ChatColor.RED + "Can't add player to pet");
                        break;
                }
            } else {
                tempPlayer.sendMessage(ChatColor.RED + "Syntax error! /tpp allow [username] [animal name]");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Could not process command");
        }
    }

    @SuppressWarnings("deprecation")
    private EditResult addAllowedPlayer(OfflinePlayer from, String addedPlayerName, String petName) {
        OfflinePlayer addedPlayer = Bukkit.getOfflinePlayer(addedPlayerName);
        if (addedPlayer != null) {
            if (!addedPlayer.hasPlayedBefore()) {
                return EditResult.NO_PLAYER;
            }
            String addedPlayerUUID = UUIDUtils.trimUUID(addedPlayer.getUniqueId());
            String petUUID = thisPlugin.getDatabase().getPetUUIDByName(from.getUniqueId().toString(), petName);
            if (addedPlayerUUID != null && petUUID != null && !petUUID.equals("")) {
                if (thisPlugin.isAllowedToPet(petUUID, addedPlayerUUID)) {
                    return EditResult.ALREADY_DONE;
                }
                if (thisPlugin.getDatabase().insertAllowedPlayer(petUUID, addedPlayerUUID)) {
                    if (!thisPlugin.getAllowedPlayers().containsKey(petUUID)) {
                        thisPlugin.getAllowedPlayers().put(petUUID, new ArrayList<>());
                    }
                    thisPlugin.getAllowedPlayers().get(petUUID).add(addedPlayerUUID);
                    return EditResult.SUCCESS;
                }
            }
        }
        return EditResult.FAILURE;
    }

    // Desired syntax: /tpp remove from:PlayerName [Allowed Player Name] [Pet Name]
    // Desired syntax: /tpp remove [Allowed Player Name] [Pet Name]
    @SuppressWarnings("deprecation")
    public void removePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player tempPlayer = (Player) sender;
            if (ArgValidator.validateArgsLength(args, 3) && ArgValidator.validateUsername(args[1]) && ArgValidator.softValidatePetName(args[2])) {
                String playerFor = ArgValidator.isForSomeoneElse(args[0]);
                if (playerFor != null) {
                    if (!sender.hasPermission("tppets.allowother")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                        return;
                    }
                    // Syntax received: /tpp remove from:PlayerName [Allowed Player Name] [Pet Name]
                    OfflinePlayer from = Bukkit.getOfflinePlayer(playerFor);
                    if (from != null) {
                        if (!from.hasPlayedBefore()) {
                            tempPlayer.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + playerFor);
                            return;
                        }
                        switch (removeAllowedPlayer(from, args[1], args[2])) {
                            case SUCCESS:
                                thisPlugin.getLogger().info(tempPlayer.getName() + " disallowed " + args[1] + " to use " + playerFor + "'s pet named " + args[2]);
                                tempPlayer.sendMessage(ChatColor.WHITE + args[1] + ChatColor.BLUE + " is no longer allowed to use " + ChatColor.WHITE + playerFor + ChatColor.BLUE + "'s pet " + ChatColor.WHITE + args[2]);
                                break;
                            case ALREADY_DONE:
                                tempPlayer.sendMessage(ChatColor.WHITE + args[1] + ChatColor.BLUE + " is already not allowed to use " + ChatColor.WHITE + playerFor + ChatColor.BLUE + "'s pet " + ChatColor.WHITE + args[2]);
                                break;
                            case NO_PLAYER:
                                tempPlayer.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + args[1]);
                                break;
                            default:
                                tempPlayer.sendMessage(ChatColor.RED + "Can't remove player from pet.");
                                break;
                        }
                    }
                } else {
                    tempPlayer.sendMessage(ChatColor.RED + "Could not find player named " + args[0]);
                }
            } else if (ArgValidator.validateArgsLength(args, 2) && ArgValidator.validateUsername(args[0]) && ArgValidator.softValidatePetName(args[1])) {
                // Syntax received: /tpp remove [Allowed Player Name] [Pet Name]
                switch (removeAllowedPlayer(tempPlayer, args[0], args[1])) {
                    case SUCCESS:
                        thisPlugin.getLogger().info(tempPlayer.getName() + " disallowed " + args[0] + " to use their pet named " + args[1]);
                        tempPlayer.sendMessage(ChatColor.WHITE + args[0] + ChatColor.BLUE + " is no longer allowed to use " + ChatColor.WHITE + args[1]);
                        break;
                    case ALREADY_DONE:
                        tempPlayer.sendMessage(ChatColor.WHITE + args[0] + ChatColor.BLUE + " is already not allowed to use " + ChatColor.WHITE + args[1]);
                        break;
                    case NO_PLAYER:
                        tempPlayer.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + args[0]);
                        break;
                    default:
                        tempPlayer.sendMessage(ChatColor.RED + "Can't remove player from pet.");
                        break;
                }
            } else {
                tempPlayer.sendMessage(ChatColor.RED + "Syntax error! /tpp remove [username] [animal name]");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Could not process command");
        }
    }

    @SuppressWarnings("deprecation")
    private EditResult removeAllowedPlayer(OfflinePlayer from, String removedPlayerName, String petName) {
        OfflinePlayer removedPlayer = Bukkit.getOfflinePlayer(removedPlayerName);
        if (removedPlayer != null) {
            if (!removedPlayer.hasPlayedBefore()) {
                return EditResult.NO_PLAYER;
            }
            String removedPlayerUUID = UUIDUtils.trimUUID(removedPlayer.getUniqueId());
            String petUUID = thisPlugin.getDatabase().getPetUUIDByName(from.getUniqueId().toString(), petName);
            if (removedPlayerUUID != null && petUUID != null && !petUUID.equals("")) {
                if (!thisPlugin.isAllowedToPet(petUUID, removedPlayerUUID)) {
                    return EditResult.ALREADY_DONE;
                }
                if (thisPlugin.getDatabase().deleteAllowedPlayer(petUUID, removedPlayerUUID)) {
                    if (!thisPlugin.getAllowedPlayers().containsKey(petUUID)) {
                        thisPlugin.getAllowedPlayers().put(petUUID, new ArrayList<>());
                    }
                    thisPlugin.getAllowedPlayers().get(petUUID).remove(removedPlayerUUID);
                    return EditResult.SUCCESS;
                }
            }
        }
        return EditResult.FAILURE;
    }

    @SuppressWarnings("deprecation")
    public void listPlayers(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player playerTemp = (Player) sender;
            if (ArgValidator.validateArgsLength(args, 2) && ArgValidator.softValidatePetName(args[1])) {
                String petOwnerName = ArgValidator.isForSomeoneElse(args[0]);
                if (petOwnerName != null) {
                    if (!sender.hasPermission("tppets.allowother")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                        return;
                    }
                    OfflinePlayer petOwner = Bukkit.getOfflinePlayer(petOwnerName);
                    switch (listAllowedPlayers(playerTemp, petOwner, args[1])) {
                        case NO_PLAYER:
                            playerTemp.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + petOwnerName);
                            break;
                        default:
                            playerTemp.sendMessage(ChatColor.RED + "Can't list players allowed to pet.");
                            break;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Could not find player named " + args[0]);
                }
            } else if (ArgValidator.validateArgsLength(args, 1) && ArgValidator.softValidatePetName(args[0])) {
                if (!listAllowedPlayers(playerTemp, playerTemp, args[0]).equals(EditResult.SUCCESS)) {
                    playerTemp.sendMessage(ChatColor.RED + "Can't list players allowed to pet.");

                }
            } else {
                sender.sendMessage(ChatColor.RED + "Syntax error! Usage: /tpp list [pet name]");
            }
        }
    }

    private EditResult listAllowedPlayers(Player reportTo, OfflinePlayer petOwner, String petName) {
        if (!petOwner.hasPlayedBefore()) {
            return EditResult.NO_PLAYER;
        }
        String petUUID = thisPlugin.getDatabase().getPetUUIDByName(petOwner.getUniqueId().toString(), petName);
        if (petUUID != null && !petUUID.equals("")) {
            List<String> playerUUIDs = thisPlugin.getDatabase().getAllowedPlayers(petUUID);
            reportTo.sendMessage(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE +  petOwner.getName() + "'s " + petName + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------");
            for (String playerUUID : playerUUIDs) {
                String untrimmedUUID = UUIDUtils.unTrimUUID(playerUUID);
                if (untrimmedUUID != null) {
                    OfflinePlayer offlinePlTemp = Bukkit.getOfflinePlayer(UUID.fromString(untrimmedUUID));
                    if (offlinePlTemp != null && offlinePlTemp.hasPlayedBefore()) {
                        reportTo.sendMessage(ChatColor.WHITE + offlinePlTemp.getName());
                    }
                }
            }
            reportTo.sendMessage(ChatColor.GRAY + "-------------------------------------------");
            return EditResult.SUCCESS;
        }
        return EditResult.FAILURE;
    }
}
