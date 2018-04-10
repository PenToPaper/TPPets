package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Object that processes tpp pet permission commands:
 * /tpp allow [username] [pet name]
 * /tpp remove [username] [pet name]
 * /tpp list [pet name]
 * @author GatheringExp
 *
 */
class CommandPermissions {
    private TPPets thisPlugin;

    /**
     * Generic constructor, needs to point to plugin for logging.
     * @param thisPlugin The {@link TPPets} instance.
     */
    public CommandPermissions(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Enum representing results of a /tpp allow or /tpp remove, so that specific feedback can be given to the player.
     */
    private enum EditResult {SUCCESS, ALREADY_DONE, NO_PLAYER, FAILURE}

    /**
     * Syntax: /tpp allow [username] [pet name]
     * Allows the player with [username] to your pet with [pet name]
     * Alternative Syntax: /tpp allow from:[username 1] [username 2] [pet name]
     * Allows [username 2] to [username 1]'s pet named [pet name]
     * @param sender The {@link CommandSender} object that originally sent the command.
     * @param args The arguments passed with the command - doesn't include the "tpp allow" in command. Ex: /tpp allow GatheringExp MyPet, String args[] would have {GatheringExp, MyPet}.
     */
    @SuppressWarnings("deprecation")
    public void allowPlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player tempPlayer = (Player) sender;
            // Checking if syntax is /tpp allow f:[username 1] [username 2] [pet name]
            if (ArgValidator.validateArgsLength(args, 3) && ArgValidator.validateUsername(args[1]) && ArgValidator.softValidatePetName(args[2])) {
                String playerFor = ArgValidator.isForSomeoneElse(args[0]);
                // Resolving f:[username 1] argument
                if (playerFor != null && ArgValidator.validateUsername(playerFor)) {
                    // Checking permission tppets.allowother
                    if (!sender.hasPermission("tppets.allowother")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                        return;
                    }
                    // Syntax received: /tpp allow from:PlayerName [Allowed Player Name] [Pet Name]
                    OfflinePlayer from = Bukkit.getOfflinePlayer(playerFor);
                    if (from != null) {
                        // Checking if OfflinePlayer has played before. If they haven't, say that you can't find that player.
                        if (!from.hasPlayedBefore()) {
                            tempPlayer.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + playerFor);
                            return;
                        }
                        // Display appropriate results to player depending on the result of the command
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
                        tempPlayer.sendMessage(ChatColor.RED + "Could not find player named " + ChatColor.WHITE + args[0]);
                    }
                } else {
                    tempPlayer.sendMessage(ChatColor.RED + "Could not find player named " + ChatColor.WHITE + args[0]);
                }
            // Checking if syntax is /tpp allow [username] [pet name]
            } else if (ArgValidator.validateArgsLength(args, 2) && ArgValidator.validateUsername(args[0]) && ArgValidator.softValidatePetName(args[1])) {
                // Syntax received: /tpp allow [username] [pet name]
                // Display appropriate results to player depending on the result of the command
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
            // Sender is not a player
            sender.sendMessage(ChatColor.RED + "Error: Sender is not a player.");
        }
    }

    /**
     * Generic processor for /tpp allow [args] commands
     * @param from The player the command is to be processed from. Because {@link Player} extends {@link OfflinePlayer}, this can be the player that ran the command, or a player running the command for someone else
     * @param addedPlayerName The name to add to the above "from" object's pet.
     * @param petName The pet name to add addedPlayerName to.
     * @return Enum representing the success/failure of the command
     */
    @SuppressWarnings("deprecation")
    private EditResult addAllowedPlayer(OfflinePlayer from, String addedPlayerName, String petName) {
        OfflinePlayer addedPlayer = Bukkit.getOfflinePlayer(addedPlayerName);
        // First two if statements check if a player by that name has logged in before.
        if (addedPlayer != null) {
            if (!addedPlayer.hasPlayedBefore()) {
                return EditResult.NO_PLAYER;
            }
            String addedPlayerUUID = UUIDUtils.trimUUID(addedPlayer.getUniqueId());
            PetStorage petByName = thisPlugin.getDatabase().getPetByName(from.getUniqueId().toString(), petName);
            // If we can retrieve values for addedPlayerName's UUID and petName's UUID, process the command
            if (addedPlayerUUID != null && petByName != null) {
                // If no action is necessary, return EditResult.ALREADY_DONE
                if (thisPlugin.isAllowedToPet(petByName.petId, addedPlayerUUID)) {
                    return EditResult.ALREADY_DONE;
                }
                if (thisPlugin.getDatabase().insertAllowedPlayer(petByName.petId, addedPlayerUUID)) {
                    if (!thisPlugin.getAllowedPlayers().containsKey(petByName.petId)) {
                        thisPlugin.getAllowedPlayers().put(petByName.petId, new ArrayList<>());
                    }
                    thisPlugin.getAllowedPlayers().get(petByName.petId).add(addedPlayerUUID);
                    // Action was necessary, and successful
                    return EditResult.SUCCESS;
                }
            }
        }
        return EditResult.FAILURE;
    }

    /**
     * Syntax: /tpp remove [username] [pet name]
     * Removes the player with [username] from your pet with [pet name]
     * Alternative Syntax: /tpp remove from:[username 1] [username 2] [pet name]
     * Removes [username 2] from [username 1]'s pet named [pet name]
     * @param sender The {@link CommandSender} object that originally sent the command.
     * @param args The arguments passed with the command - doesn't include the "tpp remove" in command. Ex: /tpp remove GatheringExp MyPet, String args[] would have {GatheringExp, MyPet}.
     */
    @SuppressWarnings("deprecation")
    public void removePlayer(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player tempPlayer = (Player) sender;
            // Checking if syntax is /tpp remove f:[username 1] [username 2] [pet name]
            if (ArgValidator.validateArgsLength(args, 3) && ArgValidator.validateUsername(args[1]) && ArgValidator.softValidatePetName(args[2])) {
                String playerFor = ArgValidator.isForSomeoneElse(args[0]);
                if (playerFor != null && ArgValidator.validateUsername(playerFor)) {
                    // Checking permission tppets.allowother
                    if (!sender.hasPermission("tppets.allowother")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                        return;
                    }
                    // Syntax received: /tpp remove from:[username 1] [username 2] [pet name]
                    OfflinePlayer from = Bukkit.getOfflinePlayer(playerFor);
                    if (from != null) {
                        // Checking if from:[username 1] exists and has an accurate UUID.
                        if (!from.hasPlayedBefore()) {
                            tempPlayer.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + playerFor);
                            return;
                        }
                        // Display appropriate results to player depending on the result of the command
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
                    tempPlayer.sendMessage(ChatColor.RED + "Could not find player named " + ChatColor.WHITE + args[0]);
                }
            // Checking if syntax is /tpp remove [username 1] [pet name]
            } else if (ArgValidator.validateArgsLength(args, 2) && ArgValidator.validateUsername(args[0]) && ArgValidator.softValidatePetName(args[1])) {
                // Syntax received: /tpp remove [username 1] [pet name]
                // Display appropriate results to player depending on the result of the command
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
            // Sender is not a player
            sender.sendMessage(ChatColor.RED + "Error: Sender is not a player.");
        }
    }

    /**
     * Generic processor for /tpp remove [args] commands
     * @param from The player the command is to be processed from. Because {@link Player} extends {@link OfflinePlayer}, this can be the player that ran the command, or a player running the command for someone else
     * @param removedPlayerName The name to remove to the above "from" object's pet.
     * @param petName The pet name to add removedPlayerName to.
     * @return Enum representing the success/failure of the command
     */
    @SuppressWarnings("deprecation")
    private EditResult removeAllowedPlayer(OfflinePlayer from, String removedPlayerName, String petName) {
        // Looking for player with removedPlayerName
        OfflinePlayer removedPlayer = Bukkit.getOfflinePlayer(removedPlayerName);
        if (removedPlayer != null) {
            // Checking if removedPlayerName's OfflinePlayer object has a valid UUID
            if (!removedPlayer.hasPlayedBefore()) {
                return EditResult.NO_PLAYER;
            }
            String removedPlayerUUID = UUIDUtils.trimUUID(removedPlayer.getUniqueId());
            PetStorage petByName = thisPlugin.getDatabase().getPetByName(from.getUniqueId().toString(), petName);
            if (removedPlayerUUID != null && petByName != null) {
                // UUIDs have been found, attempting to do the thing
                if (!thisPlugin.isAllowedToPet(petByName.petId, removedPlayerUUID)) {
                    // Thing not necessary
                    return EditResult.ALREADY_DONE;
                }
                // Checking if thing has been done
                if (thisPlugin.getDatabase().deleteAllowedPlayer(petByName.petId, removedPlayerUUID)) {
                    if (!thisPlugin.getAllowedPlayers().containsKey(petByName.petId)) {
                        thisPlugin.getAllowedPlayers().put(petByName.petId, new ArrayList<>());
                    }
                    thisPlugin.getAllowedPlayers().get(petByName.petId).remove(removedPlayerUUID);
                    return EditResult.SUCCESS;
                }
            }
        }
        return EditResult.FAILURE;
    }

    /**
     * Syntax: /tpp list [pet name]
     * Lists all players allowed to sender's [pet name]
     * Alternative Syntax: /tpp list from:[username] [pet name]
     * Lists all players allowed to [username]'s [pet name]
     * @param sender The {@link CommandSender} object that originally sent the command.
     * @param args The arguments passed with the command - doesn't include the "tpp list" in command. Ex: /tpp list from:GatheringExp MyPet, String args[] would have {from:GatheringExp, MyPet}.
     */
    @SuppressWarnings("deprecation")
    public void listPlayers(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player playerTemp = (Player) sender;
            // Checking if syntax received was /tpp list from:[username] [pet name]
            if (ArgValidator.validateArgsLength(args, 2) && ArgValidator.softValidatePetName(args[1])) {
                String petOwnerName = ArgValidator.isForSomeoneElse(args[0]);
                // Checking if from:[username] exists and has a valid UUID
                if (petOwnerName != null && ArgValidator.validateUsername(petOwnerName)) {
                    if (!sender.hasPermission("tppets.allowother")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                        return;
                    }
                    OfflinePlayer petOwner = Bukkit.getOfflinePlayer(petOwnerName);
                    // Display appropriate information based on the result of the command
                    switch (listAllowedPlayers(playerTemp, petOwner, args[1])) {
                        case NO_PLAYER:
                            playerTemp.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + petOwnerName);
                            break;
                        case SUCCESS:
                            break;
                        default:
                            playerTemp.sendMessage(ChatColor.RED + "Can't list players allowed to pet.");
                            break;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Could not find player named " + ChatColor.WHITE + args[0]);
                }
            // Checking if syntax received was /tpp list [pet name]
            } else if (ArgValidator.validateArgsLength(args, 1) && ArgValidator.softValidatePetName(args[0])) {
                // Display appropriate information based on the result of the command
                if (!listAllowedPlayers(playerTemp, playerTemp, args[0]).equals(EditResult.SUCCESS)) {
                    playerTemp.sendMessage(ChatColor.RED + "Can't list players allowed to pet.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Syntax error! Usage: /tpp list [pet name]");
            }
        }
    }

    private EditResult listAllowedPlayers(Player reportTo, OfflinePlayer petOwner, String petName) {
        // Checking if petOwner exists and has a valid UUID
        if (!petOwner.hasPlayedBefore()) {
            return EditResult.NO_PLAYER;
        }
        PetStorage petByName = thisPlugin.getDatabase().getPetByName(petOwner.getUniqueId().toString(), petName);
        // Finding petName's UUID
        if (petByName != null) {
            List<String> playerUUIDs = thisPlugin.getDatabase().getAllowedPlayers(petByName.petId);
            reportTo.sendMessage(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE +  petOwner.getName() + "'s " + petName + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------");
            // For each UUID found that's allowed, find the corresponding player name and display it
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
