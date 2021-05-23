package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Class representing a /tpp allowed command.
 * @author GatheringExp
 */
public class CommandAllowList extends BaseCommand {
    /**
     * Relays data to {@link BaseCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp allowed.
     */
    public CommandAllowList(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp allowed [Pet Name]</li>
     * </ul>
     */
    public void processCommand() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayErrors();
    }

    /**
     * Performs basic checks to the command's syntax:
     * <ul>
     *     <li>Checks that the sender is a player</li>
     *     <li>Checks that the command has the minimum number of arguments (1)</li>
     *     <li>If the command is using f:[username] syntax, checks for tppets.allowother.</li>
     * </ul>
     * @return True if command has a target player with proper permissions and a proper number of arguments, false if not.
     */
    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.allowother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    /**
     * Lists the allowed players to {@link CommandAllowList#commandFor}'s pet.
     */
    private void processCommandGeneric() {
        try {
            if (!ArgValidator.softValidatePetName(this.args[0])) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            PetStorage pet = this.thisPlugin.getDatabase().getSpecificPet(this.commandFor.getUniqueId().toString(), this.args[0]);

            if (pet == null) {
                this.commandStatus = CommandStatus.NO_PET;
                return;
            }

            List<String> playerUUIDs = this.thisPlugin.getGuestManager().getGuestsToPet(pet.petId);

            this.announceGuests(playerUUIDs);

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Announces the allowed players to {@link CommandAllowList#sender}
     * @param playerUUIDs A list of trimmed player UUIDs as strings.
     */
    private void announceGuests(List<String> playerUUIDs) {
        this.sender.sendMessage(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE +  this.commandFor.getName() + "'s " + this.args[0] + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------");

        for (String playerUUID : playerUUIDs) {
            String untrimmedUUID = UUIDUtils.unTrimUUID(playerUUID);
            if (untrimmedUUID != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(untrimmedUUID));
                if (offlinePlayer.hasPlayedBefore()) {
                    this.sender.sendMessage(ChatColor.WHITE + offlinePlayer.getName());
                }
            }
        }

        // 45 chars in header - 3 characters (buffer, since [] are small characters and footer shouldn't be larger than header
        this.sender.sendMessage(ChatColor.GRAY + StringUtils.repeat("-", 42 + (this.commandFor.getName() == null ? 4 : this.commandFor.getName().length()) + this.args[0].length()));
    }

    /**
     * Messages any command status errors to the {@link CommandAllowList#sender}.
     */
    private void displayErrors() {
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
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp list [pet name]");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  this.args[0]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not find allowed users");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
