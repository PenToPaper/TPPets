package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Object that processes tpp pet rename commands:
 * /tpp rename [existing pet name] [new pet name]
 * /tpp rename f:[username] [existing pet name] [new pet name]
 * @author GatheringExp
 *
 */
class CommandRename {
    private TPPets thisPlugin;

    /**
     * Generic constructor, supplies reference to TPPets plugin instance.
     * @param thisPlugin The TPPets plugin instance
     */
    public CommandRename(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    /**
     * Syntax: /tpp rename [existing pet name] [new pet name]
     * Renames sender's [existing pet name] to [new pet name]
     * Alternative Syntax: /tpp rename f:[username] [existing pet name] [new pet name]
     * Renames [username]'s [existing pet name] to [new pet name]
     * @param sender The {@link CommandSender} object that originally sent the command.
     * @param args The arguments passed with the command - doesn't include the "tpp rename" in command. Ex: /tpp rename OldName NewName, String args[] would have {OldName, NewName}.
     */
    @SuppressWarnings("deprecation")
    public void processCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player playerTemp = (Player) sender;
            // Checks if command meets basic syntax requirements
            if (ArgValidator.validateArgsLength(args,2)) {
                // Checks if command is /tpp rename f:[username] [existing pet name] [new pet name]
                String someoneElse = ArgValidator.isForSomeoneElse(args[0]);
                if (someoneElse != null && ArgValidator.validateUsername(someoneElse) && ArgValidator.validateArgsLength(args, 3)) {
                    if (!sender.hasPermission("tppets.renameall")) {
                        sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                        return;
                    }
                    // Checks if f:[usrename] has a valid UUID, if it does, it runs that command
                    OfflinePlayer offlinePlayerTemp = Bukkit.getOfflinePlayer(someoneElse);
                    if (offlinePlayerTemp != null && offlinePlayerTemp.hasPlayedBefore()) {
                        renamePet(playerTemp, offlinePlayerTemp, args[1], args[2]);
                    } else {
                        playerTemp.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + someoneElse);
                    }
                // Checks if syntax is /tpp rename [existing pet name] [new pet name]
                } else {
                    renamePet(playerTemp, playerTemp, args[0], args[1]);
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Syntax error. Usage: /tpp rename [old name] [new name]");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Error: Sender is not a player.");
        }
    }

    /**
     * Generic processor for the /tpp rename [args] commands
     * @param pl The player that is sending the command
     * @param commandAbout The player that the command is about, presumably from the f:[username] argument
     * @param oldName The old pet name
     * @param newName The new pet name
     * @return False if can't find a pet by that name or can't rename the pet, true if successful.
     */
    private boolean renamePet(Player pl, OfflinePlayer commandAbout, String oldName, String newName) {
        if (!ArgValidator.softValidatePetName(oldName)) {
            pl.sendMessage(ChatColor.RED + "Invalid pet name: " + ChatColor.WHITE + oldName);
            return false;
        }
        String petUUIDByName = thisPlugin.getDatabase().getPetUUIDByName(commandAbout.getUniqueId().toString(), oldName);
        if (petUUIDByName.equals("") || petUUIDByName == null) {
            pl.sendMessage(ChatColor.RED + "Can't find pet named " + ChatColor.WHITE + oldName);
            return false;
        }
        if (!ArgValidator.softValidatePetName(newName)) {
            pl.sendMessage(ChatColor.RED + "Invalid pet name: " + ChatColor.WHITE + newName);
            return false;
        }
        if (!ArgValidator.validatePetName(thisPlugin.getDatabase(), commandAbout.getUniqueId().toString(), newName)) {
            pl.sendMessage(ChatColor.RED + "Pet name " + ChatColor.WHITE + newName + " already in use!");
            return false;
        }
        if (thisPlugin.getDatabase().renamePet(commandAbout.getUniqueId().toString(), oldName, newName)) {
            pl.sendMessage(ChatColor.BLUE + "Renamed pet " + ChatColor.WHITE + oldName + ChatColor.BLUE + " to " + ChatColor.WHITE + newName);
            return true;
        }
        pl.sendMessage(ChatColor.RED + "Unable to rename pet");
        return false;
    }
}
