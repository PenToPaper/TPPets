package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;

/**
 * Class representing a /tpp store command.
 * @author GatheringExp
 */
class CommandStore extends TeleportCommand {
    /** Represents whether or not the pet is to be sent to a specific storage or a server default. */
    private boolean hasSpecificStorage;
    /** The pet to store. Can be null. */
    private PetStorage pet;

    /**
     * Relays data to {@link TeleportCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp store.
     */
    public CommandStore(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp store [Pet Name], which teleports to the server default, if available</li>
     *      <li>/tpp store [Pet Name] [Storage Name], which teleports to your storage with given name, or server storage
     *      with given name if none exists</li>
     * </ul>
     */
    public void processCommand() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayStatus();
        logStatus();
    }

    /**
     * Performs basic checks to the command's syntax:
     * <ul>
     *     <li>Checks that the sender is a player</li>
     *     <li>Checks that the command has the minimum number of arguments (1)</li>
     *     <li>If the command is using f:[username] syntax, checks for tppets.teleportother.</li>
     * </ul>
     * @return True if command has a target player with proper permissions and a proper number of arguments, false if not.
     */
    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.teleportother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    /**
     * <p>Gets a target storage location based on the command arguments.</p>
     * <p>If a specific storage location name is supplied, it first looks for a {@link com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation}
     * with that name. If it can't find any, it looks for a {@link com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation} with that name.
     * It will return null if it fails to find both, even if a server default exists.</p>
     * <p>If no specific storage location name is supplied, it looks for a default server storage location in that world.</p>
     * @return A {@link StorageLocation} to send the pet, or null if none can be found.
     * @throws SQLException If getting any storage location from the database fails.
     */
    private StorageLocation getStorageLocation() throws SQLException {
        StorageLocation storageLocation;

        if (ArgValidator.validateArgsLength(this.args, 2)) {
            // Syntax: /tpp store [pet name] [storage location]

            this.hasSpecificStorage = true;

            if (!ArgValidator.softValidateStorageName(this.args[1])) {
                this.commandStatus = CommandStatus.INVALID_NAME;
                return null;
            }

            storageLocation = this.thisPlugin.getDatabase().getStorageLocation(this.commandFor.getUniqueId().toString(), this.args[1]);

            if (storageLocation == null) {
                storageLocation = this.thisPlugin.getDatabase().getServerStorageLocation(this.args[1], this.sender.getWorld());
            }

        } else {
            // Syntax: /tpp store [pet name]

            storageLocation = this.thisPlugin.getDatabase().getServerStorageLocation("default", this.sender.getWorld());
            this.hasSpecificStorage = false;
        }

        return storageLocation;
    }

    private void processCommandGeneric() {
        // The first argument is always the pet name. Check if it's a valid pet name.
        try {
            if (!ArgValidator.softValidatePetName(this.args[0])) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            this.pet = this.thisPlugin.getDatabase().getSpecificPet(this.commandFor.getUniqueId().toString(), this.args[0]);

            if (this.pet == null) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            StorageLocation storageLocation = getStorageLocation();

            if (storageLocation == null) {
                this.commandStatus = CommandStatus.INVALID_NAME;
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

            if (!storePet(storageLocation, this.pet)) {
                this.commandStatus = CommandStatus.CANT_TELEPORT;
            }

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private boolean storePet(StorageLocation storageLocation, PetStorage pet) {
        return teleportPetFromStorage(storageLocation.getLoc(), pet, true, !this.isIntendedForSomeoneElse || this.sender.hasPermission("tppets.teleportother"));
    }

    public void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, SYNTAX_ERROR, NO_PET, NO_STORAGE, CANNOT_TP
        switch (this.commandStatus) {
            case INVALID_SENDER:
            case CANT_TELEPORT_IN_PR:
                break;
            case SUCCESS:
                this.sender.sendMessage((isForSelf() ? ChatColor.BLUE + "Your" : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.BLUE) + " pet " + ChatColor.WHITE + this.pet.petName + ChatColor.BLUE + " has been stored successfully");
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
                this.sender.sendMessage(ChatColor.RED + "Could not find location: " + ChatColor.WHITE + (this.hasSpecificStorage ? this.args[1] : "default storage"));
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

    private void logStatus() {
        if (this.commandStatus == CommandStatus.SUCCESS) {
            logSuccessfulAction("store", "stored " + this.commandFor.getName() + "'s " + this.args[0] + " at " + (this.hasSpecificStorage ? this.args[1] : "default"));
        } else {
            logUnsuccessfulAction("store", this.commandStatus.toString());
        }
    }
}
