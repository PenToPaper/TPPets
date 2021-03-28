package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.List;

public class CommandRename extends BaseCommand {
    public CommandRename(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    @Override
    public void processCommand() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayStatus();
    }

    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.renameother", 2)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(2));
    }

    private void processCommandGeneric() {
        try {
            if (!ArgValidator.softValidatePetName(this.args[0])) {
                this.commandStatus = CommandStatus.NO_TARGET_PET;
                return;
            }

            if (!ArgValidator.softValidatePetName(this.args[1])) {
                this.commandStatus = CommandStatus.INVALID_NAME;
                return;
            }

            List<PetStorage> pet = this.thisPlugin.getDatabase().getSpecificPet(this.commandFor.getUniqueId().toString(), this.args[0]);

            if (pet.size() == 0) {
                this.commandStatus = CommandStatus.NO_TARGET_PET;
                return;
            }

            if (!ArgValidator.validatePetName(this.thisPlugin.getDatabase(), this.commandFor.getUniqueId().toString(), this.args[1])) {
                this.commandStatus = CommandStatus.PET_NAME_ALREADY_IN_USE;
                return;
            }

            if (!this.thisPlugin.getDatabase().renamePet(this.commandFor.getUniqueId().toString(), this.args[0], this.args[1])) {
                this.commandStatus = CommandStatus.DB_FAIL;
                return;
            }

            this.thisPlugin.getLogWrapper().logSuccessfulAction(this.sender.getName() + " has changed " + this.commandFor.getName() + "'s pet named " + this.args[0] + " to " + this.args[1]);

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private void displayStatus() {
        // NO_TARGET_PET, INVALID_NAME, PET_NAME_ALREADY_IN_USE
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
            case NO_TARGET_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find pet named " + ChatColor.WHITE + this.args[0]);
                break;
            case INVALID_NAME:
                this.sender.sendMessage(ChatColor.WHITE + this.args[1] + ChatColor.RED + " is an invalid name");
                break;
            case PET_NAME_ALREADY_IN_USE:
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
}
