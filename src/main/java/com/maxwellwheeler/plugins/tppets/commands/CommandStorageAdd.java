package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CommandStorageAdd extends Command {
    CommandStorageAdd(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        addStorage();
        displayStatus();
        logStatus();
    }

    private void addStorage() {
        if (!this.thisPlugin.getProtectedRegionManager().canTpThere(this.sender, this.sender.getLocation())) {
            this.commandStatus = CommandStatus.CANT_TELEPORT_IN_PR;
            return;
        }

        if (!ArgValidator.validateArgsLength(this.args, 1)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return;
        }

        if (!ArgValidator.softValidateStorageName(this.args[0])) {
            this.commandStatus = CommandStatus.INVALID_NAME;
            return;
        }

        try {

            addStorageToDb();

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private void addStorageToDb() throws SQLException {
        if (this.thisPlugin.getDatabase().getStorageLocation(this.commandFor.getUniqueId().toString(), this.args[0]) != null) {
            this.commandStatus = CommandStatus.ALREADY_DONE;
            return;
        }

        if (!isNewStorageWithinLimit()) {
            this.commandStatus = CommandStatus.LIMIT_REACHED;
            return;
        }

        if (this.thisPlugin.getDatabase().insertStorageLocation(this.commandFor.getUniqueId().toString(), this.args[0], this.sender.getLocation())) {
            this.commandStatus = CommandStatus.SUCCESS;
        } else {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    public boolean isNewStorageWithinLimit() throws SQLException {
        return this.sender.hasPermission("tppets.bypassstoragelimit") || this.thisPlugin.getStorageLimit() < 0 || this.thisPlugin.getDatabase().getStorageLocations(this.commandFor.getUniqueId().toString()).size() < this.thisPlugin.getStorageLimit();
    }

    private void displayStatus() {
        // SUCCESS, DB_FAIL, LIMIT_REACHED, INVALID_NAME, ALREADY_DONE, SYNTAX_ERROR, CANT_TP_THERE
        switch(this.commandStatus) {
            case INVALID_SENDER:
            case CANT_TELEPORT_IN_PR:
                break;
            case SUCCESS:
                this.sender.sendMessage((this.isForSelf() ? ChatColor.BLUE + "You have" : ChatColor.WHITE + this.commandFor.getName() + ChatColor.BLUE + " has") + " added storage location " + ChatColor.WHITE + this.args[0]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not add storage location");
                break;
            case LIMIT_REACHED:
                this.sender.sendMessage((this.isForSelf() ? ChatColor.RED + "You" : ChatColor.WHITE + this.commandFor.getName() + ChatColor.RED) + " can't set any more than " + ChatColor.WHITE + this.thisPlugin.getStorageLimit() + ChatColor.RED + " storage locations");
                break;
            case INVALID_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid storage location name: " + ChatColor.WHITE + this.args[0]);
                break;
            case ALREADY_DONE:
                this.sender.sendMessage((this.isForSelf() ? ChatColor.RED + "Storage" : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.RED + " storage") + " location " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " already exists");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage add [storage name]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }

    private void logStatus() {
        if (this.commandStatus == CommandStatus.SUCCESS) {
            logSuccessfulAction("storage add", "added " + this.args[0] + " for " + this.commandFor.getName());
        } else {
            logUnsuccessfulAction("storage add", this.commandStatus.toString());
        }
    }
}
