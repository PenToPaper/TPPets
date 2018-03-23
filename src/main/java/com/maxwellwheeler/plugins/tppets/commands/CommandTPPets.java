package com.maxwellwheeler.plugins.tppets.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.EntityActions;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
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
public class CommandTPPets {
    private TPPets thisPlugin;
    private DBWrapper dbConn;
    private String petName;
    private String ownerName;
    private OfflinePlayer ownerOfflinePlayer;
    
    /**
     * Grabs plugin instance and database instance from Bukkit
     */
    public CommandTPPets() {
        this.thisPlugin = (TPPets)(Bukkit.getServer().getPluginManager().getPlugin("TPPets"));
        this.dbConn = this.thisPlugin.getDatabase();
        this.ownerName = null;
        this.petName = null;
        ownerOfflinePlayer = null;
    }

    // Desired syntax: /tpp dog  <--- Teleports all of command senders' dogs to them
    // Desired syntax: /tpp dog list  <--- Lists all owned dogs in chat
    // Desired syntax: /tpp dog DogName  <--- Teleports command sender's dog with that name to them
    // Desired syntax: /tpp dog f:OwnerName  <--- Teleports all of OwnerName's dogs to command sender
    // Desired syntax: /tpp dog f:OwnerName DogName  <--- Teleports OwnerName's dog with DogName to command sender
    // Desired syntax: /tpp dog f:OwnerName list  <--- Lists all dogs owned by OwnerName
    /**
     * Core command handling
     * @param sender Represents who sent the command. If it isn't a player, an error message is displayed.
     * @param pt The type of pet to be teleported
     */
    public void processCommand(CommandSender sender, String[] args, PetType.Pets pt) {
        if (!(sender instanceof Player)) {
            return;
        }
        Player tempPlayer = (Player) sender;
        if (ArgValidator.validateArgs(args, 1)) {
            this.ownerName =  ArgValidator.isForSomeoneElse(args[0]);
            if (this.ownerName != null) {
                ownerOfflinePlayer = Bukkit.getOfflinePlayer(this.ownerName);
                if (ownerOfflinePlayer != null) {
                    if (ArgValidator.validateArgs(args, 2)) {
                        if (args[1].equals("list")) {
                            // Syntax received: /tpp dog f:OwnerName list
                            listPets(tempPlayer, ownerOfflinePlayer, pt);
                        } else {
                            // Syntax received: /tpp dog f:OwnerName DogName
                            if (teleportSpecificPet(tempPlayer, ownerOfflinePlayer, args[1], pt)) {
                                thisPlugin.getLogger().info(ChatColor.BLUE + "Player " + tempPlayer.getName() + " teleported " + args[1] + ", " + ownerOfflinePlayer.getName() + "'s pet, to them.");
                                tempPlayer.sendMessage(ChatColor.BLUE + ownerOfflinePlayer.getName() + "'s pet " + args[1] + " has been teleported to you.");
                            } else {
                                tempPlayer.sendMessage(ChatColor.RED + "Can't find pet with name: " + ChatColor.WHITE + args[1]);
                            }
                        }
                    } else {
                        // Syntax received: /tpp dog f:OwnerName
                        int numPetsTeleported = getPetsAndTeleport(tempPlayer, ownerOfflinePlayer, pt).size();
                        thisPlugin.getLogger().info("Player " + tempPlayer.getName() + " teleported " + Integer.toString(numPetsTeleported) + " of " + args[1] + "'s " + pt.toString() + "s to their location.");
                    }
                }
            } else {
                if (args[0].equals("list")) {
                    // Syntax received: /tpp dog list
                    listPets(tempPlayer, tempPlayer, pt);
                } else {
                    // Syntax received: /tpp dog DogName
                    if (teleportSpecificPet(tempPlayer, tempPlayer, args[0], pt)) {
                        thisPlugin.getLogger().info("Player " + tempPlayer.getName() + " teleported their " + pt.toString() + ", " + args[0] + ", to them");
                        tempPlayer.sendMessage(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + args[0] + ChatColor.BLUE + " has been teleported to you.");
                    } else {
                        tempPlayer.sendMessage(ChatColor.RED + "Can't find pet with name: " + ChatColor.WHITE + args[0]);
                    }
                }
            }
        } else {
            // Syntax received: /tpp dog
            int numPetsTeleported = getPetsAndTeleport(tempPlayer, tempPlayer, pt).size();
            thisPlugin.getLogger().info("Player " + tempPlayer.getName() + " teleported " + Integer.toString(numPetsTeleported) + " of their " + pt.toString() + "s to their location.");
            tempPlayer.sendMessage(ChatColor.BLUE + "Your " + ChatColor.WHITE + pt.toString() + "s" + ChatColor.BLUE + " have been teleported to you.");
        }
    }

