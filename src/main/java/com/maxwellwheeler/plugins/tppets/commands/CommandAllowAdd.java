package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;

/**
 * Class representing a /tpp allow command.
 * @author GatheringExp
 */
public class CommandAllowAdd extends BaseCommand {
    /**
     * Relays data to {@link BaseCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp allow.
     */
    public CommandAllowAdd(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * Calling this method indicates that all necessary data is in the instance and the command can be processed.
     */
    public void processCommand() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayStatus();
        logStatus();
    }

    /**
     * Performs basic checks to the command's syntax:
     * <ul>
     *     <li>Checks that the sender is a player</li>
     *     <li>Checks that the command has the minimum number of arguments (2)</li>
     *     <li>If the command is using f:[username] syntax, checks for tppets.allowother.</li>
     * </ul>
     * @return True if command has a target player with proper permissions and a proper number of arguments, false if not.
     */
    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.allowother", 2)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(2));
    }

    /**
     * Allows the player to {@link CommandAllowAdd#commandFor}'s pet.
     */
    private void processCommandGeneric() {
        try {
            OfflinePlayer playerToAllow = this.getOfflinePlayer(this.args[0]);

            if (playerToAllow == null) {
                this.commandStatus = CommandStatus.NO_TARGET_PLAYER;
                return;
            }

            if (!ArgValidator.softValidatePetName(this.args[1])) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            PetStorage pet = this.thisPlugin.getDatabase().getSpecificPet(this.commandFor.getUniqueId().toString(), this.args[1]);

            if (pet == null) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            if (this.thisPlugin.getGuestManager().isGuest(pet.petId, playerToAllow.getUniqueId().toString())) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

            if (this.thisPlugin.getDatabase().insertGuest(pet.petId, playerToAllow.getUniqueId().toString())) {
                this.thisPlugin.getGuestManager().addGuest(pet.petId, playerToAllow.getUniqueId().toString());
            } else {
                this.commandStatus = CommandStatus.DB_FAIL;
            }

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages the command status to the {@link CommandAllowAdd#sender}.
     */
    private void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, NO_TARGET_PLAYER, SYNTAX_ERROR, NO_PET, DB_FAIL, ALREADY_DONE
        switch (this.commandStatus) {
            case INVALID_SENDER:
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case NO_TARGET_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + this.args[0]);
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp allow [player name] [pet name]");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  this.args[1]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not allow user to pet");
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.WHITE + this.args[0] + ChatColor.RED + " is already allowed to " + (this.isForSelf() ? "" : ChatColor.WHITE + this.commandFor.getName() + "'s ") + ChatColor.WHITE + this.args[1]);
                break;
            case SUCCESS:
                this.sender.sendMessage(ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " is now allowed to " + (this.isForSelf() ? "" : ChatColor.WHITE + this.commandFor.getName() + "'s ") + ChatColor.WHITE + this.args[1]);
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
            logSuccessfulAction("allow", this.args[0] + " to " + this.commandFor.getName() + "'s " + this.args[1]);
        } else {
            logUnsuccessfulAction("allow", this.commandStatus.toString());
        }
    }
}