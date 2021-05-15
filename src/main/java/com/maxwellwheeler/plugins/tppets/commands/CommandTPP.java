package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * Core command handler to the plugin.
 * @author GatheringExp
 *
 */
public class CommandTPP implements CommandExecutor {
    private final Hashtable<String, List<String>> commandAliases;
    private final TPPets thisPlugin;

    /**
     * Initializes the core command handler with the commandAliases provided.
     * @param commandAliases Hashtable linking &#60;Real command name, List&#60;Aliases of real command name&#62;&#62;
     * @param thisPlugin Reference to the TPPets plugin instance
     */
    public CommandTPP(Hashtable<String, List<String>> commandAliases, TPPets thisPlugin) {
        this.commandAliases = commandAliases;
        this.thisPlugin = thisPlugin;
    }

    /**
     * The core command handler for all /tpp [args] commands
     * @param sender The sender of the command
     * @param command The full commant string
     * @param label First word of the command
     * @param args Array of arguments for the command
     * @return True if command was valid, false if not
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (ArgValidator.validateArgsLength(args, 1)) {
            // Translates the command entered to any of the aliases specified in the config
            String realCommand = args[0].toLowerCase();
            for (String commands : this.commandAliases.keySet()) {
                if (this.commandAliases.get(commands).contains(args[0].toLowerCase())) {
                    realCommand = commands;
                    break;
                }
            }
            // Changes behavior based on the command type executed
            switch(realCommand) {
                case "tp":
                    // Permission check done within object, since the object needs to determine which pet type is being used
                    CommandTeleportPet commandTeleportPet = new CommandTeleportPet(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                    commandTeleportPet.processCommand();
                    break;
                case "list":
                    // Permission check done within object, since the object needs to determine which pet type is being used
                    CommandTeleportList commandTeleportList = new CommandTeleportList(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                    commandTeleportList.processCommand();
                    break;
                case "all":
                    // Permission check done within object, since the object needs to determine which pet type is being used
                    CommandTeleportAll commandTeleportAll = new CommandTeleportAll(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                    commandTeleportAll.processCommand();
                    break;
                case "protected":
                    if (sender.hasPermission("tppets.protected")) {
                        CommandProtected commandProtected = new CommandProtected(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        commandProtected.processCommand();                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "lost":
                    if (sender.hasPermission("tppets.lost")) {
                        CommandLost commandLost = new CommandLost(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        commandLost.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "rename":
                    if (sender.hasPermission("tppets.rename")) {
                        CommandRename renamePet = new CommandRename(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        renamePet.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "allow":
                    if (sender.hasPermission("tppets.allowguests")) {
                        CommandAllowAdd allowPlayer = new CommandAllowAdd(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        allowPlayer.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "remove":
                    // TODO: DOCUMENT THIS PERMISSION NAME CHANGE
                    if (sender.hasPermission("tppets.allowguests")) {
                        CommandAllowRemove removePlayer = new CommandAllowRemove(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        removePlayer.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "allowed":
                    if (sender.hasPermission("tppets.allowguests")) {
                        CommandAllowList listAllow = new CommandAllowList(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        listAllow.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "store":
                    if (sender.hasPermission("tppets.store")) {
                        CommandStore store = new CommandStore(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        store.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "storage":
                    if (sender.hasPermission("tppets.storage")) {
                        CommandStorage storage = new CommandStorage(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        storage.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "position1":
                    if (sender.hasPermission("tppets.protected") || sender.hasPermission("tppets.lost")) {
                        CommandPosition1 position1 = new CommandPosition1(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        position1.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "position2":
                    if (sender.hasPermission("tppets.protected") || sender.hasPermission("tppets.lost")) {
                        CommandPosition2 position2 = new CommandPosition2(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        position2.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "release":
                    if (sender.hasPermission("tppets.dogs") || sender.hasPermission("tppets.cats") || sender.hasPermission("tppets.parrots") || sender.hasPermission("tppets.horses") || sender.hasPermission("tppets.mules") || sender.hasPermission("tppets.llamas") || sender.hasPermission("tppets.donkeys")) {
                        CommandRelease commandRelease = new CommandRelease(this.thisPlugin, sender, Arrays.copyOfRange(args, 1, args.length));
                        commandRelease.processCommand();
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "help":
                default:
                    sendHelp(sender);
                    break;
            }
            return true;
        }
        return false;
    }
    
    /**
     * Sends help message to player
     * @param sender The player that the help message should be sent to
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------" + ChatColor.BLUE + "[ Commands ]" + ChatColor.DARK_GRAY + "--------------");
        if (sender.hasPermission("tppets.dogs") || sender.hasPermission("tppets.cats") || sender.hasPermission("tppets.parrots") || sender.hasPermission("tppets.horses") || sender.hasPermission("tppets.mules") || sender.hasPermission("tppets.llamas") || sender.hasPermission("tppets.donkeys")) {
            sender.sendMessage(ChatColor.WHITE + "/tpp tp [pet name]" + ChatColor.BLUE + "  ->  Teleports the pet with [pet name] to your location");
            sender.sendMessage(ChatColor.WHITE + "/tpp all [dogs/cats/etc]" + ChatColor.BLUE + "  ->  Teleports all [dogs/cats/etc] to your location");
            sender.sendMessage(ChatColor.WHITE + "/tpp list [dogs/cats/etc]" + ChatColor.BLUE + "  ->  Lists your owned [dogs/cats/etc]");
            sender.sendMessage(ChatColor.WHITE + "/tpp tp f:[username] [pet name]" + ChatColor.BLUE + "  ->  Teleports [username]'s pet named [pet name] to your location");
        }
        if (sender.hasPermission("tppets.allowguests")) {
            sender.sendMessage(ChatColor.WHITE + "/tpp allow [username] [pet name]" + ChatColor.BLUE + "  ->  Allows [username] to use teleport and mount your pet named [pet name]");
            sender.sendMessage(ChatColor.WHITE + "/tpp remove [username] [pet name]" + ChatColor.BLUE + "  ->  Disallows [username] to use teleport and mount your pet named [pet name]");
            sender.sendMessage(ChatColor.WHITE + "/tpp allowed [pet name]" + ChatColor.BLUE + "  ->  Lists all players who can teleport and mount pet named [pet name]");
        }
        if (sender.hasPermission("tppets.rename")) {
            sender.sendMessage(ChatColor.WHITE + "/tpp rename [old name] [new name]" + ChatColor.BLUE + "  ->  Renames [old name] to [new name].");
        }
        if (sender.hasPermission("tppets.storage")) {
            sender.sendMessage(ChatColor.WHITE + "/tpp storage [add, remove, list] [storage name]" + ChatColor.BLUE + "  ->  Adds a new storage location.");
        }
        if (sender.hasPermission("tppets.store")) {
            sender.sendMessage(ChatColor.WHITE + "/tpp store [pet name] [storage name]" + ChatColor.BLUE + "  ->  Sends [pet name] to [storage name]");
        }
        if (sender.hasPermission("tppets.protected")) {
            sender.sendMessage(ChatColor.WHITE + "/tpp protected [add, remove, list, relink]" + ChatColor.BLUE + "  ->  Creates a region where pets will not be allowed");
        }
        if (sender.hasPermission("tppets.lost")) {
            sender.sendMessage(ChatColor.WHITE + "/tpp lost [add, remove, list]" + ChatColor.BLUE + "  ->  Creates a region where lost pets will be teleported to");
        }
        if (sender.hasPermission("tppets.lost") || sender.hasPermission("tppets.protected")) {
            sender.sendMessage(ChatColor.WHITE + "/tpp position1" + ChatColor.BLUE + "  ->  Assigns your current location as the first position for region creation");
            sender.sendMessage(ChatColor.WHITE + "/tpp position2" + ChatColor.BLUE + "  ->  Assigns your current location as the second position for region creation");
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "-------------------------------------");
    }
    
    /**
     * Sends a permission denied message
     * @param sender Represents where to send the message.
     */
    private void permissionMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "You do not have permission to use that command.");
    }
}
