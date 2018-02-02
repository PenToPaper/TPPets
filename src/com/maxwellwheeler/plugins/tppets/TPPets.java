package com.maxwellwheeler.plugins.tppets;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.maxwellwheeler.plugins.tppets.storage.SQLite;

public class TPPets extends JavaPlugin implements Listener {
    private static TPPets instance;
    private SQLite dbc;
    
    public static TPPets getInstance() {
        return instance;
    }
    
    @Override
    public void onEnable() {
        // Public static property instance now refers to the server's instance of the plugin
        instance = this;
        
        // Database setup
        dbc = new SQLite(this, getDataFolder().getPath(), "test");
        dbc.createDatabase();
        dbc.createTable();
        
        // Register events + commands
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("tp-dogs").setExecutor(new CommandTPPets(dbc));
        this.getCommand("tp-cats").setExecutor(new CommandTPPets(dbc));
        this.getCommand("tp-parrots").setExecutor(new CommandTPPets(dbc));
    }
    
    @EventHandler (priority=EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent e) {
        for (Entity ent : e.getChunk().getEntities()) {
           if (ent instanceof Ocelot || ent instanceof Wolf || ent instanceof Parrot) { 
               dbc.insertPet(ent);
           }
        }
    }
}
