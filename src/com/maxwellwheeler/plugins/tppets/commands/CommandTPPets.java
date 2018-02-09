package com.maxwellwheeler.plugins.tppets.commands;

import java.util.ArrayList;
import java.util.List;

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
import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLite;

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
            ProtectedRegion tempProtected = thisPlugin.inProtectedRegion(tempPlayer);
            if (tempProtected == null || tempPlayer.hasPermission("tppets.tpanywhere")) {
                getPetsToTeleport(pt, tempPlayer);
            } else {
                tempPlayer.sendMessage(tempProtected.getEnterMessage());
            }
        }
    }

    private void getPetsToTeleport(PetType.Pets pt, Player pl) {
        // TODO: Config setting disabling cross-world teleporting
        List<World> worldsList = Bukkit.getServer().getWorlds();
        for (World world : worldsList) {
            ArrayList<PetStorage> unloadedPetsInWorld = dbc.getPetsGeneric(pt, world.getName(), pl.getUniqueId().toString());
            for (PetStorage pet : unloadedPetsInWorld) {
                Chunk tempLoadedChunk = getChunkFromCoords(world, pet.petX, pet.petZ);
                tempLoadedChunk.load();
            }
            for (Entity entity : world.getEntitiesByClasses(PetType.getClassTranslate(pt))) {
                if (isTeleportablePet(pt, entity, pl)) {
                    teleportPet(pl, entity);
                }
            }
        }
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
}