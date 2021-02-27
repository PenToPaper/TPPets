package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;

public class CommandLostList extends Command {
    CommandLostList(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
    }

    private void processCommandGeneric() {
        Collection<LostAndFoundRegion> lfrs;
        if (ArgValidator.validateArgsLength(this.args, 1)) {
            // /tpp lf list [name]

            if (!ArgValidator.softValidateRegionName(this.args[0])) {
                this.commandStatus = CommandStatus.INVALID_NAME;
                return;
            }

            LostAndFoundRegion lfr = this.thisPlugin.getLostRegion(this.args[0]);

            if (lfr == null) {
                this.commandStatus = CommandStatus.NO_REGION;
                return;
            }

            lfrs = Collections.singletonList(lfr);

        } else {
            lfrs = this.thisPlugin.getLostRegions().values();
        }

        this.listAllRegions(lfrs);
    }

    private void listAllRegions(Collection<LostAndFoundRegion> lfrs) {
        this.sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ Lost and Found Regions ]" + ChatColor.DARK_GRAY + "---------");
        for (LostAndFoundRegion lfr : lfrs) {
            listLostAndFoundRegion(lfr);
        }
        this.sender.sendMessage(ChatColor.DARK_GRAY + "-----------------------------------------");
    }

    private String getLocationString(Location lc) {
        return lc.getBlockX() + ", " + lc.getBlockY() + ", " + lc.getBlockZ();
    }

    private void listLostAndFoundRegion(LostAndFoundRegion lfr) {
        this.sender.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + lfr.getRegionName());
        this.sender.sendMessage(ChatColor.BLUE + "    " + "world: " + ChatColor.WHITE + lfr.getWorldName());
        this.sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 1: " + ChatColor.WHITE + getLocationString(lfr.getMinLoc()));
        this.sender.sendMessage(ChatColor.BLUE + "    " + "endpoint 2: " + ChatColor.WHITE + getLocationString(lfr.getMaxLoc()));
    }

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
