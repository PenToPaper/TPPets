package com.maxwellwheeler.plugins.tppets.commands;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;

public class CommandRestricted extends RegionCommand {
    public void processCommand(CommandSender sender, String[] args) {
        System.out.println("CommandRestricted Called!");
        for (String arg : args) {
            System.out.println(arg);
        }
        if (args.length >= 1 && args[0] != null) {
            switch (args[0]) {
                case "add":
                    if (args.length >= 4 && args[1] != null && args[2] != null && args[3] != null) {
                        addRegion(sender, Arrays.copyOfRange(args, 1, args.length));
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp restricted add [name] [lost region] [enter message]");
                    }
                    break;
                case "remove":
                    if (args.length >= 2 && args[1] != null) {
                        removeRegion(sender, new String[] {args[1]});
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp restricted remove [name]");
                    }
                    break;
                case "list":
                    if (args.length >= 2 && args[1] != null) {
                        listRegions(sender, new String[] {args[1]});
                    } else {
                        listRegions(sender, new String[] {});
                    }
                    break;
                case "relink":
                    if (args.length >= 3 && args[1] != null && args[2] != null) {
                        relinkRegion(sender, Arrays.copyOfRange(args, 1, 3));
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp restricted relink [name] [lost region]");
                    }
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Syntax error: /tpp restricted [add/remove/list/relink]");
                }
        } else {
            sender.sendMessage(ChatColor.RED + "Syntax error: /tpp restricted [add/remove/list]");
        }
    }
    
    private void addRegion(CommandSender sender, String[] truncatedArgs) {
        if (sender instanceof Player) {
            Player pl = (Player) sender;
            Location[] lcs = getWePoints(pl);
            if (lcs != null && truncatedArgs.length == 3) {
                ProtectedRegion pr = new ProtectedRegion(truncatedArgs[0], truncatedArgs[2], pl.getWorld().getName(), lcs[0], lcs[1], truncatedArgs[1]);
                if (thisPlugin.getSQLite().insertRestrictedRegion(pr)) {
                    thisPlugin.addProtectedRegion(pr);
                    sender.sendMessage(ChatColor.BLUE + "Restricted Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Set!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Unable to set restricted region " + ChatColor.WHITE + truncatedArgs[0]);
                }
                return;
            }
        }
        sender.sendMessage(ChatColor.RED + "Can't find WorldEdit selection.");
    }
    
    private void removeRegion(CommandSender sender, String[] truncatedArgs) {
        ProtectedRegion tempPr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
        if (tempPr != null) {
            if (thisPlugin.getSQLite().deleteRestrictedRegion(tempPr)) {
                thisPlugin.removeProtectedRegion(truncatedArgs[0]);
                sender.sendMessage(ChatColor.BLUE + "Restricted Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Removed!");
            } else {
                sender.sendMessage(ChatColor.RED + "Unable to remove restricted region " + ChatColor.WHITE + truncatedArgs[0]);
            }
        }
    }
    
    private void listRegions(CommandSender sender, String[] truncatedArgs) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[Restricted Regions]" + ChatColor.DARK_GRAY + "---------");
        if (truncatedArgs.length == 1 && truncatedArgs[0] != null) {
            ProtectedRegion pr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
            if (pr != null) {
                displayPrInfo(sender, pr);
            } else {
                sender.sendMessage(ChatColor.RED + "Could not find restricted region with name " + ChatColor.WHITE + truncatedArgs[0]);
            }
        } else {
            for (ProtectedRegion pr : thisPlugin.getProtectedRegions()) {
                displayPrInfo(sender, pr);
            }
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------");
    }
    
    private void relinkRegion(CommandSender sender, String[] truncatedArgs) {
        ProtectedRegion tempPr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
        if (thisPlugin.getSQLite().updateRestrictedRegion(tempPr.getZoneName(), tempPr.getLfReference().getZoneName())) {
            tempPr.setLfReference(truncatedArgs[1]);
            sender.sendMessage(ChatColor.BLUE + "Restricted Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Updated!");
        } else {
            sender.sendMessage(ChatColor.RED + "Unable to relink restricted region  " + ChatColor.WHITE + truncatedArgs[0]);
        }
    }
    
    private void displayPrInfo(CommandSender sender, ProtectedRegion pr) {
        String tempLfName = pr.getLfReference() == null ? "null" : pr.getLfReference().getZoneName();
        sender.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + pr.getZoneName());
        sender.sendMessage(ChatColor.BLUE + "    " + "enter message: " + ChatColor.WHITE + pr.getEnterMessage());
        sender.sendMessage(ChatColor.BLUE + "    " + "world: " + ChatColor.WHITE + pr.getWorldName());
        sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 1: " + ChatColor.WHITE + getLocationString(pr.getMinLoc()));
        sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 2: " + ChatColor.WHITE + getLocationString(pr.getMaxLoc()));
        sender.sendMessage(ChatColor.BLUE + "    " + "lost region: " + ChatColor.WHITE + tempLfName);
    }
}
