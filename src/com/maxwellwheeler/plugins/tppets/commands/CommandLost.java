package com.maxwellwheeler.plugins.tppets.commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.region.LostAndFoundRegion;

public class CommandLost extends RegionCommand {
    public void processCommand(CommandSender sender, String[] args) {
        if (args[0] != null) {
            switch (args[0]) {
                case "add":
                    if (args[1] != null) {
                        addRegion(sender, new String[] {args[1]});
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
            if (lcs != null && truncatedArgs.length == 1) {
                LostAndFoundRegion lfr = new LostAndFoundRegion(truncatedArgs[0], pl.getWorld().getName(), lcs[0], lcs[1]);
                thisPlugin.getSQLite().insertLostRegion(lfr);
                thisPlugin.addLostRegion(lfr);
                sender.sendMessage("Lost and Found Region " + truncatedArgs[0] + " Set!");
            }
        }
    }
    
    private void removeRegion(CommandSender sender, String[] truncatedArgs) {
        LostAndFoundRegion tempLfr = thisPlugin.getLostRegion(truncatedArgs[0]);
        if (tempLfr != null) {
            thisPlugin.removeLostRegion(tempLfr);
            thisPlugin.getSQLite().deleteLostRegion(tempLfr);
            sender.sendMessage("Lost and Found Region " + truncatedArgs[0] + " Removed!");
        }
    }
    
    private void listRegions(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[Lost and Found Regions]" + ChatColor.DARK_GRAY + "---------");
        for (String key : thisPlugin.getLostRegions().keySet()) {
            LostAndFoundRegion lfr = thisPlugin.getLostRegion(key);
            sender.sendMessage(ChatColor.BLUE + "name: " + lfr.getZoneName());
            sender.sendMessage(ChatColor.BLUE + "    " + "world: " + lfr.getWorldName());
            sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 1: " + getLocationString(lfr.getMinLoc()));
            sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 2: " + getLocationString(lfr.getMaxLoc()));
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "------------------------------------------");
    }
}
