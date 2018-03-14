package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.helpers.CheckArgs;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;

/**
 * Object that processes /tpp lost commands
 * @author GatheringExp
 *
 */
public class CommandLost extends RegionCommand {

    /**
     * Processes the command passed to it
     * @param sender The CommandSender object that originally sent the command.
     * @param args The arguments passed with the command - doesn't include the "tpp lostandfound" in command. Ex: /tpp lostandfound add PrimaryLost, String args[] would have {add PrimaryLost}.
     */
    public void processCommand(CommandSender sender, String[] args) {
        if (CheckArgs.validateArgs(args, 1)) {
            // Changes behavior based on the 3rd index of the original commmand, but first index of the arguments passed to this method.
            switch (args[0]) {
                case "add":
                    if (CheckArgs.validateArgs(args, 2)) {
                        addRegion(sender, new String[] {args[1]});
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp lostandfound add [name]");
                    }
                    break;
                case "remove":
                    if (CheckArgs.validateArgs(args, 2)) {
                        removeRegion(sender, new String[] {args[1]});
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp lostandfound remove [name]");
                    }
                    break;
                case "list":
                    if (CheckArgs.validateArgs(args, 2)) {
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
    
    
    @Override
    protected void addRegion(CommandSender sender, String[] truncatedArgs) {
        if (sender instanceof Player) {
            Player pl = (Player) sender;
            Location[] lcs = getWePoints(pl);
            if (lcs != null) {
                // Creates new LostAndFound object, stores it in memory, and stores it in the database.
                LostAndFoundRegion lfr = new LostAndFoundRegion(truncatedArgs[0], lcs[0].getWorld().getName(), lcs[0].getWorld(), lcs[0], lcs[1]);
                if (thisPlugin.getDatabase() != null && thisPlugin.getDatabase().insertLostRegion(lfr)) {
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
    
    
    @Override
    protected void removeRegion(CommandSender sender, String[] truncatedArgs) {
        LostAndFoundRegion tempLfr = thisPlugin.getLostRegion(truncatedArgs[0]);
        if (tempLfr != null) {
            if (thisPlugin.getDatabase() != null && thisPlugin.getDatabase().deleteLostRegion(tempLfr)) {
                // Next two lines make sure protected regions that link to this lost and found region are taken care of.
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
    
    
    @Override
    protected void listRegions(CommandSender sender, String[] truncatedArgs) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[Lost and Found Regions]" + ChatColor.DARK_GRAY + "---------");
        if (CheckArgs.validateArgs(truncatedArgs, 1)) {
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
    
    /**
     * Displays {@link LostAndFoundRegion} data to specified {@link CommandSender}.
     * @param sender Player to send data to.
     * @param lfr {@link LostAndFoundRegion} to display the data of.
     */
    private void displayLfrInfo(CommandSender sender, LostAndFoundRegion lfr) {
        sender.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + lfr.getZoneName());
        sender.sendMessage(ChatColor.BLUE + "    " + "world: " + ChatColor.WHITE + lfr.getWorldName());
        sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 1: " + ChatColor.WHITE + getLocationString(lfr.getMinLoc()));
        sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 2: " + ChatColor.WHITE + getLocationString(lfr.getMaxLoc()));
    }
}
