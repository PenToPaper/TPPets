package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CommandLostRemove extends Command {
    CommandLostRemove(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
    }

    private void processCommandGeneric() {
        if (!ArgValidator.validateArgsLength(this.args, 1)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return;
        }

        if (!ArgValidator.softValidateRegionName(this.args[0])) {
            this.commandStatus = CommandStatus.INVALID_NAME;
            return;
        }

        try {
            LostAndFoundRegion lfr = this.thisPlugin.getDatabase().getLostRegion(this.args[0]);

            if (lfr == null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

        } catch (SQLException e) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;

        }

        if (this.thisPlugin.getDatabase().removeLostRegion(this.args[0])) {
            this.thisPlugin.removeLFReference(this.args[0]);
            this.thisPlugin.removeLostRegion(this.args[0]);
            this.thisPlugin.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " removed lost and found region " + this.args[0]);
        } else {
            this.commandStatus = CommandStatus.DB_FAIL;
        }

    }

    private void displayStatus() {
        switch(this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage("You have removed lost and found region " + ChatColor.WHITE + this.args[0]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not remove lost and found region");
                break;
            case INVALID_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid region name: " + ChatColor.WHITE + this.args[0]);
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.RED + "Region " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " already does not exist");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp lost remove [region name]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
