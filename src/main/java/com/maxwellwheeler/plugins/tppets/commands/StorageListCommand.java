package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

public abstract class StorageListCommand extends Command{
    StorageListCommand(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    protected void listAllStorages(Player pl, String storageLabel, List<? extends StorageLocation> storageLocations) {
        this.sender.sendMessage(ChatColor.GRAY + "----------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + storageLabel + "'s Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "----------");

        for (StorageLocation storageLocation : storageLocations) {
            this.listIndividualStorage(pl, storageLocation);
        }

        this.sender.sendMessage(ChatColor.GRAY + "----------------------------------------");
    }

    protected void listIndividualStorage(Player pl, StorageLocation storageLoc) {
        if (storageLoc != null && storageLoc.getLoc().getWorld() != null) {
            pl.sendMessage(ChatColor.BLUE + "name: " + ChatColor.WHITE + storageLoc.getStorageName());
            pl.sendMessage(ChatColor.BLUE + "    location: " + ChatColor.WHITE + storageLoc.getLoc().getBlockX() + ", " + storageLoc.getLoc().getBlockY() + ", " + storageLoc.getLoc().getBlockZ() + ", " + storageLoc.getLoc().getWorld().getName());
        }
    }
}
