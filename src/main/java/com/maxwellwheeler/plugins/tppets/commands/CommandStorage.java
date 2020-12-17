package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Object used for storage commands
 * @author GatheringExp
 */
// TODO: JAVADOC
public class CommandStorage extends BaseCommand {

    private CommandStatus commandStatus;
    private enum CommandStatus{SUCCESS, INVALID_SENDER, NO_PLAYER, SYNTAX_ERROR, INSUFFICIENT_PERMISSIONS}

    /**
     * Generic constructor, needs to point to plugin for logging.
     * @param thisPlugin The {@link TPPets} instance.
     */
    public CommandStorage(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
        this.commandStatus = CommandStatus.SUCCESS;
    }

    private boolean correctForOtherPlayerSyntax() {
        // 1) Check that the command sender exists and is a player
        if (this.sender == null) {
            this.commandStatus = CommandStatus.INVALID_SENDER;
            return false;
        }

        // 2) Check that the player has permission to execute this type of command
        if (!this.sender.hasPermission("tppets.storageother")) {
            this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
            return false;
        }

        // 3) Check if a player with that name was found
        if (this.commandFor == null) {
            this.commandStatus = CommandStatus.NO_PLAYER;
            return false;
        }

        // 4) Check if the player the command is for is equal to the sender
        if (this.commandFor.equals(this.sender)) {
            this.commandStatus = CommandStatus.NO_PLAYER;
            return false;
        }

        // 5) Check if there's enough arguments to make a valid command
        if (!ArgValidator.validateArgsLength(this.args, 1)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return false;
        }

        // 6) All clear for now. Set commandStatus to SUCCESS to override any earlier calls to similar methods
        this.commandStatus = CommandStatus.SUCCESS;
        return true;
    }

    private boolean correctForSelfSyntax() {
        // 1) Check that the command sender exists and is a player
        if (this.sender == null) {
            this.commandStatus = CommandStatus.INVALID_SENDER;
            return false;
        }

        // 2) Check if there's enough arguments to make a valid command
        if (!ArgValidator.validateArgsLength(args, 1)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return false;
        }

        // 3) All clear for now. Set commandStatus to SUCCESS to override any earlier calls to similar methods
        this.commandStatus = CommandStatus.SUCCESS;
        return true;
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
        if ((this.isIntendedForSomeoneElse && correctForOtherPlayerSyntax()) || (!this.isIntendedForSomeoneElse && correctForSelfSyntax())) {
            processCommandGeneric();
        }

        displayErrors();

    }

    private boolean isDefaultCommand() {
        if (ArgValidator.validateArgsLength(this.args, 2) && this.args[1].equalsIgnoreCase("default")) {
            if (this.sender.hasPermission("tppets.setdefaultstorage")) {
                return true;
            } else {
                this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
            }
        }
        return false;
    }

    public void processCommandAdd() {
        Command command;
        if (this.isDefaultCommand()) {
            command = new CommandStorageAddDefault(this.thisPlugin, this.sender, Arrays.copyOfRange(this.args, 1, this.args.length));
        } else {
            command = new CommandStorageAdd(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
        }
        command.processCommand();
        command.displayStatus();
    }

    public void processCommandRemove() {
        Command command;
        if (this.isDefaultCommand()) {
            command = new CommandStorageRemoveDefault(this.thisPlugin, this.sender, Arrays.copyOfRange(this.args, 1, this.args.length));
        } else {
            command = new CommandStorageRemove(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
        }
        command.processCommand();
        command.displayStatus();
    }

    public void processCommandList() {
        Command command;
        if (this.isDefaultCommand()) {
            command = new CommandStorageListDefault(this.thisPlugin, this.sender, Arrays.copyOfRange(this.args, 1, this.args.length));
        } else {
            command = new CommandStorageList(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
        }
        command.processCommand();
        command.displayStatus();
    }

    public void processCommandGeneric() {
        switch(this.args[0].toLowerCase()) {
            case "add":
                processCommandAdd();
                break;
            case "remove":
                processCommandRemove();
                break;
            case "list":
                processCommandList();
                break;
            default:
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                break;
        }
    }

    private void displayErrors() {
        switch(this.commandStatus) {
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage [add/remove/list]");
                break;
        }
    }
}
