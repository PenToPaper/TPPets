package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

/**
 * Class representing a /tpp storage remove subcommand.
 * @author GatheringExp
 */
public class CommandStorageRemove extends Command {
    /**
     * Relays data to {@link Command} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp storage remove.
     */
    CommandStorageRemove(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * Calling this method indicates that all necessary data is in the instance and the command can be processed.
     */
    @Override
    public void processCommand() {
        removeStorage();
        displayStatus();
        logStatus();
    }

    /**
     * Removes an existing {@link com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation} from {@link CommandStorageRemove#commandFor}.
     */
    private void removeStorage() {
        try {

            if (!ArgValidator.validateArgsLength(this.args, 1)) {
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                return;
            }

            if (!ArgValidator.softValidateStorageName(this.args[0]) || this.thisPlugin.getDatabase().getStorageLocation(this.commandFor.getUniqueId().toString(), this.args[0]) == null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

            if (!this.thisPlugin.getDatabase().removeStorageLocation(this.commandFor.getUniqueId().toString(), this.args[0])) {
                this.commandStatus = CommandStatus.DB_FAIL;
            }

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages the command status to the {@link CommandStorageRemove#sender}.
     */
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

    /**
     * Logs any command status messages.
     */
    private void logStatus() {
        if (this.commandStatus == CommandStatus.SUCCESS) {
            logSuccessfulAction("storage remove", "removed " + this.args[0] + " from " + this.commandFor.getName());
        } else {
            logUnsuccessfulAction("storage remove", this.commandStatus.toString());
        }
    }
}
