package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a /tpp all command.
 * @author GatheringExp
 */
public class CommandTeleportAll extends TeleportCommand {
    /** The {@link PetType.Pets} type to be teleported. */
    private PetType.Pets petType;
    /** A list of {@link PetStorage}s to be teleported. */
    private List<PetStorage> petList;
    /** A list of {@link PetStorage}s that were unable to be teleported. */
    private final List<PetStorage> errorPetList = new ArrayList<>();

    /**
     * Relays data to {@link TeleportCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp all.
     */
    public CommandTeleportAll(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp all [Pet Type]</li>
     * </ul>
     */
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
     *     <li>If the command is using f:[username] syntax, checks for tppets.teleportother.</li>
     * </ul>
     * @return True if command has a target player with proper permissions and a proper number of arguments, false if not.
     */
    private boolean isValidSyntax() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.teleportother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    /**
     * Teleports all of {@link CommandTeleportAll#commandFor}'s pets of a particular type to {@link CommandTeleportAll#sender}.
     */
    private void processCommandGeneric() {
        try {
            if (!initializePetList()) {
                return;
            }

            if (!PermissionChecker.hasPermissionToTeleportType(this.petType, this.sender)) {
                this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
                return;
            }

            if (!this.thisPlugin.getProtectedRegionManager().canTpThere(this.sender, this.sender.getLocation())) {
                this.commandStatus = CommandStatus.CANT_TELEPORT_IN_PR;
                return;
            }

            teleportAllPets();

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Initializes and populates {@link CommandTeleportAll#petList} from {@link CommandTeleportAll#thisPlugin}'s {@link com.maxwellwheeler.plugins.tppets.storage.SQLWrapper}.
     * @return true if populated, false if not.
     * @throws SQLException If getting all pets of {@link CommandTeleportAll#petType} from the database fails.
     */
    private boolean initializePetList() throws SQLException {
        this.petType = getPetType(this.args[0]);

        if (this.petType == null || this.petType == PetType.Pets.UNKNOWN) {
            this.commandStatus = CommandStatus.NO_PET_TYPE;
            return false;
        }

        List<PetStorage> petList = this.thisPlugin.getDatabase().getPetTypeFromOwner(this.commandFor.getUniqueId().toString(), this.petType);

        if (petList.size() == 0) {
            this.commandStatus = CommandStatus.NO_PET;
            return false;
        }

        this.petList = petList;

        return true;
    }

    /**
     * Teleports all pets from {@link CommandTeleportAll#petList} to {@link CommandTeleportAll#sender}
     */
    private void teleportAllPets() {
        for (PetStorage petStorage : this.petList) {
            if (!canTpToWorld(this.sender, petStorage.petWorld) || !teleportPetFromStorage(this.sender.getLocation(), petStorage, this.isIntendedForSomeoneElse, !this.isIntendedForSomeoneElse || this.sender.hasPermission("tppets.teleportother"))) {
                this.commandStatus = CommandStatus.CANT_TELEPORT;
                this.errorPetList.add(petStorage);
            }
        }
    }

    /**
     * Messages the command status to the {@link CommandTeleportAll#sender}.
     */
    private void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, SYNTAX_ERROR, NO_PET, NO_PET_TYPE, DB_FAIL, CANT_TELEPORT
        switch (this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage((this.isIntendedForSomeoneElse ? ChatColor.WHITE + this.commandFor.getName() + "'s " : ChatColor.BLUE + "Your ") + ChatColor.WHITE + this.petType.toString().toLowerCase() + "s " + ChatColor.BLUE + "have been teleported to you");
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case SYNTAX_ERROR:
            case NO_PET_TYPE:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp all [pet type]");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find any " + ChatColor.WHITE + this.petType.toString().toLowerCase() + "s");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not get any pets");
                break;
            case CANT_TELEPORT:
                this.sender.sendMessage(ChatColor.RED + "Teleported all pets except: " + ChatColor.WHITE + getErrorPetNames());
                break;
            case CANT_TELEPORT_IN_PR:
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }

    /**
     * Generates a string representing {@link CommandTeleportAll#errorPetList}
     * @return A comma-separated list of the pet names in {@link CommandTeleportAll#errorPetList}
     */
    private String getErrorPetNames() {
        StringBuilder errorPetNames = new StringBuilder();
        for (PetStorage errorPet : this.errorPetList) {
            errorPetNames.append(errorPet.petName).append(", ");
        }
        errorPetNames.delete(errorPetNames.lastIndexOf(", "), errorPetNames.length());
        return errorPetNames.toString();
    }

    /**
     * Logs any command status messages.
     */
    private void logStatus() {
        if (this.commandStatus == CommandStatus.SUCCESS) {
            logSuccessfulAction("all", "teleported " + this.commandFor.getName() + "'s " + this.petType.toString().toLowerCase() + "s");
        } else {
            logUnsuccessfulAction("all", this.commandStatus.toString());
        }
    }
}
