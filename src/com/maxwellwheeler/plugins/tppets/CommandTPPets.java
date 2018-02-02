package com.maxwellwheeler.plugins.tppets;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLite;

public class CommandTPPets implements CommandExecutor {
    private SQLite dbc;
    
    public CommandTPPets(SQLite dbc) {
        this.dbc = dbc;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player playerTemp = (Player) sender;
            switch (command.getName()) {
                case "tp-cats":
                    getPetsToTeleport(PetType.Pets.CAT, playerTemp);
                    return true;
                case "tp-dogs":
                    getPetsToTeleport(PetType.Pets.DOG, playerTemp);
                    return true;
                case "tp-parrots":
                    getPetsToTeleport(PetType.Pets.PARROT, playerTemp);
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }
    
    private void getPetsToTeleport(PetType.Pets pt, Player pl) {
        List<World> worldsList = Bukkit.getServer().getWorlds();
        for (World world : worldsList) {
            ArrayList<PetStorage> unloadedPetsInWorld = dbc.selectGeneric(pt, world.toString(), pl.getUniqueId().toString());
            for (PetStorage pet : unloadedPetsInWorld) {
                Chunk tempLoadedChunk = getChunkFromCoords(world, pet.petX, pet.petZ);
                tempLoadedChunk.load();
                for (Entity entity : tempLoadedChunk.getEntities()) {
                    if (entity instanceof Sittable) {
                        if (isTeleportablePet(pt, entity, pl)) {
                            teleportPet(pl, entity);
                        }
                    }
                }
                tempLoadedChunk.unload();
            }
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Sittable) {
                    if (isTeleportablePet(pt, entity, pl)) {
                        teleportPet(pl, entity);
                    }
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
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    private Chunk getChunkFromCoords(World world, int x, int y, int z) {
        return new Location(world, x, y, z).getChunk();
    }
    
    private Chunk getChunkFromCoords(World world, int x, int z) {
        return new Location(world, x, 64, z).getChunk();
    }
    
    private void teleportPet(Player pl, Entity entity) {
        if (entity instanceof Sittable) {
            Sittable tempSittable = (Sittable) entity;
            tempSittable.setSitting(false);
        }
        dbc.deleteEntry(entity.getUniqueId(), pl.getUniqueId());
        entity.teleport(pl);
    }
}