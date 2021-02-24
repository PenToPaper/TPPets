package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.*;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * Object used for storage commands
 * @author GatheringExp
 */
// TODO: JAVADOC
public class CommandStorage extends BaseCommand {
    private boolean isDefaultCommand;

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
    }

    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.storageother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    private void determineIsDefaultCommand() {
        // TODO: Can change this not to static "default" in the future
        if (ArgValidator.validateArgsLength(this.args, 2) && this.args[1].equalsIgnoreCase("default")) {
            if (this.hasDefaultStoragePermissions()) {
                this.isDefaultCommand = true;
                return;
            } else {
                this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
            }
        }
        this.isDefaultCommand = false;
    }

    private boolean hasDefaultStoragePermissions() {
        return this.sender.hasPermission("tppets.setdefaultstorage");
    }

    public Command getCommandAdd() {
        if (this.isDefaultCommand) {
            return new CommandStorageAddDefault(this.thisPlugin, this.sender, this.sender, Arrays.copyOfRange(this.args, 1, this.args.length));
        } else {
            return new CommandStorageAdd(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
        }
    }

    public Command getCommandRemove() {
        if (this.isDefaultCommand) {
            return new CommandStorageRemoveDefault(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
        } else {
            return new CommandStorageRemove(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
        }
    }

    public Command getCommandList() {
        if (this.isDefaultCommand) {
            return new CommandStorageListDefault(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
        } else {
            return new CommandStorageList(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
        }
    }

    public void processCommandGeneric() {
        // Determine if the command is of type /tpp storage list default, add default, remove default
        determineIsDefaultCommand();
        if (this.commandStatus != CommandStatus.SUCCESS) {
            return;
        }

        Command commandToRun = null;

        switch(this.args[0].toLowerCase()) {
            case "add":
                commandToRun = getCommandAdd();
                break;
            case "remove":
                commandToRun = getCommandRemove();
                break;
            case "list":
                commandToRun = getCommandList();
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
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add/remove/list]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
