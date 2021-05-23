package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

/**
 * Class representing a /tpp storage server subcommand.
 * @author GatheringExp
 */
public class CommandStorageServer extends StorageListCommand {
    /**
     * Relays data to {@link StorageListCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp storage server.
     */
    CommandStorageServer(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * Calling this method indicates that all necessary data is in the instance and the command can be processed.
     */
    @Override
    public void processCommand() {
        listStorage();
        displayStatus();
    }

    /**
     * Lists all {@link ServerStorageLocation}s in {@link CommandStorageServer#sender}'s world.
     */
    private void listStorage() {
        try {

            List<ServerStorageLocation> storageLocations = this.thisPlugin.getDatabase().getServerStorageLocations(this.sender.getWorld());
            listAllStorages(this.sender, "Server", storageLocations);

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages the command status to the {@link CommandStorageServer#sender}.
     */
    public void displayStatus() {
        switch (this.commandStatus) {
            case SUCCESS:
            case INVALID_SENDER:
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not find server storage locations");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
