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

public class CommandStorageListDefault extends Command {
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

            listAllStorages(this.sender, allStorageLocations);

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private void listAllStorages(Player pl, List<ServerStorageLocation> storageLocations) {
        // Loops through the stored storage locations if there's been no failure. Lists them to the user.
        this.sender.sendMessage(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE +  "Server's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------");

        for (ServerStorageLocation storageLocation: storageLocations) {
            listIndividualStorage(pl, storageLocation);
        }

        this.sender.sendMessage(ChatColor.GRAY + "----------------------------------------");

    }

    private void listIndividualStorage(Player pl, ServerStorageLocation storageLoc) {
        if (storageLoc != null && storageLoc.getLoc().getWorld() != null) {
            pl.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + storageLoc.getStorageName());
            // TODO REFACTOR TeleportCommand.formatLocation and put it in here
            pl.sendMessage(ChatColor.BLUE + "    location: " + ChatColor.WHITE + storageLoc.getLoc().getBlockX() + ", " + storageLoc.getLoc().getBlockY() + ", " + storageLoc.getLoc().getBlockZ() + ", " + storageLoc.getLoc().getWorld().getName());
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
