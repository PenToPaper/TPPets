package com.maxwellwheeler.plugins.tppets.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;

public class CommandLost extends RegionCommand {
    
    public void processCommand(CommandSender sender, String[] args) {
        if (validateArgs(args, 1)) {
            switch (args[0]) {
                case "add":
                    if (validateArgs(args, 2)) {
                        addRegion(sender, new String[] {args[1]});
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp lostandfound add [name]");
                    }
                    break;
                case "remove":
                    if (validateArgs(args, 2)) {
                        removeRegion(sender, new String[] {args[1]});
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp lostandfound remove [name]");
                    }
                    break;
                case "list":
                    if (validateArgs(args, 2)) {
                        listRegions(sender, new String[] {args[1]});
                    } else {
                        listRegions(sender, new String[] {});
                    }
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Syntax error: /tpp lostandfound [add/remove/list]");
                }
        } else {
            sender.sendMessage(ChatColor.RED + "Syntax error: /tpp lostandfound [add/remove/list]");
        }
    }
    
    private void addRegion(CommandSender sender, String[] truncatedArgs) {
        if (sender instanceof Player) {
            Player pl = (Player) sender;
            Location[] lcs = getWePoints(pl);
            if (lcs != null) {
                LostAndFoundRegion lfr = new LostAndFoundRegion(truncatedArgs[0], lcs[0].getWorld(), lcs[0], lcs[1]);
                if (thisPlugin.getSQLite().insertLostRegion(lfr)) {
                    thisPlugin.addLostRegion(lfr);
                    thisPlugin.updateLFReference(lfr.getZoneName());
                    sender.sendMessage(ChatColor.BLUE + "Lost and Found Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Set!");
                    thisPlugin.getLogger().info("Player " + sender.getName() + " added lost and found region " + truncatedArgs[0]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unable to add lost and found region  " + ChatColor.WHITE + truncatedArgs[0]);
                }
                return;
            }
        }
        sender.sendMessage(ChatColor.RED + "Can't find WorldEdit selection.");
    }
    
    private void removeRegion(CommandSender sender, String[] truncatedArgs) {
        LostAndFoundRegion tempLfr = thisPlugin.getLostRegion(truncatedArgs[0]);
        if (tempLfr != null) {
            if (thisPlugin.getSQLite().deleteLostRegion(tempLfr)) {
                thisPlugin.removeLFReference(tempLfr.getZoneName());
                thisPlugin.removeLostRegion(tempLfr);
                sender.sendMessage(ChatColor.BLUE + "Lost and Found Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Removed!");
                thisPlugin.getLogger().info("Player " + sender.getName() + " removed lost and found region " + truncatedArgs[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "Unable to remove lost and found region  " + ChatColor.WHITE + truncatedArgs[0]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Lost and Found Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.RED + " does not exist.");
        }
    }
    
    private void listRegions(CommandSender sender, String[] truncatedArgs) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[Lost and Found Regions]" + ChatColor.DARK_GRAY + "---------");
        if (validateArgs(truncatedArgs, 1)) {
            LostAndFoundRegion lfr = thisPlugin.getLostRegion(truncatedArgs[0]);
            if (lfr != null) {
                displayLfrInfo(sender, lfr);
            } else {
                sender.sendMessage(ChatColor.RED + "Could not find lost and found region with name " + ChatColor.WHITE + truncatedArgs[0]);
            }
        } else {
            for (String key : thisPlugin.getLostRegions().keySet()) {
                LostAndFoundRegion lfr = thisPlugin.getLostRegion(key);
                displayLfrInfo(sender, lfr);
            }
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------------------");
    }
    
    private void displayLfrInfo(CommandSender sender, LostAndFoundRegion lfr) {
        sender.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + lfr.getZoneName());
        sender.sendMessage(ChatColor.BLUE + "    " + "world: " + ChatColor.WHITE + lfr.getWorldName());
        sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 1: " + ChatColor.WHITE + getLocationString(lfr.getMinLoc()));
        sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 2: " + ChatColor.WHITE + getLocationString(lfr.getMaxLoc()));
    }
}
