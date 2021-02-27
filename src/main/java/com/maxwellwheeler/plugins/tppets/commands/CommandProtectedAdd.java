package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.SelectionSession;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CommandProtectedAdd extends Command {
    CommandProtectedAdd(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
    }

    private void processCommandGeneric() {
        if (!ArgValidator.validateArgsLength(this.args, 3)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return;
        }

        if (!ArgValidator.softValidateRegionName(this.args[0])) {
            this.commandStatus = CommandStatus.INVALID_PR_NAME;
            return;
        }

        if (!ArgValidator.softValidateRegionName(this.args[1])) {
            this.commandStatus = CommandStatus.INVALID_LR_NAME;
            return;
        }

        if(!ArgValidator.softValidateRegionEnterMessage(this.args[2])) {
            this.commandStatus = CommandStatus.INVALID_MESSAGE;
            return;
        }

        SelectionSession selectionSession = this.thisPlugin.getRegionSelectionManager().getSelectionSession(this.sender);

        if (selectionSession == null || !selectionSession.isCompleteSelection()) {
            this.commandStatus = CommandStatus.NO_REGION;
            return;
        }

        try {
            ProtectedRegion pr = this.thisPlugin.getDatabase().getProtectedRegion(this.args[0]);

            if (pr != null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

        } catch (SQLException e) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;

        }

        try {
            ProtectedRegion protectedRegion = new ProtectedRegion(this.args[0], this.args[2], selectionSession.getWorld().getName(), selectionSession.getWorld(), selectionSession.getMinimumLocation(), selectionSession.getMaximumLocation(), this.args[1]);
            if (this.thisPlugin.getDatabase().insertProtectedRegion(protectedRegion)) {
                this.thisPlugin.addProtectedRegion(protectedRegion);
                this.thisPlugin.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " added protected region " + protectedRegion.getLfName());
            } else {
                this.commandStatus = CommandStatus.DB_FAIL;
            }
        } catch (NullPointerException e) {
            this.commandStatus = CommandStatus.UNEXPECTED_ERROR;
        }

    }

    private void displayStatus() {
        switch(this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage("You have added protected region " + ChatColor.WHITE + this.args[0]);
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp protected add [region name] [lost and found region] [enter message]");
                break;
            case INVALID_PR_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid protected region name: " + ChatColor.WHITE + this.args[0]);
                break;
            case INVALID_LR_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid lost and found region name: " + ChatColor.WHITE + this.args[1]);
                break;
            case INVALID_MESSAGE:
                this.sender.sendMessage(ChatColor.RED + "Invalid enter message: " + ChatColor.WHITE + this.args[2]);
                break;
            case NO_REGION:
                this.sender.sendMessage(ChatColor.RED + "Can't add region without a square WorldEdit selection");
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.RED + "Region " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " already exists");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not add protected region");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
