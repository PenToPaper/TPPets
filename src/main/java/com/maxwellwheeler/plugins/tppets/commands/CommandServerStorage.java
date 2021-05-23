package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * Class representing a /tpp serverstorage command.
 * @author GatheringExp
 */
public class CommandServerStorage extends BaseCommand {
    /**
     * Relays data to {@link BaseCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp serverstorage.
     */
    public CommandServerStorage(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * Performs basic checks to the command's syntax:
     * <ul>
     *     <li>Checks that the sender is a player</li>
     *     <li>Checks that the command has the minimum number of arguments (1)</li>
     *     <li>If the command is using f:[username] syntax, checks for tppets.storageother.</li>
     * </ul>
     * @return True if command has a target player with proper permissions and a proper number of arguments, false if not.
     */
    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.storageother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    /**
     * Calling this method indicates that all necessary data is in the instance and the command can be processed.
     */
    @Override
    public void processCommand() {
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayErrors();
        logErrors();
    }

    /**
     * Runs the given /tpp serverstorage subcommand.
     */
    private void processCommandGeneric() {
        Command commandToRun = null;

        switch(this.args[0].toLowerCase()) {
            case "add":
                commandToRun = new CommandServerStorageAdd(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            case "remove":
                commandToRun = new CommandServerStorageRemove(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            case "list":
                commandToRun = new CommandServerStorageList(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            default:
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                break;
        }

        if (commandToRun != null) {
            commandToRun.processCommand();
        }
    }

    /**
     * Messages any command status errors to the {@link CommandServerStorage#sender}.
     */
    private void displayErrors() {
        switch(this.commandStatus) {
            case SUCCESS:
            case INVALID_SENDER:
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp serverstorage [add/remove/list]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }

    /**
     * Logs any command status errors.
     */
    private void logErrors() {
        if (this.commandStatus != CommandStatus.SUCCESS) {
            logUnsuccessfulAction("serverstorage", this.commandStatus.toString());
        }
    }
}
