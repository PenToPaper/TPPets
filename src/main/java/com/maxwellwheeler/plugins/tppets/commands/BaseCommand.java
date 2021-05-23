package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * Class representing a base TPP command.
 * Ex: In /tpp lost add, lost is the base command.
 * @author GatheringExp
 */
public abstract class BaseCommand extends Command {
    /** True if command used f:[username] syntax, false if not. */
    protected boolean isIntendedForSomeoneElse;

    /**
     * Initializes instance variables and processes any f:[username] syntax used.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. This should be updated not to include the base command.
     */
    public BaseCommand(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
        initializeCommandFor();
    }

    /**
     * Determines if the command was sent by a player with sufficient permissions to send the command for others, and
     * has enough arguments to make a valid command. Updates {@link BaseCommand#commandStatus} with the reason the command
     * was invalid.
     * @param permission The expected tppets.[permission] string.
     * @param numArgs The expected number of arguments.
     * @return True if the command's format passes all checks, false if not.
     */
    protected boolean hasValidForOtherPlayerFormat(String permission, int numArgs) {
        // 1) Check that the command sender exists and is a player
        if (this.sender == null) {
            this.commandStatus = CommandStatus.INVALID_SENDER;
            return false;
        }

        // 2) Check that the player has permission to execute this type of command
        if (!this.sender.hasPermission(permission)) {
            this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
            return false;
        }

        // 3) Check if there's enough arguments to make a valid command
        if (!ArgValidator.validateArgsLength(this.args, numArgs)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return false;
        }

        // 4) All clear for now. Set commandStatus to SUCCESS to override any earlier calls to similar methods
        this.commandStatus = CommandStatus.SUCCESS;
        return true;
    }

    /**
     * Determines if the command was sent by a player, and has enough arguments to make a valid command. Updates
     * {@link BaseCommand#commandStatus} with the reason the command was invalid.
     * @param numArgs The expected number of arguments
     * @return True if the command's format passes all checks, false if not.
     */
    protected boolean hasValidForSelfFormat(int numArgs) {
        // 1) Check that the command sender exists and is a player
        if (this.sender == null) {
            this.commandStatus = CommandStatus.INVALID_SENDER;
            return false;
        }

        // 2) Check if there's enough arguments to make a valid command
        if (!ArgValidator.validateArgsLength(this.args, numArgs)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return false;
        }

        // 3) All clear for now. Set commandStatus to SUCCESS to override any earlier calls to similar methods
        this.commandStatus = CommandStatus.SUCCESS;
        return true;
    }

    /**
     * Initializes any f:[username] syntax used, populating {@link BaseCommand#commandFor} with the offline player the
     * command is for and {@link BaseCommand#isIntendedForSomeoneElse} with a boolean value representing whether or not
     * a f:[username] command was attempted by the sender. If the f:[username] player cannot be found, updates
     * {@link BaseCommand#commandStatus} to {@link CommandStatus#NO_PLAYER}
     */
    private void initializeCommandFor() {
        if (ArgValidator.validateArgsLength(this.args, 1)) {
            String isForSomeoneElse = ArgValidator.isForSomeoneElse(this.args[0]);
            if (isForSomeoneElse != null) {
                this.isIntendedForSomeoneElse = true;
                this.commandFor = getOfflinePlayer(isForSomeoneElse);
                if (this.commandFor != null) {
                    this.args = Arrays.copyOfRange(this.args, 1, this.args.length);
                } else {
                    this.commandStatus = CommandStatus.NO_PLAYER;
                }
                return;
            }
        }
        this.isIntendedForSomeoneElse = false;
        this.commandFor = this.sender;
    }
}
