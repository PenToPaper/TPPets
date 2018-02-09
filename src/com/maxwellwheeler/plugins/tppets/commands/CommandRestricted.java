package com.maxwellwheeler.plugins.tppets.commands;

import java.util.Arrays;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;

public class CommandRestricted extends RegionCommand {
    public void processCommand(CommandSender sender, String[] args) {
        if (args[0] != null) {
            switch (args[0]) {
                case "add":
                    if (args[1] != null && args[2] != null && args[3] != null) {
                        addRegion(sender, Arrays.copyOfRange(args, 1, args.length));
                    }
                    break;
                case "remove":
                    if (args[1] != null) {
                        removeRegion(sender, new String[] {args[1]});
                    }
                    break;
                case "list":
                    listRegions(sender);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Syntax error: /tpp restricted [add/remove/list]");
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
                thisPlugin.addProtectedRegion(pr);
                thisPlugin.getSQLite().insertRestrictedRegion(pr);
                sender.sendMessage("Restricted Region " + truncatedArgs[0] + " Set!");
                return;
            }
        }
        sender.sendMessage(ChatColor.RED + "Can't find WorldEdit selection.");
    }
    
    private void removeRegion(CommandSender sender, String[] truncatedArgs) {
        ProtectedRegion tempPr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
        if (tempPr != null) {
            thisPlugin.removeProtectedRegion(truncatedArgs[0]);
            thisPlugin.getSQLite().deleteRestrictedRegion(tempPr);
            sender.sendMessage("Restricted Region " + truncatedArgs[0] + " Removed!");
        }
    }
    
    private void listRegions(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[Restricted Regions]" + ChatColor.DARK_GRAY + "---------");
        for (ProtectedRegion pr : thisPlugin.getProtectedRegions()) {
            sender.sendMessage(ChatColor.BLUE + "name: " + pr.getZoneName());
            sender.sendMessage(ChatColor.BLUE + "    " + "enter message: " + pr.getEnterMessage());
            sender.sendMessage(ChatColor.BLUE + "    " + "world: " + pr.getWorldName());
            sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 1: " + getLocationString(pr.getMinLoc()));
            sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 2: " + getLocationString(pr.getMaxLoc()));
            sender.sendMessage(ChatColor.BLUE + "    " + "lost region: " + pr.getLfReference().getZoneName());
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------");
    }
}
