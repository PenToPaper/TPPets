package com.maxwellwheeler.plugins.tppets.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.maxwellwheeler.plugins.tppets.storage.PetType;

public class CommandTPP implements CommandExecutor {
    private Hashtable<String, List<String>> commandAliases;
    
    public CommandTPP(Hashtable<String, List<String>> commandAliases) {
        this.commandAliases = commandAliases;
    }
    
    // Main command handler for the plugin, forwards commands to appropriate sub-commands
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0] != null) {
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
                        CommandTPPets dogTPP = new CommandTPPets();
                        dogTPP.processCommand(sender, PetType.Pets.DOG);
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "cats":
                    if (sender.hasPermission("tppets.cats")) {
                        CommandTPPets catTPP = new CommandTPPets();
                        catTPP.processCommand(sender, PetType.Pets.CAT);
                    } else {
                        permissionMessage(sender);
                    }
                    break;
                case "birds":
                    if (sender.hasPermission("tppets.birds")) {
                        CommandTPPets parrotTPP = new CommandTPPets();
                        parrotTPP.processCommand(sender, PetType.Pets.PARROT);
                        return true;
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
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[Commands]" + ChatColor.DARK_GRAY + "---------");
        sender.sendMessage(ChatColor.BLUE + "/tpp dogs     ->    Teleports your dogs to your location");
        sender.sendMessage(ChatColor.BLUE + "/tpp cats     ->    Teleports your cats to your location");
        sender.sendMessage(ChatColor.BLUE + "/tpp birds    ->    Teleports your birds to your location");
        sender.sendMessage(ChatColor.BLUE + "/tpp restrocted [add, remove, list, relink]    ->    Creates a region where pets will not be allowed");
        sender.sendMessage(ChatColor.BLUE + "/tpp lost [add, remove, list]    ->    Creates a region where lost pets will be teleported to");
        sender.sendMessage(ChatColor.DARK_GRAY + "---------------------------");
    }
    
    // Returns an adjusted array of args, with the input array being truncated to the input length
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
    
    private void permissionMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "You do not have permission to use that command.");
    }
}
