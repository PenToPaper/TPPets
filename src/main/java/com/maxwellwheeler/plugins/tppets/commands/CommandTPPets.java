package com.maxwellwheeler.plugins.tppets.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
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
    private String ownerName;
    private OfflinePlayer ownerOfflinePlayer;
    
    /**
     * Grabs plugin instance and database instance from Bukkit
     */
    public CommandTPPets() {
        this.thisPlugin = (TPPets)(Bukkit.getServer().getPluginManager().getPlugin("TPPets"));
        this.dbConn = this.thisPlugin.getDatabase();
        this.ownerName = "";
        ownerOfflinePlayer = null;
    }

    public CommandTPPets(String ownerName) {
        this();
        this.ownerName = ownerName;
        this.ownerOfflinePlayer = Bukkit.getOfflinePlayer(ownerName);
    }
    
    /**
     * Core command handling
     * @param sender Represents who sent the command. If it isn't a player, an error message is displayed.
     * @param pt The type of pet to be teleported
     */
    public void processCommand(CommandSender sender, PetType.Pets pt) {
        if (sender instanceof Player) {
            Player tempPlayer = (Player) sender;
            ProtectedRegion tempProtected = thisPlugin.getProtectedRegionWithin(tempPlayer.getLocation());
            if (tempProtected == null || tempPlayer.hasPermission("tppets.tpanywhere")) {
                String ownerTeleported = ownerName.equals("") ? tempPlayer.getName() : ownerName;
                thisPlugin.getLogger().info("Player " + tempPlayer.getName() + " teleported " + Integer.toString(getPetsAndTeleport(pt, tempPlayer).size()) + " of " + ownerTeleported + "'s " + pt.toString() + " to their location at " + formatLocation(tempPlayer.getLocation()));
                announceComplete(sender, pt);
            } else {
                tempPlayer.sendMessage(tempProtected.getEnterMessage());
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Can't teleport pets to a non-player.");
        }
    }

    /**
     * Gets a full set of entities to be teleported, and teleports them.
     * @param pt The type of pet to be teleported.
     * @param pl The player to whom the pets should be teleported to.
     * @return A set of UUIDs of the entities that are teleported.
     */
    private Set<UUID> getPetsAndTeleport(PetType.Pets pt, Player pl) {
        List<World> worldsList = Bukkit.getServer().getWorlds();
        Set<UUID> teleportedEnts = new HashSet<UUID>();
        if (thisPlugin.getAllowTpBetweenWorlds()) {
            for (World world : worldsList) {
                teleportedEnts = loadAndTp(teleportedEnts, world, pt, pl);
            }
        } else {
            teleportedEnts = loadAndTp(teleportedEnts, pl.getWorld(), pt, pl);
        }

        return teleportedEnts;
    }
    
    /**
     * Teleports owned entities that are known in unloaded chunks or in loaded chunks to player, returning a list of entities teleported.
     * @param entList A list of already teleported entities.
     * @param world The world where entities will be teleported.
     * @param pt The type of entity to be teleported.
     * @param pl The player to teleport the entities to, also used to check if the player owns the entities.
     * @return The entList set with new entities that have been added
     */
    private Set<UUID> loadAndTp(Set<UUID> entList, World world, PetType.Pets pt, Player pl) {
        List<PetStorage> unloadedPetsInWorld = new ArrayList<PetStorage>();
        if (dbConn != null && ownerName.equals("")) {
            unloadedPetsInWorld = dbConn.getPetsGeneric(pl.getUniqueId().toString(), world.getName(), pt);
        } else if (ownerOfflinePlayer != null) {
            unloadedPetsInWorld = dbConn.getPetsGeneric(ownerOfflinePlayer.getUniqueId().toString(), world.getName(), pt);
        }
        for (PetStorage pet : unloadedPetsInWorld) {
            Chunk tempLoadedChunk = getChunkFromCoords(world, pet.petX, pet.petZ);
            tempLoadedChunk.load();
        }
        for (Entity entity : world.getEntitiesByClasses(PetType.getClassTranslate(pt))) {
            if (isTeleportablePet(pt, entity, pl)) {
                if (!entList.contains(entity.getUniqueId())) {
                    teleportPet(pl, entity);
                    entList.add(entity.getUniqueId());
                } else {
                    entity.remove();
                }
            }
        }
        return entList;
    }
    
    /**
     * Checks if a pet is of type pt, and is owned by pl.
     * @param pt The type of pet expected.
     * @param pet The entity to be checked.
     * @param pl The player that might own the entity.
     * @return If the player owns the entity and it is of the expected type
     */
    private boolean isTeleportablePet(PetType.Pets pt, Entity pet, Player pl) {
        if (pet instanceof Tameable) {
            Tameable tameableTemp = (Tameable) pet;
            if ((ownerName.equals("") && tameableTemp.isTamed() && pl.equals(tameableTemp.getOwner())) || (tameableTemp.isTamed() && ownerName.equals(tameableTemp.getOwner().getName()))) {
                switch (pt) {
                    case CAT:
                        return pet instanceof Ocelot;
                    case DOG:
                        return pet instanceof Wolf;
                    case PARROT:
                        return pet instanceof Parrot;
                    default:
                        return false;
                }
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
        if (entity instanceof Sittable) {
            Sittable tempSittable = (Sittable) entity;
            tempSittable.setSitting(false);
        }
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
}