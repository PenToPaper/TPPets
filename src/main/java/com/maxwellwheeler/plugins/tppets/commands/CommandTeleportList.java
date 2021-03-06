package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.List;

/**
 * Class representing a /tpp list command.
 * @author GatheringExp
 */
public class CommandTeleportList extends TeleportCommand {
    /** The {@link PetType.Pets} type to be listed. */
    private PetType.Pets petType;
    /** A list of {@link PetStorage}s to be listed. */
    private List<PetStorage> petList;

    /**
     * Relays data to {@link TeleportCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp list.
     */
    public CommandTeleportList(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp list [Pet Type]</li>
     * </ul>
     */
    public void processCommand() {
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayStatus();
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
     * Lists all of {@link CommandTeleportList#commandFor}'s pets of a particular type to {@link CommandTeleportList#sender}.
     */
    private void processCommandGeneric() {
        this.petType = getPetType(this.args[0]);

        if (this.petType == null || this.petType == PetType.Pets.UNKNOWN) {
            this.commandStatus = CommandStatus.NO_PET_TYPE;
            return;
        }

        if (!PermissionChecker.hasPermissionToTeleportType(this.petType, this.sender)) {
            this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
            return;
        }

        if (!initializePetList()) {
            return;
        }

        announcePetsFromList();
    }

    /**
     * Initializes and populates {@link CommandTeleportList#petList} from {@link CommandTeleportList#thisPlugin}'s {@link com.maxwellwheeler.plugins.tppets.storage.SQLWrapper}.
     * @return true if populated, false if not.
     */
    private boolean initializePetList() {
        try {
            List<PetStorage> petList = this.thisPlugin.getDatabase().getPetTypeFromOwner(this.commandFor.getUniqueId().toString(), this.petType);

            if (petList.size() == 0) {
                this.commandStatus = CommandStatus.NO_PET;
                return false;
            }

            this.petList = petList;
            return true;
        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return false;
        }
    }
    /**
     * Lists all pets from {@link CommandTeleportList#petList} to {@link CommandTeleportList#sender}
     */
    private void announcePetsFromList() {
        this.sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + this.commandFor.getName() + "'s " + this.petType.toString().toLowerCase() + ChatColor.BLUE + " names ]" + ChatColor.DARK_GRAY + "---------");
        for (int i = 0; i < this.petList.size(); i++) {
            this.sender.sendMessage(ChatColor.WHITE + "  " + (i + 1) + ") " + this.petList.get(i).petName + (canTpToWorld(this.sender, this.petList.get(i).petWorld) ? "" : ChatColor.RED + " (In: " + this.petList.get(i).petWorld + ")"));
        }
        // 31 chars in header - 3 characters (buffer, since [] are small characters and footer shouldn't be larger than header
        this.sender.sendMessage(ChatColor.DARK_GRAY + StringUtils.repeat("-", 28 + (this.commandFor.getName() == null ? 4 : this.commandFor.getName().length()) + this.petType.toString().length()));
    }

    /**
     * Messages the command status to the {@link CommandTeleportList#sender}.
     */
    private void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, SYNTAX_ERROR, NO_PET, NO_PET_TYPE, DB_FAIL
        switch (this.commandStatus) {
            case SUCCESS:
            case INVALID_SENDER:
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case SYNTAX_ERROR:
            case NO_PET_TYPE:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp list [pet type]");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find any " + ChatColor.WHITE + this.petType.toString().toLowerCase() + "s");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not find pets");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
