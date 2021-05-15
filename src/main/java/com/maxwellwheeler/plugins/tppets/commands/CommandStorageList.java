package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;

public class CommandStorageList extends StorageListCommand {
    CommandStorageList(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        listStorage();
        displayStatus();
    }

    private void listStorage() {
        try {

            List<PlayerStorageLocation> storageLocations = this.thisPlugin.getDatabase().getStorageLocations(this.commandFor.getUniqueId().toString());
            listAllStorages(this.sender, this.commandFor.getName(), storageLocations);

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    public void displayStatus() {
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
