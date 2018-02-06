package com.maxwellwheeler.plugins.tppets;

import java.util.ArrayList;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.maxwellwheeler.plugins.tppets.commands.CommandLF;
import com.maxwellwheeler.plugins.tppets.commands.CommandNoPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPPets;
import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.SQLite;

public class TPPets extends JavaPlugin implements Listener {
    private ArrayList<ProtectedRegion> protectedRegions = new ArrayList<ProtectedRegion>();
    private static TPPets instance;
    private SQLite dbc;
    
    public static TPPets getInstance() {
        return instance;
    }
    
    private void getProtectedRegions() {
        Set<String> regionKeyList = getConfig().getConfigurationSection("forbidden_zones").getKeys(false);
        System.out.println(regionKeyList.toString());
        for (String key : regionKeyList) {
            protectedRegions.add(new ProtectedRegion(key, this));
        }
        for (ProtectedRegion pr : protectedRegions) {
            System.out.println(pr.toString());
        }
        System.out.println(protectedRegions.size());
    }
    
    @Override
    public void onEnable() {
        // Public static property instance now refers to the server's instance of the plugin
        instance = this;
        
        // Config stuff
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        getProtectedRegions();
        
        // Database setup
        dbc = new SQLite(this, getDataFolder().getPath(), "test");
        dbc.createDatabase();
        dbc.createTable();
        
        // Register events + commands
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("tp-dogs").setExecutor(new CommandTPPets(dbc));
        this.getCommand("tp-cats").setExecutor(new CommandTPPets(dbc));
        this.getCommand("tp-parrots").setExecutor(new CommandTPPets(dbc));
        this.getCommand("no-pets").setExecutor(new CommandNoPets());
        // this.getCommand("pets-lf").setExecutor(new CommandLF());
    }
    
    @EventHandler (priority=EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent e) {
        for (Entity ent : e.getChunk().getEntities()) {
           if (ent instanceof Ocelot || ent instanceof Wolf || ent instanceof Parrot) { 
               dbc.insertPet(ent);
           }
        }
    }
    
    @EventHandler (priority=EventPriority.LOW)
    public void onEntityTeleportEvent(EntityTeleportEvent e) {
        if (isInProtectedRegion(e.getTo())) {
            if (e.getEntity() instanceof Sittable) {
                Sittable ent = (Sittable) e.getEntity();
                ent.setSitting(true);
            }
            e.setCancelled(true);
        }
    }
    
    public void addProtectedRegion (ProtectedRegion pr) {
        protectedRegions.add(pr);
    }
    
    public boolean isInProtectedRegion(Player pl) {
        for (ProtectedRegion pr : protectedRegions) {
            if (pr.isInZone(pl)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isInProtectedRegion(Location lc) {
        for (ProtectedRegion pr : protectedRegions) {
            if (pr.isInZone(lc)) {
                return true;
            }
        }
        return false;
    }
}
