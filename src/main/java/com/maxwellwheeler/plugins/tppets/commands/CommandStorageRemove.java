package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CommandStorageRemove extends Command {
    CommandStorageRemove(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        removeStorage();
        displayStatus();
    }

    private void removeStorage() {
        try {

            if (!ArgValidator.validateArgsLength(this.args, 1)) {
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                return;
            }

            if (!ArgValidator.validateStorageName(this.args[0]) || this.thisPlugin.getDatabase().getStorageLocation(commandFor.getUniqueId().toString(), this.args[0]) == null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

            if (!this.thisPlugin.getDatabase().removeStorageLocation(commandFor.getUniqueId().toString(), this.args[0])) {
                this.commandStatus = CommandStatus.DB_FAIL;
                return;
            }

            this.thisPlugin.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " has removed location " + this.args[0] + " from " + commandFor.getName());

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private void displayStatus() {
        // SUCCESS, SYNTAX_ERROR, DB_FAIL, ALREADY_DONE
        switch(this.commandStatus) {
            case INVALID_SENDER:
                break;
            case SUCCESS:
                this.sender.sendMessage((this.isForSelf() ? ChatColor.BLUE + "Storage" : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.BLUE) + " location " + ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " has been removed");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not remove storage location");
                break;
            case ALREADY_DONE:
                this.sender.sendMessage((this.isForSelf() ? ChatColor.RED + "Storage" : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.RED + " storage") + " location " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " already does not exist");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage remove [storage name]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
