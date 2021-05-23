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

/**
 * Class representing a /tpp release command.
 * @author GatheringExp
 */
public class CommandRelease extends BaseCommand {
    /**
     * Relays data to {@link BaseCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp release.
     */
    public CommandRelease(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * Calling this method indicates that all necessary data is in the instance and the command can be processed.
     */
    @Override
    public void processCommand() {
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
     *     <li>If the command is using f:[username] syntax, checks for tppets.releaseother.</li>
     * </ul>
     * @return True if command has a target player with proper permissions and a proper number of arguments, false if not.
     */
    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.releaseother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    /**
     * Releases {@link CommandRelease#commandFor}'s pet.
     */
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

    /**
     * Loads the chunk the pet was last seen in, and gets the entity object in the world.
     * @param petStorage The {@link PetStorage} to look for.
     * @return Entity if the entity was found, null if not.
     */
    private Entity getPet(PetStorage petStorage) {
        loadChunkFromPetStorage(petStorage);
        return getEntity(petStorage);
    }

    /**
     * Messages the command status to the {@link CommandRelease#sender}.
     */
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

    /**
     * Logs any command status messages.
     */
    private void logStatus() {
        if (this.commandStatus == CommandStatus.SUCCESS) {
            logSuccessfulAction("release", "released " + this.commandFor.getName() + "'s " + this.args[0]);
        } else {
            logUnsuccessfulAction("release", this.commandStatus.toString());
        }
    }
}
