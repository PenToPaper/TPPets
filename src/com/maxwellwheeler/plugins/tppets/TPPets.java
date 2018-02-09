package com.maxwellwheeler.plugins.tppets;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.maxwellwheeler.plugins.tppets.commands.CommandCreateDogs;
import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.commands.CommandTpForward;
import com.maxwellwheeler.plugins.tppets.helpers.TimeCalculator;
import com.maxwellwheeler.plugins.tppets.listeners.TPPetsChunkListener;
import com.maxwellwheeler.plugins.tppets.listeners.TPPetsEntityListener;
import com.maxwellwheeler.plugins.tppets.region.CheckRegions;
import com.maxwellwheeler.plugins.tppets.region.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.storage.SQLite;

public class TPPets extends JavaPlugin implements Listener {
    // Configuration-based data types
    private ArrayList<ProtectedRegion> restrictedRegions = new ArrayList<ProtectedRegion>();
    private Hashtable<String, LostAndFoundRegion> lostRegions = new Hashtable<String, LostAndFoundRegion>();
    private Hashtable<String, List<String>> commandAliases = new Hashtable<String, List<String>>();

    // Database
    private SQLite dbc;
    
    // Periodic checking if pets are in restricted regions
    private int checkInterval;
    private CheckRegions cr;
    
    private boolean preventPlayerDamage;
    private boolean preventEnvironmentalDamage;
    private boolean preventMobDamage;
    
    /*
     * VARIABLE INITIALIZERS
     * 
     */
    
    private void initializeDBC() {
        dbc = new SQLite(this, getDataFolder().getPath(), "tppets");
        dbc.createDatabase();
        dbc.createTables();
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
    
    private void initializeCommandAliases() {
        Set<String> configKeyList = getConfig().getConfigurationSection("command_aliases").getKeys(false);
        for (String key : configKeyList) {
            List<String> tempAliasList = getConfig().getStringList("command_aliases." + key);
            tempAliasList.add(key);
            commandAliases.put(key, tempAliasList);
        }
    }
    
    private void initializeRestrictedRegions() {
        restrictedRegions = dbc.getProtectedRegions();
    }
    
    private void initializeLostRegions() {
        lostRegions = dbc.getLostRegions();
    }
    
    private void initializeCheckInterval() {
        checkInterval = TimeCalculator.getTimeFromString(getConfig().getString("check_interval"));
    }


    @Override
    public void onEnable() {
        // Config setup and pulling
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        initializeCommandAliases();
        initializeCheckInterval();
        
        // Database setup
        initializeDBC();
        
        // Database pulling
        initializeLostRegions();
        initializeRestrictedRegions();
        
        
        // Register events + commands
        getServer().getPluginManager().registerEvents(new TPPetsChunkListener(this), this);
        getServer().getPluginManager().registerEvents(new TPPetsEntityListener(this), this);
        initializeCommandAliases();
        this.getCommand("tpp").setExecutor(new CommandTPP(commandAliases));
        this.getCommand("generate-tamed-dogs").setExecutor(new CommandCreateDogs());
        this.getCommand("tp-forward").setExecutor(new CommandTpForward());


        initializeDamageConfigs();
        initializeLostRegions();
        startCheckingRegions();
    }
    
    /*
     * PROTECTED REGIONS
     * 
     */
    
    public void addProtectedRegion (ProtectedRegion pr) {
        restrictedRegions.add(pr);
    }
    
    public ProtectedRegion inProtectedRegion(Location lc) {
        for (ProtectedRegion pr : restrictedRegions) {
            if (pr.isInZone(lc)) {
                return pr;
            }
        }
        return null;
    }
    
    public ProtectedRegion inProtectedRegion(Player pl) {
        return inProtectedRegion(pl.getLocation());
    }
    
    public boolean isInProtectedRegion(Location lc) {
        for (ProtectedRegion pr : restrictedRegions) {
            if (pr.isInZone(lc)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isInProtectedRegion(Player pl) {
        return isInProtectedRegion(pl.getLocation());
    }
    
    public ProtectedRegion getProtectedRegion(String name) {
        for (ProtectedRegion pr : restrictedRegions) {
            if (pr.getZoneName().equals(name)) {
                return pr;
            }
        }
        return null;
    }

    public void removeProtectedRegion(String name) {
        for (int i = 0; i < restrictedRegions.size(); i++) {
            if (restrictedRegions.get(i).getZoneName().equals(name)) {
                restrictedRegions.remove(i);
                break;
            }
        }
    }
    
    /*
     * LOST REGIONS
     * 
     */
    
    public void addLostRegion(LostAndFoundRegion lfr) {
        lostRegions.put(lfr.getZoneName(), lfr);
    }
    
    public void removeLostRegion(LostAndFoundRegion lfr) {
        lostRegions.remove(lfr.getZoneName());
    }
    
    /*
     * CHECK REGIONS TIMER
     * 
     */
    
    public void startCheckingRegions() {
        if (checkInterval != 0) {
            cr = new CheckRegions(this);
            cr.runTaskTimer(this, 0, checkInterval);
        }
    }
    
    public void stopCheckingRegions() {
        try {
            cr.cancel();
        } catch (IllegalStateException e) {
            getLogger().info("Something went wrong while trying to stop checking regions");
        }
    }
    
    /*
     * GETTERS/SETTERS
     * 
     */
    
    public SQLite getSQLite() {
        return dbc;
    }
    
    public ArrayList<ProtectedRegion> getProtectedRegions() {
        return restrictedRegions;
    }
    
    public Hashtable<String, LostAndFoundRegion> getLostRegions() {
        return lostRegions;
    }
    
    public boolean getPreventPlayerDamage() {
        return preventPlayerDamage;
    }
    
    public boolean getPreventEnvironmentalDamage() {
        return preventEnvironmentalDamage;
    }
    
    public boolean getPreventMobDamage() {
        return preventMobDamage;
    }
    
    public LostAndFoundRegion getLostRegion(String name) {
        return lostRegions.get(name);
    }
}
