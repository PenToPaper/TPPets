package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.SelectionSession;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CommandLostAdd extends Command {
    CommandLostAdd(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
    }

    private void processCommandGeneric() {
        try {
            if (!ArgValidator.validateArgsLength(this.args, 1)) {
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                return;
            }

            if (!ArgValidator.softValidateRegionName(this.args[0])) {
                this.commandStatus = CommandStatus.INVALID_NAME;
                return;
            }

            SelectionSession selectionSession = this.thisPlugin.getRegionSelectionManager().getSelectionSession(this.sender);

            if (selectionSession == null || !selectionSession.isCompleteSelection()) {
                this.commandStatus = CommandStatus.NO_REGION;
                return;
            }

            LostAndFoundRegion lfr = this.thisPlugin.getDatabase().getLostRegion(this.args[0]);

            if (lfr != null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

            LostAndFoundRegion lostAndFoundRegion = new LostAndFoundRegion(this.args[0], selectionSession.getWorld().getName(), selectionSession.getWorld(), selectionSession.getMinimumLocation(), selectionSession.getMaximumLocation());
            if (this.thisPlugin.getDatabase().insertLostRegion(lostAndFoundRegion)) {
                this.thisPlugin.addLostRegion(lostAndFoundRegion);
                this.thisPlugin.updateLFReference(lostAndFoundRegion.getRegionName());
                this.thisPlugin.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " added lost and found region " + lostAndFoundRegion.getRegionName());
            } else {
                this.commandStatus = CommandStatus.DB_FAIL;
            }
        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private void displayStatus() {
        switch(this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "You have added lost and found region " + ChatColor.WHITE + this.args[0]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not add lost and found region");
                break;
            case NO_REGION:
                this.sender.sendMessage(ChatColor.RED + "Can't add region without a region selection");
                break;
            case INVALID_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid region name: " + ChatColor.WHITE + this.args[0]);
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.RED + "Region " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " already exists");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp lost add [region name]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
