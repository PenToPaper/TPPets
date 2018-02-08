package com.maxwellwheeler.plugins.tppets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.maxwellwheeler.plugins.tppets.commands.CommandCreateDogs;
import com.maxwellwheeler.plugins.tppets.commands.CommandLF;
import com.maxwellwheeler.plugins.tppets.commands.CommandNoPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPPets;
import com.maxwellwheeler.plugins.tppets.commands.CommandTpForward;
import com.maxwellwheeler.plugins.tppets.helpers.TimeCalculator;
import com.maxwellwheeler.plugins.tppets.listeners.TPPetsChunkListener;
import com.maxwellwheeler.plugins.tppets.listeners.TPPetsEntityListener;
import com.maxwellwheeler.plugins.tppets.region.CheckRegions;
import com.maxwellwheeler.plugins.tppets.region.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.SQLite;

public class TPPets extends JavaPlugin implements Listener {
    private ArrayList<ProtectedRegion> protectedRegions = new ArrayList<ProtectedRegion>();
    private static TPPets instance;
    private SQLite dbc;
    private CheckRegions cr;
    private int checkInterval;
    private LostAndFoundRegion lostAndFound;
    
    private boolean preventPlayerDamage;
    private boolean preventEnvironmentalDamage;
    private boolean preventMobDamage;
    
    public boolean getPreventPlayerDamage() {
        return preventPlayerDamage;
    }
    
    public boolean getPreventEnvironmentalDamage() {
        return preventEnvironmentalDamage;
    }
    
    public boolean getPreventMobDamage() {
        return preventMobDamage;
    }
    
    public static TPPets getInstance() {
        return instance;
    }
    
    private void initializeDamageConfigs() {
        List<String> configList = getConfig().getStringList("protect_pets_from");
        if (configList.contains("Player")) {
            preventPlayerDamage = true;
        }
        if (configList.contains("EnvironmentalDamage")) {
            preventEnvironmentalDamage = true;
        }
        if (configList.contains("MobDamage")) {
            preventMobDamage = true;
        }
    }
    
    private void initializeProtectedRegions() {
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
    
    private void initializeLostAndFound() {
        //TODO Temporary. When we move to databases for everything, we should get a more robust solution, like the one above ^^
        if (getConfig().isSet("lost_and_found.primary")) {
            lostAndFound = new LostAndFoundRegion(this);
        }
    }

    @Override
    public void onEnable() {
        // Public static property instance now refers to the server's instance of the plugin
        instance = this;
        
        // Config stuff
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        initializeProtectedRegions();
        
        // Database setup
        dbc = new SQLite(this, getDataFolder().getPath(), "test");
        dbc.createDatabase();
        dbc.createTable();
        
        checkInterval = TimeCalculator.getTimeFromString(getConfig().getString("check_interval"));
        
        // Register events + commands
        getServer().getPluginManager().registerEvents(new TPPetsChunkListener(this), this);
        getServer().getPluginManager().registerEvents(new TPPetsEntityListener(this), this);
        this.getCommand("tp-dogs").setExecutor(new CommandTPPets(dbc));
        this.getCommand("tp-cats").setExecutor(new CommandTPPets(dbc));
        this.getCommand("tp-parrots").setExecutor(new CommandTPPets(dbc));
        this.getCommand("no-pets").setExecutor(new CommandNoPets());
        this.getCommand("pets-lf").setExecutor(new CommandLF());
        this.getCommand("generate-tamed-dogs").setExecutor(new CommandCreateDogs());
        this.getCommand("tp-forward").setExecutor(new CommandTpForward());

        startCheckingRegions();
        initializeDamageConfigs();
        initializeLostAndFound();
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
    
    public void startCheckingRegions() {
        if (lostAndFound != null) {
            cr = new CheckRegions(this, lostAndFound);
            cr.runTaskTimer(this, 0, checkInterval);
        }
    }
    
    public boolean isInProtectedRegion(Location lc) {
        for (ProtectedRegion pr : protectedRegions) {
            if (pr.isInZone(lc)) {
                return true;
            }
        }
        return false;
    }
    
    public void addLostAndFoundRegion (LostAndFoundRegion lfr) {
        if (lostAndFound == null) {
            lostAndFound = lfr;
            startCheckingRegions();
        } else {
            lostAndFound = lfr;
        }
        
    }
    
    public SQLite getSQLite() {
        return dbc;
    }
    
    public ArrayList<ProtectedRegion> getProtectedRegions() {
        return protectedRegions;
    }
    
    public void stopScheduledEvent() {
        try {
            cr.cancel();
        } catch (IllegalStateException e) {
            System.out.println("SOMETHING WENT VERY WRONG");
        }
    }
}
