package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Object that processes /tpp protected commands.
 * @author GatheringExp
 *
 */
public class CommandProtected extends RegionCommand {
    
    /**
     * Processes commands of the type /tpp pr [etc]
     * @param sender The {@link CommandSender} object that originally sent the command.
     * @param args The arguments passed with the command - doesn't include the "tpp protected" in command.
     *             Ex: /tpp protected add PrimaryProtected PrimaryLost You can't do that here, String args[] would have {add PrimaryProtected PrimaryLost You can't do that here}.
     */
    public void processCommand(CommandSender sender, String[] args) {
        if (ArgValidator.validateArgsLength(args, 1)) {
            // Changes behavior based on the 3rd index of the original command, but first index of the arguments provided here.
            switch (args[0].toLowerCase()) {
                case "add":
                    if (ArgValidator.validateArgsLength(args, 4)) {
                        addRegion(sender, Arrays.copyOfRange(args, 1, args.length));
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp protected add [name] [lost region] [enter message]");
                    }
                    break;
                case "remove":
                    if (ArgValidator.validateArgsLength(args, 2)) {
                        removeRegion(sender, new String[] {args[1]});
                    } else {
                        sender.sendMessage(ChatColor.RED + "Syntax error: /tpp protected remove [name]");
                    }
                    break;
                case "list":
                    if (ArgValidator.validateArgsLength(args, 2)) {
                        listRegions(sender, new String[] {args[1]});
                    } else {
                        listRegions(sender, new String[] {});
                    }
                    break;
                case "relink":
                    if (ArgValidator.validateArgsLength(args, 3)) {
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

    /**
     * Syntax: /tpp pr add [name] [{@link LostAndFoundRegion} name] [Enter message]
     * Enter message: The message displayed if a player is denied permission to /tpp [pet type] in the ProtectedRegion
     * {@link LostAndFoundRegion} name: The name of the {@link LostAndFoundRegion} linked to this ProtectedRegion. This can be undefined, but the ProtectedRegion will not teleport pets out of it
     * Adds a new {@link ProtectedRegion} to memory and database
     * @param sender The {@link CommandSender} that ran the command.
     * @param truncatedArgs A truncated list of arguments passed to the /tpp pr add [args] command. For command /tpp pr add PrimaryProtected, this includes only PrimaryProtected
     */
    @Override
    protected void addRegion(CommandSender sender, String[] truncatedArgs) {
        if (sender instanceof Player) {
            Player pl = (Player) sender;
            Location[] lcs = getWePoints(pl);
            if (lcs != null) {
                // Creates a new ProtectedRegion object, stores it in memory and in the database
                ProtectedRegion pr = new ProtectedRegion(truncatedArgs[0], truncatedArgs[2], lcs[0].getWorld().getName(), lcs[0].getWorld(), lcs[0], lcs[1], truncatedArgs[1]);
                if (thisPlugin.getDatabase() != null && thisPlugin.getDatabase().insertProtectedRegion(pr)) {
                    thisPlugin.addProtectedRegion(pr);
                    sender.sendMessage(ChatColor.BLUE + "Protected Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Set!");
                    if (pr.getLfReference() == null) {
                        sender.sendMessage(ChatColor.BLUE + "Warning: Lost and found region " + ChatColor.WHITE + truncatedArgs[1] + ChatColor.BLUE + " does not exist.");
                    }
                    thisPlugin.getLogWrapper().logSuccessfulAction("Player " + sender.getName() + " added protected region " + truncatedArgs[0]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unable to set protected region " + ChatColor.WHITE + truncatedArgs[0]);
                }
                return;
            }
        }
        sender.sendMessage(ChatColor.RED + "Can't find WorldEdit selection.");
    }

    /**
     * Syntax: /tpp pr remove [name]
     * Removes the pr from memory and database
     * @param sender The {@link CommandSender} that ran the command.
     * @param truncatedArgs A truncated list of arguments passed to the /tpp pr remove [args] command. For command /tpp pr remove PrimaryProtected, this includes only PrimaryProtected
     */
    @Override
    protected void removeRegion(CommandSender sender, String[] truncatedArgs) {
        ProtectedRegion tempPr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
        // If ProtectedRegion with that name exists
        if (tempPr != null) {
            // Remove it from memory and database
            if (thisPlugin.getDatabase() != null && thisPlugin.getDatabase().deleteProtectedRegion(tempPr)) {
                thisPlugin.removeProtectedRegion(truncatedArgs[0]);
                sender.sendMessage(ChatColor.BLUE + "Protected Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Removed!");
                thisPlugin.getLogWrapper().logSuccessfulAction("Player " + sender.getName() + " removed protected region " + truncatedArgs[0]);
            } else {
                sender.sendMessage(ChatColor.RED + "Unable to remove protected region " + ChatColor.WHITE + truncatedArgs[0]);
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Protected Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.RED + " does not exist.");
        }
    }

    /**
     * Syntax: /tpp pr list
     * Lists all active protected regions
     * Alternative Syntax: /tpp pr list [name]
     * Lists data for protected region with [name]
     * @param sender The {@link CommandSender} that ran the command.
     * @param truncatedArgs A truncated list of arguments passed with the addRegion command. For command /tpp pr list PrimaryProtected, it would include PrimaryProtected.
     */
    @Override
    protected void listRegions(CommandSender sender, String[] truncatedArgs) {
        sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Protected Regions ]" + ChatColor.DARK_GRAY + "---------");
        if (ArgValidator.validateArgsLength(truncatedArgs, 1)) {
            // Syntax received: /tpp pr list [name]
            ProtectedRegion pr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
            if (pr != null) {
                displayPrInfo(sender, pr);
            } else {
                sender.sendMessage(ChatColor.RED + "Could not find protected region with name " + ChatColor.WHITE + truncatedArgs[0]);
            }
        } else {
            // Syntax received: /tpp pr list
            for (String key : thisPlugin.getProtectedRegions().keySet()) {
                displayPrInfo(sender, thisPlugin.getProtectedRegion(key));
            }
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------");
    }
    
    /**
     * Relinks given {@link ProtectedRegion} to given {@link LostAndFoundRegion}
     * @param sender The {@link CommandSender} that ran the command to change this.
     * @param truncatedArgs A truncated list of arguments passed to the relink function. It really only includes [0] The {@link ProtectedRegion}'s name, and [1] The {@link LostAndFoundRegion}'s name
     *                      {@link LostAndFoundRegion} does not have to exist, although the {@link ProtectedRegion} will not teleport pets away
     */
    private void relinkRegion(CommandSender sender, String[] truncatedArgs) {
        ProtectedRegion tempPr = thisPlugin.getProtectedRegion(truncatedArgs[0]);
        // If ProtectedRegion with this name exists
        if (thisPlugin.getDatabase() != null && thisPlugin.getDatabase().updateProtectedRegion(tempPr)) {
            // Change it's LfName reference
            tempPr.setLfName(truncatedArgs[1]);
            // Update any object that might point to
            tempPr.updateLFReference();
            // Report back to the user
            sender.sendMessage(ChatColor.BLUE + "Protected Region " + ChatColor.WHITE + truncatedArgs[0] + ChatColor.BLUE + " Updated!");
            thisPlugin.getLogWrapper().logSuccessfulAction("Player " + sender.getName() + " relinked protected region " + truncatedArgs[0] + " to " + truncatedArgs[1]);
            if (tempPr.getLfReference() == null) {
                sender.sendMessage(ChatColor.BLUE + "Warning: Lost and found region " + ChatColor.WHITE + truncatedArgs[1] + ChatColor.BLUE + " does not exist.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Unable to relink protected region  " + ChatColor.WHITE + truncatedArgs[0]);
        }
    }
    
    /**
     * Displays {@link ProtectedRegion} data to specified {@link CommandSender}.
     * @param sender Player to send data to.
     * @param pr {@link ProtectedRegion} to display the data of.
     */
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
