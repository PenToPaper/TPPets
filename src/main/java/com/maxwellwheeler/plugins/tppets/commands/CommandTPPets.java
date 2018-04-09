package com.maxwellwheeler.plugins.tppets.commands;

import java.util.*;

import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;

/**
 * Processes commands that actually teleport pets around
 * @author GatheringExp
 *
 */
class CommandTPPets extends TeleportCommand {
    /**
     * General constructor, takes {@link TPPets} instance
     * @param thisPlugin The TPPets instance
     */
    public CommandTPPets(TPPets thisPlugin) {
        super(thisPlugin);
    }

    /**
     * The core command handler for /tpp [pet type] [args] commands
     * Syntax: /tpp [pet type] all
     * Teleports all of sender's [pet type] to them
     *
     * Alternative Syntax: /tpp [pet type] list
     * Lists all of sender's [pet type] names
     *
     * Alternative Syntax: /tpp [pet type] [pet name]
     * Teleports sender's [pet type] named [pet name] to them
     *
     * Alternative Syntax: /tpp [pet type] f:[username] all
     * Teleports all of [username]'s [pet type] to sender
     *
     * Alternative Syntax: /tpp [pet type] f:[username] list
     * Lists all of [username]'s [pet type] to sender
     *
     * Alternative Syntax: /tpp [pet type] f:[username] [pet name]
     * Teleports [username]'s [pet type] named [pet name] to sender. This works if others are allowed to the pet as well
     * @param sender The original sender of the command
     * @param args Truncated arguments for the command, does not include /tpp dogs, the [pet type] argument is specified in PetType.Pets form
     * @param pt The pet type to teleport
     */
    public void processCommand(CommandSender sender, String[] args, PetType.Pets pt) {
        if (!(sender instanceof Player)) {
            return;
        }
        Player senderPlayer = (Player) sender;
        if (ArgValidator.validateArgsLength(args, 2)) {
            String isForSomeoneElse = ArgValidator.isForSomeoneElse(args[0]);
            if (isForSomeoneElse != null && ArgValidator.validateUsername(isForSomeoneElse)) {
                OfflinePlayer commandFor = Bukkit.getOfflinePlayer(isForSomeoneElse);
                if (commandFor != null && commandFor.hasPlayedBefore()) {
                    processCommandGeneric(senderPlayer, commandFor, Arrays.copyOfRange(args, 1, args.length), pt);
                    return;
                }
            }
            senderPlayer.sendMessage(ChatColor.RED + "Can't find player " + ChatColor.WHITE + args[0]);
        } else {
            processCommandGeneric(senderPlayer, senderPlayer, args, pt);
        }
    }

    /**
     * The generic command handler for /tpp [pet type] [args] commands. This takes {@link Player} and {@link OfflinePlayer} objects instead of raw strings, so it's used for translating f:[username]-type commands to the actual action
     * @param commandSender The player that sent the command
     * @param commandAbout The player that the command is about. This can be .equals(commandSender), because Player extends OfflinePlayer
     * @param args For a command /tpp dogs f:[username] list, args = [list].
     *             For a command /tpp dogs list, args = [list].
     * @param pt The type of pet the command is about.
     */
    private void processCommandGeneric(Player commandSender, OfflinePlayer commandAbout, String[] args, PetType.Pets pt) {
        if (ArgValidator.validateArgsLength(args, 1)) {
            switch (args[0].toLowerCase()) {
                case "list":
                    if (commandSender.equals(commandAbout) || commandSender.hasPermission("tppets.teleportother")) {
                        listPets(commandSender, commandAbout, pt);
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                    }
                    break;
                case "all":
                    if (thisPlugin.canTpThere(commandSender)) {
                        if ((commandSender.equals(commandAbout) || commandSender.hasPermission("tppets.teleportother"))) {
                            int numPetsTeleported = getPetsAndTeleport(commandSender.getLocation(), commandAbout, pt, shouldKeepSitting(commandSender, commandAbout)).size();
                            thisPlugin.getLogger().info("Player " + commandSender.getName() + " teleported " + Integer.toString(numPetsTeleported) + " of " + commandAbout.getName() + "'s " + pt.toString() + "s to their location at: " + formatLocation(commandSender.getLocation()));
                            commandSender.sendMessage((commandSender.equals(commandAbout) ? ChatColor.BLUE + "Your " : ChatColor.WHITE + commandAbout.getName() + "'s ") + ChatColor.WHITE + pt.toString() + "s " + ChatColor.BLUE + "have been teleported to you");
                        } else {
                            commandSender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                        }
                    }
                    break;
                // Default is assumed that args[0] = dog name
                default:
                    if (thisPlugin.canTpThere(commandSender)) {
                        PetStorage pet = thisPlugin.getDatabase().getPetByName(commandAbout.getUniqueId().toString(), args[0]);
                        if (pet == null || !ArgValidator.softValidatePetName(args[0])) {
                            commandSender.sendMessage(ChatColor.RED + "Can't find pet with name " + ChatColor.WHITE + args[0]);
                            return;
                        }
                        if (!hasPermissionToTp(commandSender, commandAbout, pet.petId)) {
                            commandSender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                            return;
                        }
                        if (ArgValidator.softValidatePetName(args[0]) && teleportSpecificPet(commandSender.getLocation(), commandAbout, args[0], pt, shouldKeepSitting(commandSender, commandAbout), true)){
                            thisPlugin.getLogger().info("Player " + commandSender.getName() + " teleported " + commandAbout.getName() + "'s pet named " + args[0] + " to their location at: " + formatLocation(commandSender.getLocation()));
                            commandSender.sendMessage((commandSender.equals(commandAbout) ? ChatColor.BLUE + "Your pet " : ChatColor.WHITE + commandAbout.getName() + "'s " + ChatColor.BLUE + "pet ") + ChatColor.WHITE + args[0] + ChatColor.BLUE + " has been teleported to you");
                        } else {
                            commandSender.sendMessage(ChatColor.RED + "Can't find pet with name " + ChatColor.WHITE + args[0]);
                        }
                    }
                    break;
            }
        } else {
            commandSender.sendMessage(ChatColor.RED + "Syntax error! /tpp [pet type] [all/list/dog name]");
        }
    }

    /**
     * Lists the pets owned by petsFrom to pl's chat
     * @param pl The player to report the list to
     * @param petsFrom The player who owns the pets
     * @param pt The type of pets to check for
     */
    private void listPets(Player pl, OfflinePlayer petsFrom, PetType.Pets pt) {
        pl.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + petsFrom.getName() + "'s " + ChatColor.BLUE + pt.toString() + " names ]" + ChatColor.DARK_GRAY + "---------");
        int i = 0;
        // Loop through all the worlds, pulling pets from the database
        for (World wld : Bukkit.getServer().getWorlds()) {
            List<PetStorage> tempPs = thisPlugin.getDatabase().getPetsGeneric(petsFrom.getUniqueId().toString(), wld.getName(), pt);
            while (i < tempPs.size()) {
                pl.sendMessage(ChatColor.WHITE + Integer.toString(i+1) + ") " + tempPs.get(i).petName);
                i++;
            }
        }
        pl.sendMessage(ChatColor.DARK_GRAY + "----------------------------------");
    }

    @Override
    protected boolean hasPermissionToTp(Player pl, OfflinePlayer petOwner, String petUUID) {
        return petUUID != null && !petUUID.equals("") && (pl.equals(petOwner) || pl.hasPermission("tppets.teleportother") || thisPlugin.isAllowedToPet(petUUID, pl.getUniqueId().toString()));
    }
}