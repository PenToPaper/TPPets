package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;

/**
 * Class representing a /tpp lost list command.
 * @author GatheringExp
 */
public class CommandLostList extends Command {
    /**
     * Relays data to {@link Command} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp lost list.
     */
    CommandLostList(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp lost list</li>
     *      <li>/tpp lost list [Lost and Found Region Name]</li>
     * </ul>
     */
    @Override
    public void processCommand() {
        processCommandGeneric();
        displayErrors();
    }

    /**
     * Lists all active {@link LostAndFoundRegion}s on the server.
     */
    private void processCommandGeneric() {
        Collection<LostAndFoundRegion> lfrs;
        if (ArgValidator.validateArgsLength(this.args, 1)) {
            // /tpp lf list [name]

            if (!ArgValidator.softValidateRegionName(this.args[0])) {
                this.commandStatus = CommandStatus.INVALID_NAME;
                return;
            }

            LostAndFoundRegion lfr = this.thisPlugin.getLostRegionManager().getLostRegion(this.args[0]);

            if (lfr == null) {
                this.commandStatus = CommandStatus.NO_REGION;
                return;
            }

            lfrs = Collections.singletonList(lfr);

        } else {
            lfrs = this.thisPlugin.getLostRegionManager().getLostRegions();
        }

        this.listAllRegions(lfrs);
    }

    /**
     * Lists all supplied {@link LostAndFoundRegion}s to the {@link CommandLostList#sender}.
     * @param lfrs A collection of {@link LostAndFoundRegion}s to list.
     */
    private void listAllRegions(Collection<LostAndFoundRegion> lfrs) {
        this.sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Lost and Found Regions ]" + ChatColor.DARK_GRAY + "---------");
        for (LostAndFoundRegion lfr : lfrs) {
            listLostAndFoundRegion(lfr);
        }
        // 44 chars in header - 2 characters (buffer, since header has small characters and footer shouldn't be larger than header
        this.sender.sendMessage(ChatColor.DARK_GRAY + StringUtils.repeat("-", 42));
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
     * Displays an individual {@link LostAndFoundRegion}'s data to the {@link CommandLostList#sender}.
     * @param lfr A single {@link LostAndFoundRegion} to display.
     */
    private void listLostAndFoundRegion(LostAndFoundRegion lfr) {
        this.sender.sendMessage(ChatColor.BLUE + "Name: " + ChatColor.WHITE + lfr.getRegionName());
        this.sender.sendMessage(ChatColor.BLUE + "    World: " + ChatColor.WHITE + lfr.getWorldName());
        this.sender.sendMessage(ChatColor.BLUE + "    Endpoint 1: " + ChatColor.WHITE + getLocationString(lfr.getMinLoc()));
        this.sender.sendMessage(ChatColor.BLUE + "    Endpoint 2: " + ChatColor.WHITE + getLocationString(lfr.getMaxLoc()));
    }

    /**
     * Messages any command status errors to the {@link CommandLostList#sender}.
     */
    private void displayErrors() {
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
