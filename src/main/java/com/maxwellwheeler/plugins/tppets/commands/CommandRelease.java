package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;

import java.sql.SQLException;

public class CommandRelease extends BaseCommand {
    public CommandRelease(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    @Override
    public void processCommand() {
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayStatus();
        logStatus();
    }

    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.releaseother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    private void processCommandGeneric() {
        try {
            if (!this.thisPlugin.getAllowUntamingPets() && !this.sender.hasPermission("tppets.releaseother")) {
                this.commandStatus = CommandStatus.NOT_ENABLED;
                return;
            }

            if (!ArgValidator.softValidatePetName(this.args[0])) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            PetStorage petStorage = this.thisPlugin.getDatabase().getSpecificPet(this.commandFor.getUniqueId().toString(), this.args[0]);

            if (petStorage == null) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            Entity pet = getPet(petStorage);

            // Also tests if pet == null
            if (!(pet instanceof Tameable)) {
                this.commandStatus = CommandStatus.NO_ENTITY;
                return;
            }

            if (this.thisPlugin.getDatabase().removePet(petStorage.petId)) {
                EntityActions.releasePetEntity((Tameable) pet);
            } else {
                this.commandStatus = CommandStatus.DB_FAIL;
            }

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private Entity getPet(PetStorage petStorage) {
        loadChunkFromPetStorage(petStorage);
        return getEntity(petStorage);
    }

    private void displayStatus() {
        switch(this.commandStatus) {
            case INVALID_SENDER:
                break;
            case SUCCESS:
                this.sender.sendMessage((this.isForSelf() ? ChatColor.BLUE + "Your pet " : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.BLUE + " pet ") + ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " has been released");
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not release pet");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case NO_ENTITY:
                this.sender.sendMessage(ChatColor.RED + "Can't find pet");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find pet named " + ChatColor.WHITE + this.args[0]);
                break;
            case NOT_ENABLED:
                this.sender.sendMessage(ChatColor.RED + "You can't release pets");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp release [pet name]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }

    private void logStatus() {
        if (this.commandStatus == CommandStatus.SUCCESS) {
            logSuccessfulAction("release", "released " + this.commandFor.getName() + "'s " + this.args[0]);
        } else {
            logUnsuccessfulAction("release", this.commandStatus.toString());
        }
    }
}
