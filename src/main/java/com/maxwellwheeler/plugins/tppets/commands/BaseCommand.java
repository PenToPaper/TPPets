package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class BaseCommand {
    protected boolean isIntendedForSomeoneElse;
    public OfflinePlayer commandFor;
    public Player sender;
    public String[] args;
    public TPPets thisPlugin;
    public enum CommandStatus {SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, SYNTAX_ERROR, NO_PET, NO_PET_TYPE, DB_FAIL, CANT_TELEPORT, CANT_TELEPORT_IN_PR}
    protected CommandStatus commandStatus = CommandStatus.SUCCESS;

    public BaseCommand(TPPets thisPlugin, CommandSender sender, String[] args) {
        this.thisPlugin = thisPlugin;
        this.sender = null;
        this.commandFor = null;
        if (sender instanceof Player) {
            this.sender = (Player) sender;
        }
        this.args = args;
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

    public boolean isForSelf() {
        return this.sender.equals(this.commandFor);
    }

    @SuppressWarnings("deprecation")
    public OfflinePlayer getOfflinePlayer(String username) {
        if (ArgValidator.validateUsername(username)) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(username);
            if (player.hasPlayedBefore()) {
                return player;
            }
        }
        return null;
    }

    public CommandStatus getCommandStatus() {
        return this.commandStatus;
    }
}