    private void loadApplicableChunks(List<PetStorage> psList) {
        for (PetStorage ps : psList) {
            World world = Bukkit.getWorld(ps.petWorld);
            if (world != null) {
                Chunk tempLoadedChunk = getChunkFromCoords(world, ps.petX, ps.petZ);
                tempLoadedChunk.load();
            }
        }
    }

    private boolean teleportSpecificPet(Player sendTo, OfflinePlayer sendFrom, String name, PetType.Pets pt) {
        if (dbConn != null && sendFrom != null && name != null) {
            List<PetStorage> psList = dbConn.getPetsFromOwnerNamePetType(sendFrom.getUniqueId().toString(), name, pt);
            loadApplicableChunks(psList);
            for (World world : Bukkit.getWorlds()) {
                for (Entity ent : world.getEntitiesByClasses(PetType.getClassTranslate(pt))) {
                    for (PetStorage ps : psList) {
                        if (UUIDUtils.trimUUID(ent.getUniqueId()).equals(ps.petId)) {
                            teleportPet(sendTo, ent);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Set<UUID> getPetsAndTeleport(Player sendTo, OfflinePlayer sendFrom, PetType.Pets pt) {
        List<World> worlds = Bukkit.getServer().getWorlds();
        Set<UUID> teleportedEnts = new HashSet<UUID>();
        if (thisPlugin.getAllowTpBetweenWorlds()) {
            for (World world : worlds) {
                teleportedEnts.addAll(loadAndTp(sendTo, sendFrom, world, pt, teleportedEnts));
            }
        } else {
            teleportedEnts.addAll(loadAndTp(sendTo, sendFrom, sendTo.getWorld(), pt, teleportedEnts));
        }
        return teleportedEnts;
    }

    private Set<UUID> loadAndTp(Player sendTo, OfflinePlayer sendFrom, World world, PetType.Pets pt, Set<UUID> alreadyTeleportedPets) {
        if (dbConn != null && sendFrom != null && sendTo != null && world != null) {
            List<PetStorage> unloadedPetsInWorld = dbConn.getPetsGeneric(sendFrom.getUniqueId().toString(), world.getName(), pt);
            loadApplicableChunks(unloadedPetsInWorld);
            for (Entity ent : world.getEntitiesByClasses(PetType.getClassTranslate(pt))) {
                if (isTeleportablePet(sendTo, sendFrom, ent, pt)) {
                    if (!alreadyTeleportedPets.contains(ent.getUniqueId())) {
                        teleportPet(sendTo, ent);
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

    private boolean isTeleportablePet(Player sendTo, OfflinePlayer sendFrom, Entity ent, PetType.Pets pt) {
        if (ent instanceof Tameable) {
            Tameable tameableTemp = (Tameable) ent;
            if (tameableTemp.isTamed() && tameableTemp.getOwner() != null && tameableTemp.getOwner().equals(sendFrom) && PetType.getEnumByEntity(ent).equals(pt)) {
                return true;
            }
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
     */
    private void teleportPet(Player pl, Entity entity) {
        EntityActions.setSitting(entity);
        entity.teleport(pl);
    }
    
    /**
     * Sends a message to the player after their pets have been teleported
     * @param sender The sender to send a confirmation for
     * @param pt The pet type that was teleported
     */
    private void announceComplete(CommandSender sender, PetType.Pets pt) {
        String firstWord = ownerName.equals("") ? "Your " : ownerName + "'s ";
        switch (pt) {
            case CAT:
                sender.sendMessage(ChatColor.BLUE + firstWord + ChatColor.WHITE + "cats " + ChatColor.BLUE + "have been teleported to you.");
                break;
            case DOG:
                sender.sendMessage(ChatColor.BLUE + firstWord + ChatColor.WHITE + "dogs " + ChatColor.BLUE + "have been teleported to you.");
                break;
            case PARROT:
                sender.sendMessage(ChatColor.BLUE + firstWord + ChatColor.WHITE + "birds " + ChatColor.BLUE + "have been teleported to you.");
                break;
            default:
                break;
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

    private void listPets(Player pl, OfflinePlayer dogOwner, PetType.Pets pt) {
        pl.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[Your " + pt.toString() + " names]" + ChatColor.DARK_GRAY + "---------");
        for (World wld : Bukkit.getServer().getWorlds()) {
            List<PetStorage> tempPs = thisPlugin.getDatabase().getPetsGeneric(pl.getUniqueId().toString(), wld.getName(), pt);
            for (int i = 0; i < tempPs.size(); i++) {
                pl.sendMessage(ChatColor.WHITE + Integer.toString(i+1) + ") " + tempPs.get(i).petName);
            }
        }
        pl.sendMessage(ChatColor.DARK_GRAY + "----------------------------------");
    }
}