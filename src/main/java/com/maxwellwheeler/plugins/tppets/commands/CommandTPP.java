package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * Core command handler to the plugin.
 * @author GatheringExp
 *
 */
public class CommandTPP implements CommandExecutor {
    private Hashtable<String, List<String>> commandAliases;
    private TPPets thisPlugin;
    
    /**
     * Initializes the core command handler with the commandAliases provided.
     * @param commandAliases Hashtable linking <Real command name, List<Aliases of real command name>>
     */
    public CommandTPP(Hashtable<String, List<String>> commandAliases, TPPets thisPlugin) {
        this.commandAliases = commandAliases;
        this.thisPlugin = thisPlugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (ArgValidator.validateArgs(args, 1)) {
            String realCommand = "";
            for (String commands : commandAliases.keySet()) {
                if (commandAliases.get(commands).contains(args[0])) {
                    realCommand = commands;
                    break;
                }
            }
            switch(realCommand) {
                case "protected":
                    if (sender.hasPermission("tppets.protected")) {
                        CommandProtected cr = new CommandProtected();
                        cr.processCommand(sender, separateArgs(Arrays.copyOfRange(args, 1, args.length), 4));
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "lost":
                    if (sender.hasPermission("tppets.lost")) {
                        CommandLost cl = new CommandLost();
                        cl.processCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "dogs":
                    if (sender.hasPermission("tppets.dogs")) {
                        CommandTPPets dogTPP = getTPPets(sender, args);
                        dogTPP.processCommand(sender, Arrays.copyOfRange(args, 1, args.length), PetType.Pets.DOG);
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "cats":
                    if (sender.hasPermission("tppets.cats")) {
                        CommandTPPets catTPP = getTPPets(sender, args);
                        catTPP.processCommand(sender, Arrays.copyOfRange(args, 1, args.length), PetType.Pets.CAT);
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "birds":
                    if (sender.hasPermission("tppets.birds")) {
                        CommandTPPets parrotTPP = getTPPets(sender, args);
                        parrotTPP.processCommand(sender, Arrays.copyOfRange(args, 1, args.length), PetType.Pets.PARROT);
                        return true;
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "horses":
                    if (sender.hasPermission("tppets.horses")) {
                        CommandTPPets horseTPP = getTPPets(sender, args);
                        horseTPP.processCommand(sender, Arrays.copyOfRange(args, 1, args.length), PetType.Pets.HORSE);
                        return true;
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "mules":
                    if (sender.hasPermission("tppets.mules")) {
                        CommandTPPets muleTPP = getTPPets(sender, args);
                        muleTPP.processCommand(sender, Arrays.copyOfRange(args, 1, args.length), PetType.Pets.MULE);
                        return true;
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "llamas":
                    if (sender.hasPermission("tppets.llamas")) {
                        CommandTPPets llamaTPP = getTPPets(sender, args);
                        llamaTPP.processCommand(sender, Arrays.copyOfRange(args, 1, args.length), PetType.Pets.LLAMA);
                        return true;
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "donkeys":
                    if (sender.hasPermission("tppets.donkeys")) {
                        CommandTPPets donkeyTPP = getTPPets(sender, args);
                        donkeyTPP.processCommand(sender, Arrays.copyOfRange(args, 1, args.length), PetType.Pets.DONKEY);
                        return true;
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "rename":
                    CommandRename renamePet = new CommandRename(thisPlugin);
                    renamePet.processCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                    return true;
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
     * Sends appropriate arguments to CommandTPPets constructor
     * @param args The arguments originally sent to the onCommand method.
     * @return An instance of {@link CommandTPPets}, constructed based on the number of args.
     */
    private CommandTPPets getTPPets(CommandSender sender, String[] args) {
        if (ArgValidator.validateArgs(args, 2) && sender.hasPermission("tppets.teleportother")) {
            return new CommandTPPets();
        }
        return new CommandTPPets();
    }
    
    /**
     * Sends help message to player
     * @param sender The player that the help message should be sent to
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[Commands]" + ChatColor.DARK_GRAY + "---------");
        sender.sendMessage(ChatColor.BLUE + "/tpp dogs     ->    Teleports your dogs to your location");
        sender.sendMessage(ChatColor.BLUE + "/tpp cats     ->    Teleports your cats to your location");
        sender.sendMessage(ChatColor.BLUE + "/tpp birds    ->    Teleports your birds to your location");
        sender.sendMessage(ChatColor.BLUE + "/tpp protected [add, remove, list, relink]    ->    Creates a region where pets will not be allowed");
        sender.sendMessage(ChatColor.BLUE + "/tpp lost [add, remove, list]    ->    Creates a region where lost pets will be teleported to");
        sender.sendMessage(ChatColor.DARK_GRAY + "---------------------------");
    }
    
    /**
     * Takes an inputArray of strings, and truncates it to length truncate, taking all leftover elements of the array and combining it into the last element of the truncated one. Ex: separateArgs(new String[]{"Hi", "Hello", " World"}, 2) = {"Hi", "Hello World"} 
     * @param inputArray The array to be concatenated
     * @param truncate The length of the final array
     * @return A truncated String[] array, with all elements after truncate combined into the last element.
     */
    private String[] separateArgs(String[] inputArray, int truncate) {
        if (inputArray.length < truncate) {
            return inputArray;
        }
        String[] truncatedArrayBits = Arrays.copyOfRange(inputArray, truncate-1, inputArray.length);
        List<String> retList = new ArrayList<String>(Arrays.asList(Arrays.copyOfRange(inputArray, 0, truncate-1)));
        String replacementTruncation = "";
        for (String str : truncatedArrayBits) {
            replacementTruncation = replacementTruncation + str + " ";
        }
        retList.add(replacementTruncation);
        return retList.toArray(new String[truncate]);
    }
    
    /**
     * Sends a permission denied message
     * @param sender Represents where to send the message.
     */
    private void permissionMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "You do not have permission to use that command.");
    }
}
