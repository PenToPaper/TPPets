package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public abstract class BaseCommand extends Command {
    protected boolean isIntendedForSomeoneElse;

    public BaseCommand(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
        initializeCommandFor();
    }

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
