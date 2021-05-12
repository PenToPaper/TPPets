package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;

/**
 * Object used for store commands
 * @author GatheringExp
 */

class CommandStore extends TeleportCommand {
    private boolean hasSpecificStorage;
    private PetStorage pet;

    /**
     * Generic constructor, needs to point to plugin for logging.
     * @param thisPlugin The {@link TPPets} instance.
     */
    public CommandStore(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    public void processCommand() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayStatus();
    }

    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.teleportother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    private void processCommandGeneric() {
        // The first argument is always the pet name. Check if it's a valid pet name.
        try {
            if (!ArgValidator.softValidatePetName(this.args[0])) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            StorageLocation storageLocation;

            if (ArgValidator.validateArgsLength(this.args, 2)) {
                // Syntax: /tpp store [pet name] [storage location]

                this.hasSpecificStorage = true;

                if (!ArgValidator.softValidateStorageName(this.args[1])) {
                    this.commandStatus = CommandStatus.INVALID_NAME;
                    return;
                }

                storageLocation = this.thisPlugin.getDatabase().getStorageLocation(this.commandFor.getUniqueId().toString(), this.args[1]);
            } else {
                // Syntax: /tpp store [pet name]

                storageLocation = this.thisPlugin.getDatabase().getServerStorageLocation("default", this.sender.getWorld());
                this.hasSpecificStorage = false;
            }

            if (storageLocation == null) {
                this.commandStatus = CommandStatus.INVALID_NAME;
                return;
            }

            this.pet = this.thisPlugin.getDatabase().getSpecificPet(this.commandFor.getUniqueId().toString(), this.args[0]);

            if (this.pet == null) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            if (!this.thisPlugin.getProtectedRegionManager().canTpThere(this.sender, storageLocation.getLoc())) {
                this.commandStatus = CommandStatus.CANT_TELEPORT_IN_PR;
                return;
            }

            if (!canTpToWorld(this.sender, this.pet.petWorld)) {
                this.commandStatus = CommandStatus.TP_BETWEEN_WORLDS;
                return;
            }

            if (storePet(storageLocation, this.pet)) {
                this.thisPlugin.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " teleported " + (isForSelf() ? "their" : this.commandFor.getName() + "'s") + " pet " + this.args[0] + " to storage location at: " + formatLocation(storageLocation.getLoc()));
            } else {
                this.commandStatus = CommandStatus.CANT_TELEPORT;
            }

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private boolean storePet(StorageLocation storageLocation, PetStorage pet) throws SQLException {
        try {
            return teleportPetsFromStorage(storageLocation.getLoc(), pet, true, !this.isIntendedForSomeoneElse || this.sender.hasPermission("tppets.teleportother"));
        } catch (SQLException ignored) {
            return true;
        }
    }

    public void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, SYNTAX_ERROR, NO_PET, NO_STORAGE, CANNOT_TP
        switch (this.commandStatus) {
            case INVALID_SENDER:
            case CANT_TELEPORT_IN_PR:
                break;
            case SUCCESS:
                this.sender.sendMessage((isForSelf() ? ChatColor.BLUE + "Your" : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.BLUE) + " pet has been stored successfully");
                break;
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
            case INVALID_NAME:
                this.sender.sendMessage(ChatColor.RED + "Could not find " + (this.hasSpecificStorage ? ChatColor.WHITE + this.args[1] : "default storage"));
                break;
            case CANT_TELEPORT:
                this.sender.sendMessage(ChatColor.RED + "Could not store " + (isForSelf() ? "your " : ChatColor.WHITE + this.commandFor.getName() + "'s " + ChatColor.RED) + "pet");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not process request");
                break;
            case TP_BETWEEN_WORLDS:
                this.sender.sendMessage(ChatColor.RED + "Can't teleport pet between worlds. Your pet is in " + ChatColor.WHITE + this.pet.petWorld);
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
