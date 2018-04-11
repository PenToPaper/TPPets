package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Object that processes /tpp lost commands
 * @author GatheringExp
 *
 */
public class CommandLost extends RegionCommand {

    /**
     * Processes commands of the type /tpp lost [etc]
     * @param sender The {@link CommandSender} object that originally sent the command.
     * @param args The arguments passed with the command - doesn't include the "tpp lostandfound" in command.
     *             Ex: /tpp lostandfound add PrimaryLost, String args[] would have {add PrimaryLost}.
     */
    public void processCommand(CommandSender sender, String[] args) {
        if (ArgValidator.validateArgsLength(args, 1)) {
            // Changes behavior based on the 3rd index of the original commmand, but first index of the arguments passed to this method.
            switch (args[0].toLowerCase()) {
                case "add":
                    if (ArgValidator.validateArgsLength(args, 2)) {
                        addRegion(sender, new String[] {args[1]});
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp lostandfound add [name]");
                    }
                    break;
                case "remove":
                    if (ArgValidator.validateArgsLength(args, 2)) {
                        removeRegion(sender, new String[] {args[1]});
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp lostandfound remove [name]");
                    }
                    break;
                case "list":
                    if (ArgValidator.validateArgsLength(args, 2)) {
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

    /**
     * Syntax: /tpp lf add [name]
     * Adds a {@link LostAndFoundRegion} based on the current worldedit cuboid selection. This region can be linked with {@link com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion}s.
     * @param sender The {@link CommandSender} that ran the command.
     * @param truncatedArgs A truncated list of arguments passed to the /tpp lostandfound add [args] command. For command /tpp lostandfound add PrimaryLost, this includes only PrimaryLost
     */
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
                    thisPlugin.getLogWrapper().logSuccessfulAction("Player " + sender.getName() + " added lost and found region " + truncatedArgs[0]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unable to add lost and found region  " + ChatColor.WHITE + truncatedArgs[0]);
                }
                return;
            }
        }
        sender.sendMessage(ChatColor.RED + "Can't find WorldEdit selection.");
    }

    /**
     * Syntax: /tpp lf remove [name]
     * Removes the {@link LostAndFoundRegion} with name truncatedArgs[0].
     * {@link com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion}s that link to this will still be valid, and will return back to teleporting pets there if a lf region with this name is set with {@link #addRegion(CommandSender, String[]) addRegion} command.
     * @param sender The {@link CommandSender} that ran the command.
     * @param truncatedArgs A truncated list of arguments passed with the addRegion command. For command /tpp lostandfound remove PrimaryLost, this includes only PrimaryLost
     */
    @Override
    protected void removeRegion(CommandSender sender, String[] truncatedArgs) {
        // Gets a LostAndFoundRegion with name truncatedArgs[0]. Returns null if this can't be found.
        LostAndFoundRegion tempLfr = thisPlugin.getLostRegion(truncatedArgs[0]);
        if (tempLfr != null) {
            if (thisPlugin.getDatabase() != null && thisPlugin.getDatabase().deleteLostRegion(tempLfr)) {
                // Next two lines make sure protected regions that link to this lost and found region are taken care of.
                thisPlugin.removeLFReference(tempLfr.getZoneName());
                thisPlugin.removeLostRegion(tempLfr);
                sender.sendMessage(ChatColor.BLUE + "Lost and Found Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Removed!");
                thisPlugin.getLogWrapper().logSuccessfulAction("Player " + sender.getName() + " removed lost and found region " + truncatedArgs[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "Unable to remove lost and found region  " + ChatColor.WHITE + truncatedArgs[0]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Lost and Found Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.RED + " does not exist.");
        }
    }

    /**
     * Syntax: /tpp lf list
     * Lists all active {@link LostAndFoundRegion}s, along with their location and name.
     * Alternative Syntax: /tpp lf list [name]
     * Lists the {@link LostAndFoundRegion} with name [name].
     * @param sender The {@link CommandSender} that ran the command.
     * @param truncatedArgs A truncated list of arguments passed with the addRegion command. For command /tpp lostandfound list PrimaryLost, it would include PrimaryLost.
     */
    @Override
    protected void listRegions(CommandSender sender, String[] truncatedArgs) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Lost and Found Regions ]" + ChatColor.DARK_GRAY + "---------");
        if (ArgValidator.validateArgsLength(truncatedArgs, 1)) {
            // Syntax received: /tpp lf list [name]
            LostAndFoundRegion lfr = thisPlugin.getLostRegion(truncatedArgs[0]);
            if (lfr != null) {
                displayLfrInfo(sender, lfr);
            } else {
                sender.sendMessage(ChatColor.RED + "Could not find lost and found region with name " + ChatColor.WHITE + truncatedArgs[0]);
            }
        } else {
            // Syntax received: /tpp lf list
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
