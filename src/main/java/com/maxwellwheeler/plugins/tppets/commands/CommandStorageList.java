package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandStorageList extends Command {
    CommandStorageList(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        listStorage();
        displayStatus();
    }

    private void listStorage() {
        if (this.thisPlugin.getDatabase() == null) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;
        }

        List<StorageLocation> storageLocations = this.thisPlugin.getDatabase().getStorageLocations(this.commandFor.getUniqueId().toString());

        if (storageLocations == null) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;
        }

        listAllStorages(this.sender, storageLocations);
    }

    private void listAllStorages(Player pl, List<StorageLocation> storageLocations) {
        this.sender.sendMessage(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + commandFor.getName() + "'s Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------");
        for (StorageLocation storageLocation : storageLocations) {
            this.listIndividualStorage(pl, storageLocation);
        }
        this.sender.sendMessage(ChatColor.GRAY + "----------------------------------------");
    }

    private void listIndividualStorage(Player pl, StorageLocation storageLoc) {
        if (storageLoc != null) {
            pl.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + storageLoc.getStorageName());
            // TODO REFACTOR TeleportCommand.formatLocation and put it in here
            pl.sendMessage(ChatColor.BLUE + "    location: " + ChatColor.WHITE + storageLoc.getLoc().getBlockX() + ", " + storageLoc.getLoc().getBlockY() + ", " + storageLoc.getLoc().getBlockZ() + ", " + storageLoc.getLoc().getWorld().getName());
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
