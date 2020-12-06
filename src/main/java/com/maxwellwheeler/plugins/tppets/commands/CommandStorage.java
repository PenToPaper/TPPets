package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Object used for storage commands
 * @author GatheringExp
 */
public class CommandStorage {
    private TPPets thisPlugin;

    /**
     * Generic constructor, needs to point to plugin for logging.
     * @param thisPlugin The {@link TPPets} instance.
     */
    public CommandStorage(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Processes all /tpp storage commands. Separates the command into who ran the command and who the command is about, and passes this information to processCommandGeneric
     * @param sender The sender of the command
     * @param args The arguments passed to the command, excluding /tpp storage
     */
    // Desired Syntax: /tpp storage add [storage name]
    // Desired Syntax: /tpp storage remove [storage name]
    // Desired Syntax: /tpp storage list
    // Desired Syntax: /tpp storage f:[username] add [storage name]
    // Desired Syntax: /tpp storage f:[username] remove [storage name]
    // Desired Syntax: /tpp storage f:[username] list
    // Admin Syntax: /tpp storage list server
    // Storage Name: \w{1,64}
    // Storage Name: default
    @SuppressWarnings("deprecation")
    public void processCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && ArgValidator.validateArgsLength(args, 1)) {
            Player playerTemp = (Player) sender;
            String isForSomeoneElse = ArgValidator.isForSomeoneElse(args[0]);
            if (isForSomeoneElse != null) {
                if (ArgValidator.validateUsername(isForSomeoneElse)) {
                    if (sender.hasPermission("tppets.storageother")) {
                        OfflinePlayer commandFor = Bukkit.getOfflinePlayer(isForSomeoneElse);
                        if (commandFor != null && commandFor.hasPlayedBefore()) {
                            if (ArgValidator.validateArgsLength(args, 2)) {
                                processCommandGeneric(playerTemp, commandFor, Arrays.copyOfRange(args, 1, args.length));
                            } else {
                                sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add, remove, list] [storage name]");
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + isForSomeoneElse);
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + isForSomeoneElse);
                }
            } else {
                processCommandGeneric(playerTemp, playerTemp, args);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add, remove, list]");
        }
    }

    /**
     * The generic processor for /tpp storage [] commands, that takes into account who ran the command and whom the command is about. Analyzes arguments, passes to proper methods, and reports results back to user.
     * @param sender The command sender
     * @param commandFor Who the command is about
     * @param args The arguments passed to /tpp storage, not including /tpp storage
     */
    public void processCommandGeneric(Player sender, OfflinePlayer commandFor, String[] args) {
        if (ArgValidator.validateArgsLength(args, 1)) {
            switch (args[0].toLowerCase()) {
                case "add":
                    if (!thisPlugin.canTpThere(sender)) {
                        return;
                    }
                    if (ArgValidator.validateArgsLength(args, 2)) {
                        if (ArgValidator.validateStorageName(args[1])) {
                            switch (addStorage(sender, commandFor, args[1])) {
                                case SUCCESS:
                                    sender.sendMessage((sender.equals(commandFor) ? ChatColor.BLUE + "You have" : ChatColor.WHITE + commandFor.getName() + ChatColor.BLUE + " has") + " added storage location " + ChatColor.WHITE + args[1]);
                                    break;
                                case LIMIT_REACHED:
                                    sender.sendMessage(ChatColor.RED + "You can't set any more than " + ChatColor.WHITE + this.thisPlugin.getStorageLimit() + ChatColor.RED + " storage locations.");
                                    break;
                                case ALREADY_DONE:
                                    if (sender.equals(commandFor)) {
                                        sender.sendMessage(ChatColor.RED + "You have already set a location named " + ChatColor.WHITE + args[1]);
                                    } else {
                                        sender.sendMessage(ChatColor.WHITE + commandFor.getName() + ChatColor.RED + " already has a location named " + ChatColor.WHITE + args[1]);
                                    }
                                    break;
                                case FAILURE:
                                default:
                                    sender.sendMessage(ChatColor.RED + "Unable to set location.");
                            }
                            return;
                        } else if (args[1].toLowerCase().equals("default") && sender.hasPermission("tppets.setdefaultstorage")) {
                            switch (addServerStorage(sender, args[1])) {
                                case SUCCESS:
                                    sender.sendMessage((sender.equals(commandFor) ? ChatColor.BLUE + "You have" : ChatColor.WHITE + commandFor.getName() + ChatColor.BLUE + " has") + " added server storage location " + ChatColor.WHITE + args[1]);
                                    break;
                                case ALREADY_DONE:
                                    sender.sendMessage(ChatColor.BLUE + "Server already has a location named " + ChatColor.WHITE + args[1] + ChatColor.BLUE + " in this world");
                                    break;
                                case FAILURE:
                                default:
                                    sender.sendMessage(ChatColor.RED + "Unable to add location.");
                                    break;
                            }
                            return;
                        } else {
                            sender.sendMessage(ChatColor.RED + "Invalid storage name!");
                            return;
                        }
                    }
                    sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage add [storage name]");
                    break;
                case "remove":
                    if (ArgValidator.validateArgsLength(args, 2)) {
                        if (ArgValidator.validateStorageName(args[1])) {
                            switch (removeStorage(sender, commandFor, args[1])) {
                                case SUCCESS:
                                    sender.sendMessage((sender.equals(commandFor) ? ChatColor.BLUE + "You have" : ChatColor.WHITE + commandFor.getName() + ChatColor.BLUE + " has") + " removed storage location " + ChatColor.WHITE + args[1]);
                                    break;
                                case ALREADY_DONE:
                                    if (sender.equals(commandFor)) {
                                        sender.sendMessage(ChatColor.WHITE + args[1] + ChatColor.BLUE + " does not exist");
                                    } else {
                                        sender.sendMessage(ChatColor.WHITE + commandFor.getName() + ChatColor.BLUE + " does not have a storage location named " + ChatColor.WHITE + args[1]);
                                    }
                                    break;
                                case FAILURE:
                                default:
                                    sender.sendMessage(ChatColor.RED + "Unable to remove location.");
                            }
                            return;
                        } else if (args[1].toLowerCase().equals("default") && sender.hasPermission("tppets.setdefaultstorage")) {
                            switch (removeServerStorage(sender, args[1])) {
                                case SUCCESS:
                                    sender.sendMessage((sender.equals(commandFor) ? ChatColor.BLUE + "You have" : ChatColor.WHITE + commandFor.getName() + ChatColor.BLUE + " has") + " removed server storage location " + ChatColor.WHITE + args[1]);
                                    break;
                                case ALREADY_DONE:
                                    sender.sendMessage(ChatColor.BLUE + "Server location " + ChatColor.WHITE + args[1] + ChatColor.BLUE + " does not exist");
                                    break;
                                case FAILURE:
                                default:
                                    sender.sendMessage(ChatColor.RED + "Unable to remove location.");
                            }
                            return;
                        } else {
                            sender.sendMessage(ChatColor.RED + "Invalid storage name!");
                            return;
                        }
                    }
                    sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage remove [storage name]");
                    break;
                case "list":
                    if (ArgValidator.validateArgsLength(args, 2) && args[1].equals("default") && sender.hasPermission("tppets.setdefaultstorage")) {
                        listServerStorage(sender);
                    } else {
                        listStorage(sender, commandFor);
                    }
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add, remove, list]");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add, remove, list]");
        }
    }

    /**
     * Enum representing results of a /tpp allow or /tpp remove, so that specific feedback can be given to the player.
     */
    private enum EditResult {SUCCESS, ALREADY_DONE, NO_PLAYER, LIMIT_REACHED, FAILURE}

    /**
     * Generic processor for adding storage locations.
     * @param pl The player that ran the command
     * @param commandFor The player to whom the storage location will be added.
     * @param storageName The name of the storage location
     * @return Representation of the result of the edit
     */
    public EditResult addStorage(Player pl, OfflinePlayer commandFor, String storageName) {
        if (thisPlugin.getDatabase() != null) {
            if (thisPlugin.getDatabase().getStorageLocation(commandFor.getUniqueId().toString(), storageName) == null) {
                if (pl.hasPermission("tppets.bypassstoragelimit") || thisPlugin.getStorageLimit() < 0 || thisPlugin.getDatabase().getStorageLocations(commandFor.getUniqueId().toString()).size() < thisPlugin.getStorageLimit()) {
                    if (thisPlugin.getDatabase().addStorageLocation(commandFor.getUniqueId().toString(), storageName, pl.getLocation())) {
                        thisPlugin.getLogWrapper().logSuccessfulAction("Player " + pl.getUniqueId().toString() + " has added location " + storageName + " " + TeleportCommand.formatLocation(pl.getLocation()) + " for " + commandFor.getName());
                        return EditResult.SUCCESS;
                    } else {
                        return EditResult.FAILURE;
                    }
                } else {
                    return EditResult.LIMIT_REACHED;
                }
            } else {
                return EditResult.ALREADY_DONE;
            }
        }
        return EditResult.FAILURE;
    }

    /**
     * Generic processor for removing storage locations.
     * @param pl The player that ran the command
     * @param commandFor The player from whom the storage location will be removed.
     * @param storageName The name of the storage location
     * @return Representation of the result of the edit
     */
    public EditResult removeStorage(Player pl, OfflinePlayer commandFor, String storageName) {
        if (thisPlugin.getDatabase() != null) {
            if (thisPlugin.getDatabase().getStorageLocation(commandFor.getUniqueId().toString(), storageName) != null) {
                if (thisPlugin.getDatabase().removeStorageLocation(commandFor.getUniqueId().toString(), storageName)) {
                    thisPlugin.getLogWrapper().logSuccessfulAction("Player " + pl.getName() + " has removed location " + storageName + " from " + commandFor.getName());
                    return EditResult.SUCCESS;
                }
            } else {
                return EditResult.ALREADY_DONE;
            }
        }
        return EditResult.FAILURE;
    }

    /**
     * Lists the storage unit commandFor has to pl
     * @param pl The player that ran the command
     * @param commandFor The player whose storage location information is to be displayed
     */
    public void listStorage(Player pl, OfflinePlayer commandFor) {
        if (thisPlugin.getDatabase() != null) {
            pl.sendMessage(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + commandFor.getName() + "'s Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------");
            List<StorageLocation> foundStorage = thisPlugin.getDatabase().getStorageLocations(commandFor.getUniqueId().toString());
            if (foundStorage != null) {
                for (StorageLocation storageLoc : foundStorage) {
                    listIndividualStorage(pl, storageLoc);
                }
            }
            pl.sendMessage(ChatColor.GRAY + "----------------------------------------");
        }
    }

    /**
     * Generic processor for adding server storage locations.
     * @param pl The player that ran the command
     * @param storageName The name of the storage location
     * @return Representation of the result of the edit
     */
    public EditResult addServerStorage(Player pl, String storageName) {
        if (thisPlugin.getDatabase() != null) {
            if (thisPlugin.getDatabase().getServerStorageLocation(storageName, pl.getWorld()) == null) {
                if (thisPlugin.getDatabase().addServerStorageLocation(storageName, pl.getLocation())) {
                    thisPlugin.getLogWrapper().logSuccessfulAction("Player " + pl.getUniqueId().toString() + " has added server location " + storageName + " " + TeleportCommand.formatLocation(pl.getLocation()));
                    return EditResult.SUCCESS;
                }
            } else {
                return EditResult.ALREADY_DONE;
            }
        }
        return EditResult.FAILURE;
    }

    /**
     * Generic processor for removing server storage locations.
     * @param pl The player that ran the command
     * @param storageName The name of the storage location
     * @return Representation of the result of the edit
     */
    public EditResult removeServerStorage(Player pl, String storageName) {
        if (thisPlugin.getDatabase() != null) {
            if (thisPlugin.getDatabase().getServerStorageLocation(storageName, pl.getWorld()) != null) {
                if (thisPlugin.getDatabase().removeServerStorageLocation(storageName, pl.getWorld())) {
                    thisPlugin.getLogWrapper().logSuccessfulAction("Player " + pl.getUniqueId().toString() + " has removed server location " + storageName + " " + TeleportCommand.formatLocation(pl.getLocation()));
                    return EditResult.SUCCESS;
                }
            } else {
                return EditResult.ALREADY_DONE;
            }
        }
        return EditResult.FAILURE;
    }

    /**
     * Generic processor for listing server storage locations.
     * @param pl The player that ran the command
     */
    public void listServerStorage(Player pl) {
        if (thisPlugin.getDatabase() != null) {
            pl.sendMessage(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE +  "Server's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------");
            for (World world : Bukkit.getWorlds()) {
                pl.sendMessage(ChatColor.WHITE + world.getName());
                List<StorageLocation> foundStorage = thisPlugin.getDatabase().getServerStorageLocations(world);
                if (foundStorage != null) {
                    for (StorageLocation storageLoc : foundStorage) {
                        listIndividualStorage(pl, storageLoc);
                    }
                }
            }
            pl.sendMessage(ChatColor.GRAY + "----------------------------------------");
        }
    }

    /**
     * Displays data from an individual storage location to a player.
     * @param pl The player to display the data to.
     * @param storageLoc The storage location data to display.
     */
    private void listIndividualStorage (Player pl, StorageLocation storageLoc) {
        if (storageLoc != null) {
            pl.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + storageLoc.getStorageName());
            // TODO REFACTOR TeleportCommand.formatLocation and put it in here
            pl.sendMessage(ChatColor.BLUE + "    location: " + ChatColor.WHITE + Integer.toString(storageLoc.getLoc().getBlockX()) + ", " + Integer.toString(storageLoc.getLoc().getBlockY()) + ", " + Integer.toString(storageLoc.getLoc().getBlockZ()) + ", " + storageLoc.getLoc().getWorld().getName());
        }
    }
}
