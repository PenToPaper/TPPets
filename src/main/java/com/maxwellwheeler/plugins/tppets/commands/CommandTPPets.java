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
class CommandTPPets {
    private TPPets thisPlugin;
    private DBWrapper dbConn;
    
    /**
     * Grabs plugin instance and database instance from Bukkit
     */

    public CommandTPPets(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
        this.dbConn = this.thisPlugin.getDatabase();
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
            switch (args[0]) {
                case "list":
                    if (commandSender.equals(commandAbout) || commandSender.hasPermission("tppets.teleportother")) {
                        listPets(commandSender, commandAbout, pt);
                    } else {
                        commandSender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                    }
                    break;
                case "all":
                    if (canTpThere(commandSender)) {
                        if ((commandSender.equals(commandAbout) || commandSender.hasPermission("tppets.teleportother"))) {
                            int numPetsTeleported = getPetsAndTeleport(commandSender, commandAbout, pt).size();
                            thisPlugin.getLogger().info("Player " + commandSender.getName() + " teleported " + Integer.toString(numPetsTeleported) + " of " + commandAbout.getName() + "'s " + pt.toString() + "s to their location at: " + formatLocation(commandSender.getLocation()));
                            commandSender.sendMessage((commandSender.equals(commandAbout) ? ChatColor.BLUE + "Your " : ChatColor.WHITE + commandAbout.getName() + "'s ") + ChatColor.WHITE + pt.toString() + "s " + ChatColor.BLUE + "have been teleported to you");
                        } else {
                            commandSender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                        }
                    }
                    break;
                // Default is assumed that args[0] = dog name
                default:
                    if (canTpThere(commandSender)) {
                        String petUUID = thisPlugin.getDatabase().getPetUUIDByName(commandAbout.getUniqueId().toString(), args[0]);
                        if (petUUID == null || petUUID.equals("") || !ArgValidator.softValidatePetName(args[0])) {
                            commandSender.sendMessage(ChatColor.RED + "Can't find pet with name " + ChatColor.WHITE + args[0]);
                            return;
                        }
                        if (!hasPermissionToTp(commandSender, commandAbout, petUUID)) {
                            commandSender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                            return;
                        }
                        if (ArgValidator.softValidatePetName(args[0]) && teleportSpecificPet(commandSender, commandAbout, args[0], pt)){
                            thisPlugin.getLogger().info("Player " + commandSender.getName() + " teleported " + commandAbout.getName() + "'s pet named " + args[0] + " to their location at: " + formatLocation(commandSender.getLocation()));
                            commandSender.sendMessage((commandSender.equals(commandAbout) ? ChatColor.BLUE + "Your pet " : ChatColor.WHITE + commandAbout.getName() + "'s " + ChatColor.BLUE + "pet ") + ChatColor.WHITE + args[0] + ChatColor.BLUE + " has been teleported to you");
                        } else {
                            commandSender.sendMessage(ChatColor.RED + "Can't find pet with name " + ChatColor.WHITE + args[0]);
                        }
                    }
                    break;
            }
        } else {
            commandSender.sendMessage(ChatColor.RED + "Syntax error! /tpp dogs [all/list/dog name]");
        }
    }

    /**
     * Checks if pl has permission to teleport petOwner's pet named petName. This works for both the tppets.teleportother permission, and /tpp allow [username] [pet name] allow. Used only when f:[username] syntax is used
     * @param pl The player that ran the command
     * @param petOwner The owner of the pet, could potentially be the same as the player
     * @param petUUID The UUID of the pet
     * @return True if successful, false if not
     */
    private boolean hasPermissionToTp(Player pl, OfflinePlayer petOwner, String petUUID) {
        return petUUID != null && !petUUID.equals("") && (pl.equals(petOwner) || pl.hasPermission("tppets.teleportother") || thisPlugin.isAllowedToPet(petUUID, pl.getUniqueId().toString()));
    }

    /**
     * Loads the chunks specified by the locations in psList
     * @param psList The {@link PetStorage} list of pets in unloaded chunks
     */
    private void loadApplicableChunks(List<PetStorage> psList) {
        for (PetStorage ps : psList) {
            World world = Bukkit.getWorld(ps.petWorld);
            if (world != null) {
                Chunk tempLoadedChunk = getChunkFromCoords(world, ps.petX, ps.petZ);
                tempLoadedChunk.load();
            }
        }
    }

    /**
     * Teleports a particular pet name from a particular owner of a particular pet type
     * @param sendTo The player to send the pet to
     * @param sendFrom The player who owns the pet
     * @param name The name of the pet
     * @param pt The type of the pet
     * @return True if successful, false if not
     */
    private boolean teleportSpecificPet(Player sendTo, OfflinePlayer sendFrom, String name, PetType.Pets pt) {
        if (dbConn != null && sendFrom != null && name != null) {
            List<PetStorage> psList = dbConn.getPetsFromOwnerNamePetType(sendFrom.getUniqueId().toString(), name, pt);
            if (psList.size() < 1) {
                return false;
            }
            loadApplicableChunks(psList);
            // If you can teleport between worlds, check every world
            if (thisPlugin.getAllowTpBetweenWorlds()) {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity ent : world.getEntitiesByClasses(PetType.getClassTranslate(pt))) {
                        for (PetStorage ps : psList) {
                            if (UUIDUtils.trimUUID(ent.getUniqueId()).equals(ps.petId)) {
                                teleportPet(sendTo, ent, !sendTo.equals(sendFrom) && sendTo.hasPermission("tppets.teleportother"));
                                return true;
                            }
                        }
                    }
                }
            } else {
                // If you can't teleport between worlds, check only the world the player is in.
                for (Entity ent : sendTo.getWorld().getEntitiesByClasses(PetType.getClassTranslate(pt))) {
                    for (PetStorage ps : psList) {
                        if (UUIDUtils.trimUUID(ent.getUniqueId()).equals(ps.petId)) {
                            teleportPet(sendTo, ent, !sendTo.equals(sendFrom) && sendTo.hasPermission("tppets.teleportother"));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Teleports all pets owned by sendFrom of type pt to sendTo
     * @param sendTo The player to send the pets to
     * @param sendFrom The player who owns the pets
     * @param pt The type of pets to teleport
     * @return A set of UUIDs of the pets that have been teleported
     */
    private Set<UUID> getPetsAndTeleport(Player sendTo, OfflinePlayer sendFrom, PetType.Pets pt) {
        List<World> worlds = Bukkit.getServer().getWorlds();
        Set<UUID> teleportedEnts = new HashSet<>();
        // If youc an teleport between worlds, check every world
        if (thisPlugin.getAllowTpBetweenWorlds()) {
            for (World world : worlds) {
                Set<UUID> teleportedTemp = loadAndTp(sendTo, sendFrom, world, pt, teleportedEnts);
                if (teleportedTemp != null) {
                    teleportedEnts.addAll(teleportedTemp);
                }
            }
        } else {
            // If you can't teleport between worlds, check only the world the player is in
            Set<UUID> teleportedTemp = loadAndTp(sendTo, sendFrom, sendTo.getWorld(), pt, teleportedEnts);
            if (teleportedTemp != null) {
                teleportedEnts.addAll(teleportedTemp);
            }
        }
        return teleportedEnts;
    }

    /**
     * Teleports all pets owned by sendFrom of type pt in world world not already in set alreadyTeleportedPets to sendTo
     * @param sendTo The player to send the pets to
     * @param sendFrom The player who owns the pets
     * @param world The world to check
     * @param pt The type of pets to teleport
     * @param alreadyTeleportedPets A set representing pets that have already been teleported, so that duplicates can't exist
     * @return alreadyTeleportedPets plus the new pets teleported by this command
     */
    private Set<UUID> loadAndTp(Player sendTo, OfflinePlayer sendFrom, World world, PetType.Pets pt, Set<UUID> alreadyTeleportedPets) {
        if (dbConn != null && sendFrom != null && sendTo != null && world != null) {
            List<PetStorage> unloadedPetsInWorld = dbConn.getPetsGeneric(sendFrom.getUniqueId().toString(), world.getName(), pt);
            loadApplicableChunks(unloadedPetsInWorld);
            for (Entity ent : world.getEntitiesByClasses(PetType.getClassTranslate(pt))) {
                if (isTeleportablePet(sendFrom, ent, pt)) {
                    if (!alreadyTeleportedPets.contains(ent.getUniqueId())) {
                        teleportPet(sendTo, ent, !sendTo.equals(sendFrom) && sendTo.hasPermission("tppets.teleportother"));
                        alreadyTeleportedPets.add(ent.getUniqueId());
                    } else {
                        ent.remove();
                    }
                }
            }
            return alreadyTeleportedPets;
        }
        return null;
    }

    /**
     * Checks if the pet should be teleported. Used to verify it's of the right type and is owned by the right player
     * @param sendFrom The pet's owner
     * @param ent The entity to check
     * @param pt The pet type expected
     * @return True if the pet should be teleported. False otherwise.
     */
    private boolean isTeleportablePet(OfflinePlayer sendFrom, Entity ent, PetType.Pets pt) {
        if (ent instanceof Tameable) {
            Tameable tameableTemp = (Tameable) ent;
            return tameableTemp.isTamed() && tameableTemp.getOwner() != null && tameableTemp.getOwner().equals(sendFrom) && PetType.getEnumByEntity(ent).equals(pt);
        }
        return false;
    }

    
    /**
     * Gets the chunk using normal x and z coordinates, rather than the chunkwide x and z coordinates
     * @param world The world where the chunk is.
     * @param x The normal x coordinate where the chunk is.
     * @param z The normal z coordinate where the chunk is.
     * @return The found chunk
     */
    private Chunk getChunkFromCoords(World world, int x, int z) {
        return new Location(world, x, 64, z).getChunk();
    }
    
    /**
     * Teleports the pet to the player
     * @param pl The player the pet is to be teleported to.
     * @param entity The entity to be teleported
     * @param keepSitting Whether or not the entity should be forced to sit
     */
    private void teleportPet(Player pl, Entity entity, boolean keepSitting) {
        EntityActions.setStanding(entity);
        entity.teleport(pl);
        if (keepSitting) {
            EntityActions.setSitting(entity);
        }
    }
    
    /**
     * Formats the location in a readable way
     * @param lc Location to format
     * @return The readable string of location data
     */
    private String formatLocation(Location lc) {
        return "x: " + Integer.toString(lc.getBlockX()) + ", " + "y: " + Integer.toString(lc.getBlockY()) + ", " + "z: " + Integer.toString(lc.getBlockZ());
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

    /**
     * Checks if a player can teleport pets there at all. Checks tppets.tpanywhere permissions and if they are in a ProtectedRegion. Sends that ProtectedRegion's EnterMessage when it's determined that the player can't tp there.
     * @param pl The player to check permissions of. The location of the player is checked
     * @return True if they're allowed to, false if they're not allowed to and have been sent a message
     */
    private boolean canTpThere(Player pl) {
        ProtectedRegion tempPr = thisPlugin.getProtectedRegionWithin(pl.getLocation());
        boolean ret = pl.hasPermission("tppets.tpanywhere") || tempPr == null;
        if (!ret) {
            pl.sendMessage(tempPr.getEnterMessage());
        }
        return ret;
    }
}