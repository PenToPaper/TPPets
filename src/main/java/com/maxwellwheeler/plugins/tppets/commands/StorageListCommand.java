package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

abstract class StorageListCommand extends Command {
    StorageListCommand(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    protected void listAllStorages(Player pl, String storageLabel, List<? extends StorageLocation> storageLocations) {
        this.sender.sendMessage(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + storageLabel + "'s Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "---------");

        for (StorageLocation storageLocation : storageLocations) {
            this.listIndividualStorage(pl, storageLocation);
        }
        // 31 chars in header - 3 characters (buffer, since [] are small characters and footer shouldn't be larger than header
        this.sender.sendMessage(ChatColor.GRAY + StringUtils.repeat("-", 28 + storageLabel.length()));
    }

    protected void listIndividualStorage(Player pl, StorageLocation storageLoc) {
        if (storageLoc != null && storageLoc.getLoc().getWorld() != null) {
            pl.sendMessage(ChatColor.BLUE + "Name: " + ChatColor.WHITE + storageLoc.getStorageName());
            pl.sendMessage(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + storageLoc.getLoc().getBlockX() + ", " + storageLoc.getLoc().getBlockY() + ", " + storageLoc.getLoc().getBlockZ() + ", " + storageLoc.getLoc().getWorld().getName());
        }
    }
}
