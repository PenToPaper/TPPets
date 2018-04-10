package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class CommandStore extends TeleportCommand {
    /**
     * Generic constructor, needs to point to plugin for logging.
     * @param thisPlugin The {@link TPPets} instance.
     */
    public CommandStore(TPPets thisPlugin) {
        super(thisPlugin);
    }

    // Desired Syntax: /tpp store [pet name] [storage location]
    // Desired Syntax: /tpp store [pet name] [storage location defaults to "default"]
    // Desired Syntax: /tpp store f:[username] [pet name] [storage location]
    // Desired Syntax: /tpp store f:[username] [pet name] [storage location defaults to "default"]
    // If allowTpBetweenWorlds() find only stores in world
    @SuppressWarnings("deprecation")
    public void processCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player && ArgValidator.validateArgsLength(args, 1)) {
            Player playerTemp = (Player) sender;
            String isForSomeoneElse = ArgValidator.isForSomeoneElse(args[0]);
            if (isForSomeoneElse != null && sender.hasPermission("tppets.teleportother") && ArgValidator.validateUsername(isForSomeoneElse)) {
                OfflinePlayer commandFor = Bukkit.getOfflinePlayer(isForSomeoneElse);
                if (commandFor != null && commandFor.hasPlayedBefore() && ArgValidator.validateArgsLength(args, 2)) {
                    processCommandGeneric(playerTemp, commandFor, Arrays.copyOfRange(args, 1, args.length));
                }
            } else {
                processCommandGeneric(playerTemp, playerTemp, args);
            }
        }
    }

    public void processCommandGeneric(Player pl, OfflinePlayer commandFor, String[] args) {
        if (ArgValidator.validateArgsLength(args, 2)) {
            // Syntax received: /tpp store [pet name] [storage location]
            if (ArgValidator.softValidatePetName(args[0])) {
                if (ArgValidator.validateStorageName(args[1])) {
                    StorageLocation storageLoc = thisPlugin.getDatabase().getStorageLocation(commandFor.getUniqueId().toString(), args[1]);
                    if (storageLoc != null && storePet(commandFor, args[0], storageLoc)) {
                        thisPlugin.getLogger().info("Player " + pl.getName() + " has teleported their pet " + args[0] + " to location " + storageLoc.getStorageName() + "at " + TeleportCommand.formatLocation(storageLoc.getLoc()));
                        pl.sendMessage((pl.equals(commandFor) ? ChatColor.BLUE + "Your " : ChatColor.WHITE + commandFor.getName() + "'s ") + ChatColor.WHITE + args[0] + ChatColor.BLUE + " has been teleported to " + ChatColor.WHITE + storageLoc.getStorageName());
                        return;
                    }
                }
            }
            pl.sendMessage(ChatColor.RED + "Could not find " + ChatColor.WHITE + args[0]);
        } else if (ArgValidator.validateArgsLength(args, 1)) {
            // Syntax received: /tpp store [pet name]
            StorageLocation storageLoc = thisPlugin.getDatabase().getDefaultServerStorageLocation(pl.getWorld());
            if (storageLoc != null) {
                if (storePet(commandFor, args[0], storageLoc)) {
                    thisPlugin.getLogger().info("Player " + pl.getName() + " has teleported their pet " + args[0] + " to server location" + storageLoc.getStorageName() + "at " + TeleportCommand.formatLocation(storageLoc.getLoc()));
                    pl.sendMessage((pl.equals(commandFor) ? ChatColor.BLUE + "Your " : ChatColor.WHITE + commandFor.getName() + "'s ") + ChatColor.WHITE + args[0] + ChatColor.BLUE + " has been teleported to " + ChatColor.WHITE + storageLoc.getStorageName());
                    return;
                }
                pl.sendMessage(ChatColor.RED + "Could not find " + ChatColor.WHITE + args[0]);
            } else {
                pl.sendMessage(ChatColor.RED + "Could not find default storage location");
            }
        }
    }

    public boolean storePet(OfflinePlayer petOwner, String petName, StorageLocation storageLoc) {
        return teleportSpecificPet(storageLoc.getLoc(), petOwner, petName, PetType.Pets.UNKNOWN, true, false);
    }

    @Override
    protected boolean hasPermissionToTp(Player pl, OfflinePlayer petOwner, String petUUID) {
        return false;
    }
}
