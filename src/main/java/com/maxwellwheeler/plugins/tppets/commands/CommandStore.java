package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Object used for store commands
 * @author GatheringExp
 */

class CommandStore extends TeleportCommand {
    private CommandStatus commandStatus;
    private boolean hasSpecificStorage;
    private enum CommandStatus{SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, SYNTAX_ERROR, NO_PET, NO_STORAGE, CANNOT_TP}

    /**
     * Generic constructor, needs to point to plugin for logging.
     * @param thisPlugin The {@link TPPets} instance.
     */
    public CommandStore(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
        this.commandStatus = CommandStatus.SUCCESS;
    }

    public boolean isForSelf() {
        return this.sender.equals(this.commandFor);
    }

    private boolean correctForOtherPlayerSyntax() {
        // 1) Check that the command sender exists and is a player
        if (this.sender == null) {
            this.commandStatus = CommandStatus.INVALID_SENDER;
            return false;
        }

        // 2) Check that the player has permission to execute this type of command
        if (!this.sender.hasPermission("tppets.teleportother")) {
            this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
            return false;
        }

        // 3) Check if a player with that name was found
        if (this.commandFor == null) {
            this.commandStatus = CommandStatus.NO_PLAYER;
            return false;
        }

        // 4) Check if there's enough arguments to make a valid command
        if (!ArgValidator.validateArgsLength(this.args, 1)) {
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
        if (!ArgValidator.validateArgsLength(args, 1)) {
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
        // The first argument is always the pet name. Check if it's a valid pet name.
        if (!ArgValidator.softValidatePetName(this.args[0])) {
            this.commandStatus = CommandStatus.NO_PET;
            return;
        }

        StorageLocation storageLocation;

        if (ArgValidator.validateArgsLength(this.args, 2)) {
            // Syntax: /tpp store [pet name] [storage location]

            this.hasSpecificStorage = true;

            if (!ArgValidator.validateStorageName(this.args[1])) {
                this.commandStatus = CommandStatus.NO_STORAGE;
                return;
            }

            storageLocation = this.thisPlugin.getDatabase().getStorageLocation(this.commandFor.getUniqueId().toString(), this.args[1]);
        } else {
            // Syntax: /tpp store [pet name]

            storageLocation = this.thisPlugin.getDatabase().getDefaultServerStorageLocation(this.sender.getWorld());
            this.hasSpecificStorage = false;
        }

        if (storageLocation != null) {
            if (storePet(storageLocation)) {
                thisPlugin.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " teleported " + (isForSelf() ? "their" : this.commandFor.getName() + "'s") + " pet " + this.args[0] + " to storage location at: " + formatLocation(storageLocation.getLoc()));
            } else {
                this.commandStatus = CommandStatus.CANNOT_TP;
            }
        } else {
            this.commandStatus = CommandStatus.NO_STORAGE;
        }

    }

    public boolean storePet(StorageLocation storageLocation) {
        return teleportSpecificPet(storageLocation.getLoc(), this.commandFor, this.args[0], PetType.Pets.UNKNOWN, true, !this.isIntendedForSomeoneElse || this.sender.hasPermission("tppets.teleportother"), false);
    }

    public void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, SYNTAX_ERROR, NO_PET, NO_STORAGE, CANNOT_TP
        switch (this.commandStatus) {
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp store [pet name] [storage name]");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  this.args[0]);
                break;
            case NO_STORAGE:
                this.sender.sendMessage(ChatColor.RED + "Could not find " + (this.hasSpecificStorage ? ChatColor.WHITE + this.args[1] : "default storage"));
                break;
            case CANNOT_TP:
                this.sender.sendMessage(ChatColor.RED + "Could not store " + (isForSelf() ? "your " : ChatColor.WHITE + this.commandFor.getName() + "'s " + ChatColor.RED) + "pet");
                break;
            case SUCCESS:
                this.sender.sendMessage((isForSelf() ? ChatColor.BLUE + "Your" : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.BLUE) + " pet has been stored successfully");
                break;
        }
    }
}
