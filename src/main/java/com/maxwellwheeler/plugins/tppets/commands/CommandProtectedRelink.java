package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

/**
 * Class representing a /tpp protected relink subcommand.
 * @author GatheringExp
 */
public class CommandProtectedRelink extends Command {
    /**
     * Relays data to {@link Command} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp protected relink.
     */
    CommandProtectedRelink(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp protected relink [Protected Region Name] [Lost and Found Region Name]</li>
     * </ul>
     */
    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
        logStatus();
    }

    /**
     * Relinks an existing {@link ProtectedRegion} to a {@link com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion}.
     */
    private void processCommandGeneric() {
        try {
            if (!ArgValidator.validateArgsLength(this.args, 2)) {
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

            ProtectedRegion protectedRegion = this.thisPlugin.getProtectedRegionManager().getProtectedRegion(this.args[0]);

            if (protectedRegion == null) {
                this.commandStatus = CommandStatus.NO_REGION;
                return;
            }

            if (this.thisPlugin.getDatabase().relinkProtectedRegion(this.args[0], this.args[1])) {
                protectedRegion.setLfName(this.args[1]);
                protectedRegion.updateLFReference(this.thisPlugin);
            } else {
                this.commandStatus = CommandStatus.DB_FAIL;
            }

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages the command status to the {@link CommandProtectedRelink#sender}.
     */
    private void displayStatus() {
        switch(this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "You have relinked protected region " + ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " to lost and found region " + ChatColor.WHITE + this.args[1]);
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp protected relink [protected region name] [lost and found region name]");
                break;
            case INVALID_PR_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid protected region name: " + ChatColor.WHITE + this.args[0]);
                break;
            case INVALID_LR_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid lost and found region name: " + ChatColor.WHITE + this.args[1]);
                break;
            case NO_REGION:
                this.sender.sendMessage(ChatColor.RED + "Can't find protected region: " + ChatColor.WHITE + this.args[0]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not relink regions");
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
            logSuccessfulAction("protected relink", "relinked " + this.args[0] + " to " + this.args[1]);
        } else {
            logUnsuccessfulAction("protected relink", this.commandStatus.toString());
        }
    }
}
