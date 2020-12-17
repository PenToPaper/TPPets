package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandStorageListDefault implements Command {
    private final TPPets tpPets;
    private final Player sender;
    private final String[] args;
    private CommandStatus commandStatus;
    private enum CommandStatus{SUCCESS, DB_FAIL}

    CommandStorageListDefault(TPPets tpPets, Player sender, String[] args) {
        this.tpPets = tpPets;
        this.sender = sender;
        this.args = args;
        this.commandStatus = CommandStatus.SUCCESS;
    }

    @Override
    public void processCommand() {
        if (this.tpPets.getDatabase() == null) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;
        }

        this.sender.sendMessage(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE +  "Server's Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------");
        for (World world : Bukkit.getWorlds()) {
            this.sender.sendMessage(ChatColor.WHITE + world.getName());
            List<StorageLocation> storageLocations = this.tpPets.getDatabase().getServerStorageLocations(world);
            if (storageLocations == null) {
                this.commandStatus = CommandStatus.DB_FAIL;
                return;
            }
            for (StorageLocation storageLocation : storageLocations) {
                listIndividualStorage(this.sender, storageLocation);
            }
        }
        this.sender.sendMessage(ChatColor.GRAY + "----------------------------------------");

    }

    private void listIndividualStorage (Player pl, StorageLocation storageLoc) {
        if (storageLoc != null) {
            pl.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + storageLoc.getStorageName());
            // TODO REFACTOR TeleportCommand.formatLocation and put it in here
            pl.sendMessage(ChatColor.BLUE + "    location: " + ChatColor.WHITE + storageLoc.getLoc().getBlockX() + ", " + storageLoc.getLoc().getBlockY() + ", " + storageLoc.getLoc().getBlockZ() + ", " + storageLoc.getLoc().getWorld().getName());
        }
    }

    @Override
    public void displayStatus() {
        // SUCCESS, DB_FAIL, LIMIT_REACHED, INVALID_NAME, ALREADY_DONE, SYNTAX_ERROR
        if (this.commandStatus == CommandStatus.DB_FAIL) {
            this.sender.sendMessage(ChatColor.RED + "Could not find storage locations");
        }
    }
}
