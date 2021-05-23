package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Class representing any TPP storage list command.
 * @author GatheringExp
 */
abstract class StorageListCommand extends Command {
    /**
     * Relays data to {@link StorageListCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param commandFor The player that the command is targeting.
     * @param args A truncated list of arguments, depending on the expected command.
     */
    StorageListCommand(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    /**
     * Lists all {@link StorageLocation}s in a list to {@link StorageListCommand#sender}.
     * @param pl The player to display {@link StorageLocation}s to.
     * @param storageLabel A string representing the owner of the {@link StorageLocation}s for use in the response header.
     * @param storageLocations A list of {@link StorageLocation}s to display to the player.
     */
    protected void listAllStorages(Player pl, String storageLabel, List<? extends StorageLocation> storageLocations) {
        this.sender.sendMessage(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + storageLabel + "'s Storage" + ChatColor.BLUE + "]" + ChatColor.GRAY + "---------");

        for (StorageLocation storageLocation : storageLocations) {
            this.listIndividualStorage(pl, storageLocation);
        }
        // 31 chars in header - 3 characters (buffer, since [] are small characters and footer shouldn't be larger than header
        this.sender.sendMessage(ChatColor.GRAY + StringUtils.repeat("-", 28 + storageLabel.length()));
    }

    /**
     * Lists a single {@link StorageLocation} to {@link StorageListCommand#sender}.
     * @param pl The player to display the {@link StorageLocation} to.
     * @param storageLoc The {@link StorageLocation} to display to the player.
     */
    protected void listIndividualStorage(Player pl, StorageLocation storageLoc) {
        if (storageLoc != null && storageLoc.getLoc().getWorld() != null) {
            pl.sendMessage(ChatColor.BLUE + "Name: " + ChatColor.WHITE + storageLoc.getStorageName());
            pl.sendMessage(ChatColor.BLUE + "    Location: " + ChatColor.WHITE + storageLoc.getLoc().getBlockX() + ", " + storageLoc.getLoc().getBlockY() + ", " + storageLoc.getLoc().getBlockZ() + ", " + storageLoc.getLoc().getWorld().getName());
        }
    }
}
