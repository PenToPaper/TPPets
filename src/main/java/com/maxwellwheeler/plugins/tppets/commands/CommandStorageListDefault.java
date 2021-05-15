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

public class CommandStorageListDefault extends StorageListCommand {
    CommandStorageListDefault(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        listDefaultStorages();
        displayStatus();
    }

    private void listDefaultStorages() {
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

    private void displayStatus() {
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
