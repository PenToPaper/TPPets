package com.maxwellwheeler.plugins.tppets.commands;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private String petName;
    private String ownerName;
    private OfflinePlayer ownerOfflinePlayer;
    
    /**
     * Grabs plugin instance and database instance from Bukkit
     */

    // TODO RETHINK THESE VALUES
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
    @SuppressWarnings("deprecation")
    public void processCommand(CommandSender sender, String[] args, PetType.Pets pt) {
        if (!(sender instanceof Player)) {
            return;
        }
        Player tempPlayer = (Player) sender;
        if (ArgValidator.validateArgsLength(args, 1)) {
            this.ownerName =  ArgValidator.isForSomeoneElse(args[0]);
            if (this.ownerName != null) {
                ownerOfflinePlayer = Bukkit.getOfflinePlayer(this.ownerName);
                if (ownerOfflinePlayer != null) {
                    if (ArgValidator.validateArgsLength(args, 2)) {
                        if (args[1].equals("list")) {
                            // Syntax received: /tpp dog f:OwnerName list
                            if (tempPlayer.hasPermission("tppets.teleportother")) {
                                listPets(tempPlayer, ownerOfflinePlayer, pt);
                            } else {
                                // Player isn't allowed to teleport the pet, and shouldn't be allowed to check the player's pet names
                                tempPlayer.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                            }
                        } else {
                            // Syntax received: /tpp dog f:OwnerName DogName
                            ProtectedRegion prWithin = canTpThere(tempPlayer);
                            if (prWithin != null) {
                                tempPlayer.sendMessage(prWithin.getEnterMessage());
                                return;
                            }
                            if (ArgValidator.softValidatePetName(args[1]) && hasPermissionToTp(tempPlayer, ownerOfflinePlayer, args[1]) && teleportSpecificPet(tempPlayer, ownerOfflinePlayer, args[1], pt)) {
                                thisPlugin.getLogger().info(ChatColor.BLUE + "Player " + tempPlayer.getName() + " teleported " + args[1] + ", " + ownerOfflinePlayer.getName() + "'s pet, to them at: " + formatLocation(tempPlayer.getLocation()));
                                tempPlayer.sendMessage(ChatColor.WHITE + ownerOfflinePlayer.getName() + "'s " + ChatColor.BLUE + "pet " + ChatColor.WHITE + args[1] + ChatColor.BLUE + " has been teleported to you.");
                            } else {
                                tempPlayer.sendMessage(ChatColor.RED + "Unable to teleport pet " + ChatColor.WHITE + args[1]);
                            }
                        }
                    } else {
                        // Syntax received: /tpp dog f:OwnerName
                        ProtectedRegion prWithin = canTpThere(tempPlayer);
                        if (prWithin != null) {
                            tempPlayer.sendMessage(prWithin.getEnterMessage());
                            return;
                        }
                        if (tempPlayer.hasPermission("tppets.teleportother")) {
                            int numPetsTeleported = getPetsAndTeleport(tempPlayer, ownerOfflinePlayer, pt).size();
                            thisPlugin.getLogger().info("Player " + tempPlayer.getName() + " teleported " + Integer.toString(numPetsTeleported) + " of " + this.ownerName + "'s " + pt.toString() + "s to their location at: " + formatLocation(tempPlayer.getLocation()));
                            tempPlayer.sendMessage(ChatColor.WHITE + ownerOfflinePlayer.getName() + "'s " + pt.toString() + "s " + ChatColor.BLUE + "have been teleported to you");
                        } else {
                            tempPlayer.sendMessage(ChatColor.RED + "You don't have permission to do that!");
                        }
                    }
                }
            } else {
                if (args[0].equals("list")) {
                    // Syntax received: /tpp dog list
                    listPets(tempPlayer, tempPlayer, pt);
                } else {
                    // Syntax received: /tpp dog DogName
                    ProtectedRegion prWithin = canTpThere(tempPlayer);
                    if (prWithin != null) {
                        tempPlayer.sendMessage(prWithin.getEnterMessage());
                        return;
                    }
                    if (teleportSpecificPet(tempPlayer, tempPlayer, args[0], pt)) {
                        thisPlugin.getLogger().info("Player " + tempPlayer.getName() + " teleported their " + pt.toString() + ", " + args[0] + ", to their location at: " + formatLocation(tempPlayer.getLocation()));
                        tempPlayer.sendMessage(ChatColor.BLUE + "Your pet " + ChatColor.WHITE + args[0] + ChatColor.BLUE + " has been teleported to you.");
                    } else {
                        tempPlayer.sendMessage(ChatColor.RED + "Can't find pet with name: " + ChatColor.WHITE + args[0]);
                    }
                }
            }
        } else {
            // Syntax received: /tpp dog
            ProtectedRegion prWithin = canTpThere(tempPlayer);
            if (prWithin != null) {
                tempPlayer.sendMessage(prWithin.getEnterMessage());
                return;
            }
            int numPetsTeleported = getPetsAndTeleport(tempPlayer, tempPlayer, pt).size();
            thisPlugin.getLogger().info("Player " + tempPlayer.getName() + " teleported " + Integer.toString(numPetsTeleported) + " of their " + pt.toString() + "s to their location at: " + formatLocation(tempPlayer.getLocation()));
            tempPlayer.sendMessage(ChatColor.BLUE + "Your " + ChatColor.WHITE + pt.toString() + "s" + ChatColor.BLUE + " have been teleported to you.");
        }
    }

    private boolean hasPermissionToTp(Player pl, OfflinePlayer petOwner, String petName) {
        String petUUID = thisPlugin.getDatabase().getPetUUIDByName(petOwner.getUniqueId().toString(), petName);
        return petUUID != null && !petUUID.equals("") && (pl.hasPermission("tppets.teleportother") || thisPlugin.isAllowedToPet(petUUID, pl.getUniqueId().toString()));
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
                            teleportPet(sendTo, ent, !sendTo.equals(sendFrom) && sendTo.hasPermission("tppets.teleportother"));
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
        Set<UUID> teleportedEnts = new HashSet<>();
        if (thisPlugin.getAllowTpBetweenWorlds()) {
            for (World world : worlds) {
                Set<UUID> teleportedTemp = loadAndTp(sendTo, sendFrom, world, pt, teleportedEnts);
                if (teleportedTemp != null) {
                    teleportedEnts.addAll(teleportedTemp);
                }
            }
        } else {
            Set<UUID> teleportedTemp = loadAndTp(sendTo, sendFrom, sendTo.getWorld(), pt, teleportedEnts);
            if (teleportedTemp != null) {
                teleportedEnts.addAll(teleportedTemp);
            }
        }
        return teleportedEnts;
    }

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

    private void listPets(Player pl, OfflinePlayer petsFrom, PetType.Pets pt) {
        pl.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ " + petsFrom.getName() + "'s " + pt.toString() + " names ]" + ChatColor.DARK_GRAY + "---------");
        int i = 0;
        for (World wld : Bukkit.getServer().getWorlds()) {
            List<PetStorage> tempPs = thisPlugin.getDatabase().getPetsGeneric(petsFrom.getUniqueId().toString(), wld.getName(), pt);
            while (i < tempPs.size()) {
                pl.sendMessage(ChatColor.WHITE + Integer.toString(i+1) + ") " + tempPs.get(i).petName);
                i++;
            }
        }
        pl.sendMessage(ChatColor.DARK_GRAY + "----------------------------------");
    }

    private ProtectedRegion canTpThere(Player pl) {
        if (!pl.hasPermission("tppets.tpanywhere")) {
            return thisPlugin.getProtectedRegionWithin(pl.getLocation());
        }
        return null;
    }
}