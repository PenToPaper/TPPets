package com.maxwellwheeler.plugins.tppets.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
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
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLite;

import net.md_5.bungee.api.ChatColor;

public class CommandTPPets {
    private TPPets thisPlugin;
    private SQLite dbc;
    
    public CommandTPPets() {
        this.thisPlugin = (TPPets)(Bukkit.getServer().getPluginManager().getPlugin("TPPets"));
        this.dbc = this.thisPlugin.getSQLite();
    }
    
    public void processCommand(CommandSender sender, PetType.Pets pt) {
        if (sender instanceof Player) {
            Player tempPlayer = (Player) sender;
            ProtectedRegion tempProtected = thisPlugin.getProtectedRegionWithin(tempPlayer.getLocation());
            if (tempProtected == null || tempPlayer.hasPermission("tppets.tpanywhere")) {
                thisPlugin.getLogger().info("Player " + tempPlayer.getName() + " teleported " + Integer.toString(getPetsAndTeleport(pt, tempPlayer).size()) + " " + pt.toString() + " to their location at " + formatLocation(tempPlayer.getLocation()));
                announceComplete(sender, pt);
            } else {
                tempPlayer.sendMessage(tempProtected.getEnterMessage());
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Can't teleport pets to a non-player.");
        }
    }

    private List<UUID> getPetsAndTeleport(PetType.Pets pt, Player pl) {
        List<World> worldsList = Bukkit.getServer().getWorlds();
        List<UUID> teleportedEnts = new ArrayList<UUID>();
        if (thisPlugin.getAllowTpBetweenWorlds()) {
            for (World world : worldsList) {
                teleportedEnts.addAll(loadAndTp(teleportedEnts, world, pt, pl));
            }
        } else {
            teleportedEnts = loadAndTp(teleportedEnts, pl.getWorld(), pt, pl);
        }

        return teleportedEnts;
    }
    
    private List<UUID> loadAndTp(List<UUID> entList, World world, PetType.Pets pt, Player pl) {
        ArrayList<PetStorage> unloadedPetsInWorld = dbc.getPetsGeneric(pt, world.getName(), pl.getUniqueId().toString());
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
    
    private boolean isTeleportablePet(PetType.Pets pt, Entity pet, Player pl) {
        if (pet instanceof Tameable) {
            Tameable tameableTemp = (Tameable) pet;

            if (tameableTemp.isTamed() && pl.equals(tameableTemp.getOwner())) {
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
    
    private Chunk getChunkFromCoords(World world, int x, int z) {
        return new Location(world, x, 64, z).getChunk();
    }
    
    private void teleportPet(Player pl, Entity entity) {
        if (entity instanceof Sittable) {
            Sittable tempSittable = (Sittable) entity;
            tempSittable.setSitting(false);
        }
        entity.teleport(pl);
    }
    
    private void announceComplete(CommandSender sender, PetType.Pets pt) {
        switch (pt) {
            case CAT:
                sender.sendMessage(ChatColor.BLUE + "Your " + ChatColor.WHITE + "cats " + ChatColor.BLUE + "have been teleported to you.");
                break;
            case DOG:
                sender.sendMessage(ChatColor.BLUE + "Your " + ChatColor.WHITE + "dogs " + ChatColor.BLUE + "have been teleported to you.");
                break;
            case PARROT:
                sender.sendMessage(ChatColor.BLUE + "Your " + ChatColor.WHITE + "birds " + ChatColor.BLUE + "have been teleported to you.");
                break;
            default:
                break;
        }
    }
    
    private String formatLocation(Location lc) {
        return "x: " + Integer.toString(lc.getBlockX()) + ", " + "y: " + Integer.toString(lc.getBlockY()) + ", " + "z: " + Integer.toString(lc.getBlockZ());
    }
}