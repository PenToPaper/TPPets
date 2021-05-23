package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;

/**
 * Class representing a /tpp rename command.
 * @author GatheringExp
 */
public class CommandRename extends BaseCommand {
    /**
     * Relays data to {@link BaseCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp rename.
     */
    public CommandRename(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * Calling this method indicates that all necessary data is in the instance and the command can be processed.
     */
    @Override
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
     *     <li>If the command is using f:[username] syntax, checks for tppets.renameother.</li>
     * </ul>
     * @return True if command has a target player with proper permissions and a proper number of arguments, false if not.
     */
    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.renameother", 2)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(2));
    }

    /**
     * Renames {@link CommandRename#commandFor}'s pet.
     */
    private void processCommandGeneric() {
        try {
            if (!ArgValidator.softValidatePetName(this.args[0])) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            if (!ArgValidator.softValidatePetName(this.args[1])) {
                this.commandStatus = CommandStatus.INVALID_NAME;
                return;
            }

            PetStorage pet = this.thisPlugin.getDatabase().getSpecificPet(this.commandFor.getUniqueId().toString(), this.args[0]);

            if (pet == null) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            if (this.thisPlugin.getDatabase().getSpecificPet(this.commandFor.getUniqueId().toString(), this.args[1]) != null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

            if (!this.thisPlugin.getDatabase().renamePet(this.commandFor.getUniqueId().toString(), this.args[0], this.args[1])) {
                this.commandStatus = CommandStatus.DB_FAIL;
            }

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages the command status to the {@link CommandRename#sender}.
     */
    private void displayStatus() {
        switch(this.commandStatus) {
            case INVALID_SENDER:
                break;
            case SUCCESS:
                this.sender.sendMessage((this.isForSelf() ? ChatColor.BLUE + "Your pet " : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.BLUE + " pet ") + ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " has been renamed to " + ChatColor.WHITE + this.args[1]);
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not rename pet");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find pet named " + ChatColor.WHITE + this.args[0]);
                break;
            case INVALID_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid pet name: " + ChatColor.WHITE + this.args[1]);
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.RED + "Pet name " + ChatColor.WHITE + this.args[1] + ChatColor.RED + " is already in use");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp rename [old name] [new name]");
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
            logSuccessfulAction("rename", "renamed " + this.commandFor.getName() + "'s " + this.args[0] + " to " + this.args[1]);
        } else {
            logUnsuccessfulAction("rename", this.commandStatus.toString());
        }
    }
}
