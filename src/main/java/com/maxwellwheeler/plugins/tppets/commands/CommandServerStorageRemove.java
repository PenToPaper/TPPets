package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

/**
 * Class representing a /tpp serverstorage remove subcommand.
 * @author GatheringExp
 */
public class CommandServerStorageRemove extends Command {
    /**
     * Relays data to {@link Command} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp serverstorage remove.
     */
    CommandServerStorageRemove(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp storage remove [Storage Name]</li>
     * </ul>
     */
    @Override
    public void processCommand() {
        removeServerStorage();
        displayStatus();
        logStatus();
    }

    /**
     * Removes an existing {@link com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation} from the server.
     */
    private void removeServerStorage() {
        try {
            if (!ArgValidator.validateArgsLength(this.args, 1)) {
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                return;
            }

            if (this.thisPlugin.getDatabase().getServerStorageLocation(this.args[0], this.sender.getWorld()) == null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

            if (!this.thisPlugin.getDatabase().removeServerStorageLocation(this.args[0], this.sender.getWorld())) {
                this.commandStatus = CommandStatus.DB_FAIL;
            }

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages the command status to the {@link CommandServerStorageRemove#sender}.
     */
    private void displayStatus() {
        // SUCCESS, DB_FAIL, ALREADY_DONE, SYNTAX_ERROR
        switch (this.commandStatus) {
            case INVALID_SENDER:
                break;
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "Server storage " + ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " in " + ChatColor.WHITE + this.sender.getWorld().getName() + ChatColor.BLUE + " has been removed");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not remove sever storage location" + ChatColor.WHITE + this.args[0]);
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.RED + "Server storage " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " in " + ChatColor.WHITE + this.sender.getWorld().getName() + ChatColor.RED + " already does not exist");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp serverstorage remove [storage name]");
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
            logSuccessfulAction("serverstorage remove", "removed " + this.args[0] + " from " + this.sender.getWorld().getName());
        } else {
            logUnsuccessfulAction("serverstorage remove", this.commandStatus.toString());
        }
    }
}
