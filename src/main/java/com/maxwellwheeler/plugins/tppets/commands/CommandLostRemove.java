package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

/**
 * Class representing a /tpp lost remove subcommand.
 * @author GatheringExp
 */
public class CommandLostRemove extends Command {
    /**
     * Relays data to {@link Command} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp lost remove.
     */
    CommandLostRemove(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp lost remove [Lost and Found Region Name]</li>
     * </ul>
     */
    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
        logStatus();
    }

    /**
     * Removes an existing {@link LostAndFoundRegion} from the server.
     */
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


            LostAndFoundRegion lfr = this.thisPlugin.getDatabase().getLostRegion(this.args[0]);

            if (lfr == null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

            if (this.thisPlugin.getDatabase().removeLostRegion(this.args[0])) {
                this.thisPlugin.getLostRegionManager().removeLostRegion(this.args[0]);
                this.thisPlugin.getProtectedRegionManager().updateLFReferences(this.args[0]);
            } else {
                this.commandStatus = CommandStatus.DB_FAIL;
            }
        } catch (SQLException e) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages the command status to the {@link CommandLostRemove#sender}.
     */
    private void displayStatus() {
        switch(this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "You have removed lost and found region " + ChatColor.WHITE + this.args[0]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not remove lost and found region");
                break;
            case INVALID_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid region name: " + ChatColor.WHITE + this.args[0]);
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.RED + "Lost and Found Region " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " already does not exist");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp lost remove [region name]");
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
            logSuccessfulAction("lost remove", "removed " + this.args[0]);
        } else {
            logUnsuccessfulAction("lost remove", this.commandStatus.toString());
        }
    }
}
