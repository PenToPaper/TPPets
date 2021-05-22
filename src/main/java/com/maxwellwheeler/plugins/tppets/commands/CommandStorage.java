package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * Object used for storage commands
 * @author GatheringExp
 */
// TODO: JAVADOC
public class CommandStorage extends BaseCommand {
    /**
     * Generic constructor, needs to point to plugin for logging.
     * @param thisPlugin The {@link TPPets} instance.
     */
    public CommandStorage(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    // Desired Syntax: /tpp storage add [storage name]
    // Desired Syntax: /tpp storage remove [storage name]
    // Desired Syntax: /tpp storage list
    // Desired Syntax: /tpp storage f:[username] add [storage name]
    // Desired Syntax: /tpp storage f:[username] remove [storage name]
    // Desired Syntax: /tpp storage f:[username] list
    // Admin Syntax: /tpp storage list server
    // Storage Name: \w{1,64}
    // Storage Name: default
    public void processCommand() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayErrors();
        logErrors();
    }

    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.storageother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    public void processCommandGeneric() {
        // Determine if the command is of type /tpp storage list default, add default, remove default
        Command commandToRun = null;

        switch(this.args[0].toLowerCase()) {
            case "add":
                commandToRun = new CommandStorageAdd(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            case "remove":
                commandToRun = new CommandStorageRemove(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            case "list":
                commandToRun = new CommandStorageList(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            case "server":
            case "serverlist":
            case "slist":
                commandToRun = new CommandWorldServerStorageList(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            default:
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                break;
        }

        if (commandToRun != null) {
            commandToRun.processCommand();
        }
    }

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
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add/remove/list/server]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }

    private void logErrors() {
        if (this.commandStatus != CommandStatus.SUCCESS) {
            logUnsuccessfulAction("storage", this.commandStatus.toString());
        }
    }
}
