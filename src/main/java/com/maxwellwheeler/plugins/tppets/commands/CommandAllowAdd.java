package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CommandAllowAdd extends BaseCommand {
    private CommandStatus commandStatus;
    private enum CommandStatus {SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, NO_TARGET_PLAYER, SYNTAX_ERROR, NO_PET, DB_FAIL}

    public CommandAllowAdd(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    private boolean correctForOtherPlayerSyntax() {
        // 1) Check that the command sender exists and is a player
        if (this.sender == null) {
            this.commandStatus = CommandStatus.INVALID_SENDER;
            return false;
        }

        // 2) Check that the player has permission to execute this type of command
        if (!this.sender.hasPermission("tppets.allowother")) {
            this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
            return false;
        }

        // 3) Check if a player with that name was found
        if (this.commandFor == null) {
            this.commandStatus = CommandStatus.NO_PLAYER;
            return false;
        }

        // 4) Check if there's enough arguments to make a valid command
        if (!ArgValidator.validateArgsLength(this.args, 2)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return false;
        }

        // 5) All clear for now. Set commandStatus to SUCCESS to override any earlier calls to similar methods
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
        if (!ArgValidator.validateArgsLength(args, 2)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return false;
        }

        // 3) All clear for now. Set commandStatus to SUCCESS to override any earlier calls to similar methods
        this.commandStatus = CommandStatus.SUCCESS;
        return true;
    }

    public void processCommand() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        if ((this.isIntendedForSomeoneElse && correctForOtherPlayerSyntax()) || (!this.isIntendedForSomeoneElse && correctForSelfSyntax())) {
            processCommandGeneric();
        }

        displayStatus();
    }

    private void processCommandGeneric() {
        OfflinePlayer playerToAllow = this.getOfflinePlayer(this.args[0]);

        if (playerToAllow == null) {
            this.commandStatus = CommandStatus.NO_TARGET_PLAYER;
            return;
        }

        if (!ArgValidator.softValidatePetName(this.args[1])) {
            this.commandStatus = CommandStatus.NO_PET;
            return;
        }

        List<PetStorage> petList = this.thisPlugin.getDatabase().getPetByName(this.commandFor.getUniqueId().toString(), this.args[1]);

        if (petList == null) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;
        }

        if (petList.size() == 0) {
            this.commandStatus = CommandStatus.NO_PET;
            return;
        }

        if (this.allowPlayer(petList.get(0).petId, UUIDUtils.trimUUID(playerToAllow.getUniqueId().toString()))) {
            this.thisPlugin.getLogWrapper().logSuccessfulAction(this.sender.getName() + " allowed " + this.args[0] + " to use " + this.commandFor.getName() + "'s pet named " + this.args[1]);
            this.commandStatus = CommandStatus.SUCCESS;
        } else {
            this.commandStatus = CommandStatus.DB_FAIL;
        }

    }

    private boolean allowPlayer(String petId, String playerId) {
        if (this.thisPlugin.getDatabase().insertAllowedPlayer(petId, playerId)) {
            if (!this.thisPlugin.getAllowedPlayers().containsKey(petId)) {
                this.thisPlugin.getAllowedPlayers().put(petId, new ArrayList<>());
            }
            this.thisPlugin.getAllowedPlayers().get(petId).add(playerId);

            return true;
        }

        return false;
    }

    private void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, NO_TARGET_PLAYER, SYNTAX_ERROR, NO_PET, DB_FAIL
        switch (this.commandStatus) {
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
        }
    }
}