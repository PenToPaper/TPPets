package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.ServerStorageLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a /tpp serverstorage list subcommand.
 * @author GatheringExp
 */
public class CommandServerStorageList extends StorageListCommand {
    /**
     * Relays data to {@link StorageListCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp serverstorage list.
     */
    CommandServerStorageList(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * <p>Calling this method indicates that all necessary data is in the instance and the command can be processed.</p>
     * <p>Expected Syntax:</p>
     * <ul>
     *      <li>/tpp serverstorage list</li>
     * </ul>
     */
    @Override
    public void processCommand() {
        listServerStorages();
        displayErrors();
    }

    /**
     * Lists all {@link ServerStorageLocation}s to {@link CommandServerStorageList#sender}
     */
    private void listServerStorages() {
        try {

            // Checks all worlds and stores their storage locations. Doing this first checks for database failures\
            List<ServerStorageLocation> allStorageLocations = new ArrayList<>();

            for (World world : Bukkit.getWorlds()) {
                List<ServerStorageLocation> storageLocations = this.thisPlugin.getDatabase().getServerStorageLocations(world);

                allStorageLocations.addAll(storageLocations);
            }

            listAllStorages(this.sender, "Server", allStorageLocations);

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    /**
     * Messages any command status errors to the {@link CommandServerStorageList#sender}.
     */
    private void displayErrors() {
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
