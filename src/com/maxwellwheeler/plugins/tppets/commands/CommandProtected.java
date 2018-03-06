package com.maxwellwheeler.plugins.tppets.commands;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;

public class CommandProtected extends RegionCommand {
    public void processCommand(CommandSender sender, String[] args) {
        if (validateArgs(args, 1)) {
            switch (args[0]) {
                case "add":
                    if (validateArgs(args, 4)) {
                        addRegion(sender, Arrays.copyOfRange(args, 1, args.length));
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp protected add [name] [lost region] [enter message]");
                    }
                    break;
                case "remove":
                    if (validateArgs(args, 2)) {
                        removeRegion(sender, new String[] {args[1]});
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp protected remove [name]");
                    }
                    break;
                case "list":
                    if (validateArgs(args, 2)) {
                        listRegions(sender, new String[] {args[1]});
                    } else {
                        listRegions(sender, new String[] {});
                    }
                    break;
                case "relink":
                    if (validateArgs(args, 3)) {
                        relinkRegion(sender, Arrays.copyOfRange(args, 1, 3));
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp protected relink [name] [lost region]");
                    }
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Syntax error: /tpp protected [add/remove/list/relink]");
                }
        } else {
            sender.sendMessage(ChatColor.RED + "Syntax error: /tpp protected [add/remove/list/relink]");
        }
    }
    
    private void addRegion(CommandSender sender, String[] truncatedArgs) {
        if (sender instanceof Player) {
            Player pl = (Player) sender;
            Location[] lcs = getWePoints(pl);
            if (lcs != null) {
                ProtectedRegion pr = new ProtectedRegion(truncatedArgs[0], truncatedArgs[2], lcs[0].getWorld().getName(), lcs[0].getWorld(), lcs[0], lcs[1], truncatedArgs[1]);
                if (thisPlugin.getDatabase() != null && thisPlugin.getDatabase().insertProtectedRegion(pr)) {
                    thisPlugin.addProtectedRegion(pr);
                    sender.sendMessage(ChatColor.BLUE + "Protected Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Set!");
                    if (pr.getLfReference() == null) {
                        sender.sendMessage(ChatColor.BLUE + "Warning: Lost and found region " + ChatColor.WHITE + truncatedArgs[1] + ChatColor.BLUE + " does not exist.");
                    }
                    thisPlugin.getLogger().info("Player " + sender.getName() + " added protected region " + truncatedArgs[0]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unable to set protected region " + ChatColor.WHITE + truncatedArgs[0]);
                }
                return;
            }
        }
        sender.sendMessage(ChatColor.RED + "Can't find WorldEdit selection.");
    }
    
    private void removeRegion(CommandSender sender, String[] truncatedArgs) {
        ProtectedRegion tempPr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
        if (tempPr != null) {
            if (thisPlugin.getDatabase() != null && thisPlugin.getDatabase().deleteProtectedRegion(tempPr)) {
                thisPlugin.removeProtectedRegion(truncatedArgs[0]);
                sender.sendMessage(ChatColor.BLUE + "Protected Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Removed!");
                thisPlugin.getLogger().info("Player " + sender.getName() + " removed protected region " + truncatedArgs[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "Unable to remove protected region " + ChatColor.WHITE + truncatedArgs[0]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Protected Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.RED + " does not exist.");
        }
    }
    
    private void listRegions(CommandSender sender, String[] truncatedArgs) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[Protected Regions]" + ChatColor.DARK_GRAY + "---------");
        if (truncatedArgs.length >= 1 && truncatedArgs[0] != null) {
            ProtectedRegion pr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
            if (pr != null) {
                displayPrInfo(sender, pr);
            } else {
                sender.sendMessage(ChatColor.RED + "Could not find protected region with name " + ChatColor.WHITE + truncatedArgs[0]);
            }
        } else {
            for (String key : thisPlugin.getProtectedRegions().keySet()) {
                displayPrInfo(sender, thisPlugin.getProtectedRegion(key));
            }
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------");
    }
    
    private void relinkRegion(CommandSender sender, String[] truncatedArgs) {
        ProtectedRegion tempPr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
        if (thisPlugin.getDatabase() != null && thisPlugin.getDatabase().updateProtectedRegion(tempPr)) {
            tempPr.setLfName(truncatedArgs[1]);
            tempPr.updateLFReference();
            sender.sendMessage(ChatColor.BLUE + "Protected Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Updated!");
            thisPlugin.getLogger().info("Player " + sender.getName() + " relinked protected region " + truncatedArgs[0] + " to " + truncatedArgs[1]);
            if (tempPr.getLfReference() == null) {
                sender.sendMessage(ChatColor.BLUE + "Warning: Lost and found region " + ChatColor.WHITE + truncatedArgs[1] + ChatColor.BLUE + " does not exist.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Unable to relink protected region  " + ChatColor.WHITE + truncatedArgs[0]);
        }
    }
    
    private void displayPrInfo(CommandSender sender, ProtectedRegion pr) {
        String tempLfName = pr.getLfName() + (pr.getLfReference() == null ? " (Unset)" : ""); 
        sender.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + pr.getZoneName());
        sender.sendMessage(ChatColor.BLUE + "    " + "enter message: " + ChatColor.WHITE + pr.getEnterMessage());
        sender.sendMessage(ChatColor.BLUE + "    " + "world: " + ChatColor.WHITE + pr.getWorldName());
        sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 1: " + ChatColor.WHITE + getLocationString(pr.getMinLoc()));
        sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 2: " + ChatColor.WHITE + getLocationString(pr.getMaxLoc()));
        sender.sendMessage(ChatColor.BLUE + "    " + "lost region: " + ChatColor.WHITE + tempLfName);
    }
}
