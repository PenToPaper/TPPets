package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;

/**
 * Class representing a /tpp protected list command.
 * @author GatheringExp
 */
public class CommandProtectedList extends Command {
    /**
     * Relays data to {@link Command} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp protected list.
     */
    CommandProtectedList(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * Calling this method indicates that all necessary data is in the instance and the command can be processed.
     */
    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
    }

    /**
     * Lists all active {@link ProtectedRegion}s on the server.
     */
    private void processCommandGeneric() {
        Collection<ProtectedRegion> prs;
        if (ArgValidator.validateArgsLength(this.args, 1)) {
            // /tpp lf list [name]

            if (!ArgValidator.softValidateRegionName(this.args[0])) {
                this.commandStatus = CommandStatus.INVALID_NAME;
                return;
            }

            ProtectedRegion pr = this.thisPlugin.getProtectedRegionManager().getProtectedRegion(this.args[0]);

            if (pr == null) {
                this.commandStatus = CommandStatus.NO_REGION;
                return;
            }

            prs = Collections.singletonList(pr);

        } else {
            prs = this.thisPlugin.getProtectedRegionManager().getProtectedRegions();
        }

        this.listAllRegions(prs);
    }

    /**
     * Lists all supplied {@link ProtectedRegion}s to the {@link CommandProtectedList#sender}.
     * @param prs A collection of {@link ProtectedRegion}s to list.
     */
    private void listAllRegions(Collection<ProtectedRegion> prs) {
        this.sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Protected Regions ]" + ChatColor.DARK_GRAY + "---------");
        for (ProtectedRegion pr : prs) {
            listProtectedRegion(pr);
        }
        // 39 chars in header - 2 characters (buffer, since header has small characters and footer shouldn't be larger than header
        this.sender.sendMessage(ChatColor.DARK_GRAY + StringUtils.repeat("-", 37));
    }

    /**
     * Gets a formatted string from a location.
     * @param lc The location to parse.
     * @return A formatted string with rounded block coordinates.
     */
    private String getLocationString(Location lc) {
        return lc.getBlockX() + ", " + lc.getBlockY() + ", " + lc.getBlockZ();
    }

    /**
     * Displays an individual {@link ProtectedRegion}'s data to the {@link CommandProtectedList#sender}.
     * @param pr A single {@link ProtectedRegion} to display.
     */
    private void listProtectedRegion(ProtectedRegion pr) {
        String tempLfName = pr.getLfName() + (pr.getLfReference() == null ? ChatColor.BLUE + " (Unset)" : "");
        this.sender.sendMessage(ChatColor.BLUE + "Name: " + ChatColor.WHITE + pr.getRegionName());
        this.sender.sendMessage(ChatColor.BLUE + "    Enter Message: " + ChatColor.WHITE + pr.getEnterMessage());
        this.sender.sendMessage(ChatColor.BLUE + "    World: " + ChatColor.WHITE + pr.getWorldName());
        this.sender.sendMessage(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + getLocationString(pr.getMinLoc()));
        this.sender.sendMessage(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + getLocationString(pr.getMaxLoc()));
        this.sender.sendMessage(ChatColor.BLUE + "    Lost Region: " + ChatColor.WHITE + tempLfName);
    }

    /**
     * Messages any command status errors to the {@link CommandProtectedList#sender}.
     */
    private void displayStatus() {
        switch(this.commandStatus) {
            case SUCCESS:
                break;
            case NO_REGION:
            case INVALID_NAME:
                this.sender.sendMessage(ChatColor.RED + "Can't find region named " + ChatColor.WHITE + this.args[0]);
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}