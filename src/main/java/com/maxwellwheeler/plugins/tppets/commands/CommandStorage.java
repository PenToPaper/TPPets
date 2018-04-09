package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class CommandStorage {
    private TPPets thisPlugin;

    /**
     * Generic constructor, needs to point to plugin for logging.
     * @param thisPlugin The {@link TPPets} instance.
     */
    public CommandStorage(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    // Desired Syntax: /tpp storage add [store name]
    // Desired Syntax: /tpp storage remove [store name]
    // Desired Syntax: /tpp storage list
    // Desired Syntax: /tpp storage f:[username] add [store name]
    // Desired Syntax: /tpp storage f:[username] remove [store name]
    // Desired Syntax: /tpp storage f:[username] list
    // Admin Syntax: /tpp storage list server
    // Store Name: \w{1,64}
    // Store Name: default
    @SuppressWarnings("deprecation")
    public void processCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && ArgValidator.validateArgsLength(args, 1)) {
            Player playerTemp = (Player) sender;
            String isForSomeoneElse = ArgValidator.isForSomeoneElse(args[0]);
            if (isForSomeoneElse != null && ArgValidator.validateUsername(isForSomeoneElse)) {
                OfflinePlayer commandFor = Bukkit.getOfflinePlayer(isForSomeoneElse);
                if (commandFor != null && commandFor.hasPlayedBefore() && ArgValidator.validateArgsLength(args, 2)) {
                    processCommandGeneric(playerTemp, commandFor, Arrays.copyOfRange(args, 1, args.length));
                }
            } else {
                processCommandGeneric(playerTemp, playerTemp, args);
            }
        }
    }

    public void processCommandGeneric(Player sender, OfflinePlayer commandFor, String[] args) {
        if (ArgValidator.validateArgsLength(args, 1)) {
            switch (args[0].toLowerCase()) {
                case "add":
                    if (!thisPlugin.canTpThere(sender)) {
                        return;
                    }
                    if (ArgValidator.validateArgsLength(args, 2)) {
                        if (ArgValidator.validateStorageName(args[1])) {
                            switch (addStorage(sender, commandFor, args[0])) {
                                case SUCCESS:
                                    sender.sendMessage((sender.equals(commandFor) ? ChatColor.BLUE + "You" : ChatColor.WHITE + commandFor.getName()) + " have added storage location " + ChatColor.WHITE + args[0]);
                                    break;
                                case LIMIT_REACHED:
                                    sender.sendMessage(ChatColor.RED + "You can't set any more storage locations!");
                                    break;
                                case ALREADY_DONE:
                                    if (sender.equals(commandFor)) {
                                        sender.sendMessage(ChatColor.BLUE + "You have already set a location named " + ChatColor.WHITE + args[0]);
                                    } else {
                                        sender.sendMessage(ChatColor.WHITE + commandFor.getName() + ChatColor.BLUE + " already has a location named " + ChatColor.WHITE + args[0]);
                                    }
                                    break;
                                case FAILURE:
                                default:
                                    sender.sendMessage(ChatColor.RED + "Unable to set location.");
                            }
                            return;
                        } else if (args[1].toLowerCase().equals("default") && sender.hasPermission("tppets.setdefaultstore")) {
                            switch (addServerStorage(sender, args[1])) {
                                case SUCCESS:
                                    sender.sendMessage((sender.equals(commandFor) ? ChatColor.BLUE + "You" : ChatColor.WHITE + commandFor.getName()) + " have added server storage location " + ChatColor.WHITE + args[0]);
                                    break;
                                case ALREADY_DONE:
                                    sender.sendMessage(ChatColor.BLUE + "Server already has a location named " + ChatColor.WHITE + args[0] + ChatColor.BLUE + " in this world");
                                    break;
                                case FAILURE:
                                default:
                                    sender.sendMessage(ChatColor.RED + "Unable to add location.");
                                    break;
                            }
                            return;
                        }
                    }
                    sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp store add [store name]");
                    break;
                case "remove":
                    if (ArgValidator.validateArgsLength(args, 2)) {
                        if (ArgValidator.validateStorageName(args[1])) {
                            switch (removeStorage(sender, commandFor, args[0])) {
                                case SUCCESS:
                                    sender.sendMessage((sender.equals(commandFor) ? ChatColor.BLUE + "You" : ChatColor.WHITE + commandFor.getName()) + " have removed storage location " + ChatColor.WHITE + args[0]);
                                    break;
                                case ALREADY_DONE:
                                    if (sender.equals(commandFor)) {
                                        sender.sendMessage(ChatColor.WHITE + args[0] + ChatColor.BLUE + " does not exist");
                                    } else {
                                        sender.sendMessage(ChatColor.WHITE + commandFor.getName() + ChatColor.BLUE + " does not have a location named " + ChatColor.WHITE + args[0]);
                                    }
                                    break;
                                case FAILURE:
                                default:
                                    sender.sendMessage(ChatColor.RED + "Unable to remove location.");
                            }
                            return;
                        } else if (args[1].toLowerCase().equals("default") && sender.hasPermission("tppets.setdefaultstore")) {
                            switch (removeServerStorage(sender, args[1])) {
                                case SUCCESS:
                                    sender.sendMessage((sender.equals(commandFor) ? ChatColor.BLUE + "You" : ChatColor.WHITE + commandFor.getName()) + " have removed server storage location " + ChatColor.WHITE + args[0]);
                                    break;
                                case ALREADY_DONE:
                                    sender.sendMessage(ChatColor.BLUE + "Server location " + ChatColor.WHITE + args[0] + ChatColor.BLUE + " does not exist");
                                    break;
                                case FAILURE:
                                default:
                                    sender.sendMessage(ChatColor.RED + "Unable to remove location.");
                            }
                            return;
                        }
                    }
                    sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp store remove [store name]");
                    break;
                case "list":
                    if (ArgValidator.validateArgsLength(args, 2) && args[1].equals("default")) {
                        listServerStorage(sender);
                    } else {
                        listStorage(sender, commandFor);
                    }
                    break;
            }
        }
    }

    /**
     * Enum representing results of a /tpp allow or /tpp remove, so that specific feedback can be given to the player.
     */
    private enum EditResult {SUCCESS, ALREADY_DONE, NO_PLAYER, LIMIT_REACHED, FAILURE}

    public EditResult addStorage(Player pl, OfflinePlayer commandFor, String storageName) {
        if (thisPlugin.getDatabase() != null) {
            if (thisPlugin.getDatabase().getStorageLocation(commandFor.getUniqueId().toString(), storageName) == null) {
                if (thisPlugin.getDatabase().getStorageLocations(commandFor.getUniqueId().toString()).size() < thisPlugin.getStorageLimit()) {
                    if (thisPlugin.getDatabase().addStorageLocation(commandFor.getUniqueId().toString(), storageName, pl.getLocation())) {
                        thisPlugin.getLogger().info("Player " + pl.getUniqueId().toString() + " has added location " + storageName + " " + TeleportCommand.formatLocation(pl.getLocation()) + " for " + commandFor.getName());
                        return EditResult.SUCCESS;
                    }
                    return thisPlugin.getDatabase().addStorageLocation(commandFor.getUniqueId().toString(), storageName, pl.getLocation()) ? EditResult.SUCCESS : EditResult.FAILURE;
                } else {
                    return EditResult.LIMIT_REACHED;
                }
            } else {
                return EditResult.ALREADY_DONE;
            }
        }
        return EditResult.FAILURE;
    }

    public EditResult removeStorage(Player pl, OfflinePlayer commandFor, String storageName) {
        if (thisPlugin.getDatabase() != null) {
            if (thisPlugin.getDatabase().getStorageLocation(commandFor.getUniqueId().toString(), storageName) != null) {
                if (thisPlugin.getDatabase().removeStorageLocation(commandFor.getUniqueId().toString(), storageName)) {
                    thisPlugin.getLogger().info("Player " + pl.getUniqueId().toString() + " has added location " + storageName + " " + TeleportCommand.formatLocation(pl.getLocation()) + " for " + commandFor.getName());
                    return EditResult.SUCCESS;
                }
            } else {
                return EditResult.ALREADY_DONE;
            }
        }
        return EditResult.FAILURE;
    }

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

    public EditResult addServerStorage(Player pl, String storageName) {
        if (thisPlugin.getDatabase() != null) {
            if (thisPlugin.getDatabase().getServerStorageLocation(storageName, pl.getWorld()) == null) {
                if (thisPlugin.getDatabase().addServerStorageLocation(storageName, pl.getLocation())) {
                    thisPlugin.getLogger().info("Player " + pl.getUniqueId().toString() + " has added server location " + storageName + " " + TeleportCommand.formatLocation(pl.getLocation()));
                    return EditResult.SUCCESS;
                }
            } else {
                return EditResult.ALREADY_DONE;
            }
        }
        return EditResult.FAILURE;
    }

    public EditResult removeServerStorage(Player pl, String storageName) {
        if (thisPlugin.getDatabase() != null) {
            if (thisPlugin.getDatabase().getServerStorageLocation(storageName, pl.getWorld()) == null) {
                if (thisPlugin.getDatabase().removeServerStorageLocation(storageName, pl.getWorld())) {
                    thisPlugin.getLogger().info("Player " + pl.getUniqueId().toString() + " has removed server location " + storageName + " " + TeleportCommand.formatLocation(pl.getLocation()));
                    return EditResult.SUCCESS;
                }
            } else {
                return EditResult.ALREADY_DONE;
            }
        }
        return EditResult.FAILURE;
    }

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

    private void listIndividualStorage (Player pl, StorageLocation storageLoc) {
        if (storageLoc != null) {
            pl.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + storageLoc.getStorageName());
            pl.sendMessage(ChatColor.BLUE + "    location: " + ChatColor.WHITE + Integer.toString(storageLoc.getLoc().getBlockX()) + ", " + Integer.toString(storageLoc.getLoc().getBlockY()) + ", " + Integer.toString(storageLoc.getLoc().getBlockZ()) + ", " + storageLoc.getLoc().getWorld().getName());
        }
    }
}
