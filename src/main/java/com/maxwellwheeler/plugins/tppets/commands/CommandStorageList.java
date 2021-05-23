package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

/**
 * Class representing a /tpp storage list subcommand.
 * @author GatheringExp
 */
public class CommandStorageList extends StorageListCommand {
    /**
     * Relays data to {@link StorageListCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp storage list.
     */
    CommandStorageList(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp storage list</li>
     * </ul>
     */
    @Override
    public void processCommand() {
        listStorage();
        displayErrors();
    }

    /**
     * Lists all {@link Command#commandFor}'s {@link PlayerStorageLocation}s to {@link CommandStorageList#sender}
     */
    private void listStorage() {
        try {

            List<PlayerStorageLocation> storageLocations = this.thisPlugin.getDatabase().getStorageLocations(this.commandFor.getUniqueId().toString());
            listAllStorages(this.sender, this.commandFor.getName(), storageLocations);

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages any command status errors to the {@link CommandStorageList#sender}.
     */
    public void displayErrors() {
        switch (this.commandStatus) {
            case SUCCESS:
            case INVALID_SENDER:
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not find storage locations");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
