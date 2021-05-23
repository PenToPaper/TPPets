package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.SelectionSession;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Class representing a /tpp protected add subcommand.
 * @author GatheringExp
 */
public class CommandProtectedAdd extends Command {
    /** The added protected region's enter message. Can be null. */
    private String enterMessage;

    /**
     * Relays data to {@link Command} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp protected add.
     */
    CommandProtectedAdd(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp protected add [Protected Region Name] [Lost and Found Region Name] [Enter Message]</li>
     * </ul>
     */
    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
        logStatus();
    }

    /**
     * Adds a new {@link ProtectedRegion} to the server.
     */
    private void processCommandGeneric() {
        try {
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

            this.enterMessage = String.join(" ", Arrays.copyOfRange(this.args, 2, this.args.length));

            if (!ArgValidator.softValidateRegionEnterMessage(this.enterMessage)) {
                this.commandStatus = CommandStatus.INVALID_MESSAGE;
                return;
            }

            SelectionSession selectionSession = this.thisPlugin.getRegionSelectionManager().getSelectionSession(this.sender);

            if (selectionSession == null || !selectionSession.isCompleteSelection()) {
                this.commandStatus = CommandStatus.NO_REGION;
                return;
            }

            ProtectedRegion pr = this.thisPlugin.getDatabase().getProtectedRegion(this.args[0]);

            if (pr != null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

            ProtectedRegion protectedRegion = new ProtectedRegion(this.args[0], this.enterMessage, selectionSession.getWorld().getName(), selectionSession.getWorld(), selectionSession.getMinimumLocation(), selectionSession.getMaximumLocation(), this.args[1], this.thisPlugin);
            if (this.thisPlugin.getDatabase().insertProtectedRegion(protectedRegion)) {
                this.thisPlugin.getProtectedRegionManager().addProtectedRegion(protectedRegion);
            } else {
                this.commandStatus = CommandStatus.DB_FAIL;
            }

        } catch (SQLException e) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages the command status to the {@link CommandProtectedAdd#sender}.
     */
    private void displayStatus() {
        switch(this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "You have added protected region " + ChatColor.WHITE + this.args[0]);
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
                this.sender.sendMessage(ChatColor.RED + "Invalid enter message: " + ChatColor.WHITE + this.enterMessage);
                break;
            case NO_REGION:
                this.sender.sendMessage(ChatColor.RED + "Can't add region without a region selection");
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

    /**
     * Logs any command status messages.
     */
    private void logStatus() {
        if (this.commandStatus == CommandStatus.SUCCESS) {
            logSuccessfulAction("protected add", "added " + this.args[0]);
        } else {
            logUnsuccessfulAction("protected add", this.commandStatus.toString());
        }
    }
}
