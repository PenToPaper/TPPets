package com.maxwellwheeler.plugins.tppets;

import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.*;
import com.maxwellwheeler.plugins.tppets.listeners.*;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.RegionSelectionManager;
import com.maxwellwheeler.plugins.tppets.storage.*;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * The plugin's main class.
 * @author GatheringExp
 *
 */
public class TPPets extends JavaPlugin {
    private Hashtable<String, ProtectedRegion> protectedRegions = new Hashtable<>();
    private Hashtable<String, LostAndFoundRegion> lostRegions = new Hashtable<>();
    private Hashtable<String, List<String>> commandAliases = new Hashtable<>();
    private Hashtable<String, List<String>> allowedPlayers = new Hashtable<>();

    private final ToolsManager toolsManager = new ToolsManager(getConfig().getConfigurationSection("tools"));
    private final RegionSelectionManager regionSelectionManager = new RegionSelectionManager();
    private final MobDamageManager mobDamageManager = new MobDamageManager(this, getConfig().getStringList("protect_pets_from"));

    // Database
    private SQLWrapper database;
    private DBUpdater databaseUpdater;

    // Config
    private ConfigUpdater configUpdater;

    private LogWrapper logWrapper;
    
    // Vault stuff
    private Permission perms;

    private boolean vaultEnabled;
    
    private boolean allowTpBetweenWorlds;
    private boolean allowUntamingPets;
    
    private PetLimitChecker petIndex;
    private int storageLimit;



    /*
     * VARIABLE INITIALIZERS
     *
     */

    private void initializeLogWrapper() {
        logWrapper = new LogWrapper(this, getConfig().getBoolean("logging.updated_pets", true), getConfig().getBoolean("logging.successful_actions", true), getConfig().getBoolean("logging.unsuccessful_actions", true), getConfig().getBoolean("logging.prevented_damage", true), getConfig().getBoolean("logging.errors", true));
    }

    private void initializeStorageLimit() {
        storageLimit = getConfig().getInt("storage_limit", 0);
    }

    /**
     * Initializes the {@link PetLimitChecker} based on total_pet_limit, dog_limit, cat_limit, and bird_limit integers in the config.
     */
    private void initializePetIndex() {
        petIndex = new PetLimitChecker(this, getConfig().getInt("total_pet_limit"), getConfig().getInt("dog_limit"), getConfig().getInt("cat_limit"), getConfig().getInt("bird_limit"), getConfig().getInt("horse_limit"), getConfig().getInt("mule_limit"), getConfig().getInt("llama_limit"), getConfig().getInt("donkey_limit"));
    }

    /**
     * Initializes the {@link DBWrapper} based on config options mysql.enable, mysql.host, mysql.port, mysql.database, mysql.username, and mysql.password.
     * If DBWrapper is false, it will use the SQLite connection rather than a MySQL one.
     */
    private void initializeDBC() {
        if (!getConfig().getBoolean("mysql.enable")) {
            // Use SQLite connection
            this.database = new SQLiteWrapper(getDataFolder().getPath(), "tppets", this);
        } else {
            // Use MySQL connection
            this.database = new MySQLWrapper(getConfig().getString("mysql.host"), getConfig().getInt("mysql.port"), getConfig().getString("mysql.database"), getConfig().getString("mysql.username"), getConfig().getString("mysql.password"), this);
        }
    }

    /**
     * Updates the database if it can, otherwise turns database = null, negatively impacting virtually every aspect of this plugin
     */
    private void updateDBC() {
        this.databaseUpdater = new DBUpdater(this);
        this.databaseUpdater.update(this.getDatabase());
        if (!databaseUpdater.isUpToDate()) {
            getLogWrapper().logErrors("Database is unable to be updated. Plugin cannot run.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Creates the tables if they don't exist, using the DBWrapper
     */
    private void createTables() {
        try {
            if (!this.database.initializeTables()) {
                getLogWrapper().logErrors("Database is unable to be initialized. Plugin cannot run.");
                getServer().getPluginManager().disablePlugin(this);
            }
        } catch (SQLException exception) {
            getLogWrapper().logErrors("Database is unable to be initialized. Plugin cannot run." + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Updates the spigot/bukkit config
     */
    private void updateConfig() {
        configUpdater = new ConfigUpdater(this);
        configUpdater.update();
    }
    
    /**
     * Loads configuration option tp_pets_between_worlds into memory.
     */
    private void initializeAllowTP() {
        allowTpBetweenWorlds = getConfig().getBoolean("tp_pets_between_worlds");
    }
    
    /**
     * Loads configuration option allow_untaming_pets into memory.
     */
    private void initializeAllowUntamingPets() {
        allowUntamingPets = getConfig().getBoolean("allow_untaming_pets");
    }
    
    /**
     * Initializes local variables of command aliases.
     */
    private void initializeCommandAliases() {
        Set<String> configKeyList = getConfig().getConfigurationSection("command_aliases").getKeys(false);
        for (String key : configKeyList) {
            List<String> tempAliasList = getConfig().getStringList("command_aliases." + key);
            List<String> lowercaseAliasList = new ArrayList<>();
            for (String alias : tempAliasList) {
                lowercaseAliasList.add(alias.toLowerCase());
            }
            lowercaseAliasList.add(key.toLowerCase());
            commandAliases.put(key.toLowerCase(), lowercaseAliasList);
        }
    }
    
    /**
     * Initializes protected regions in a list
     */
    private void initializeProtectedRegions() {
        try {
            this.protectedRegions = this.database.getProtectedRegions();
        } catch (SQLException ignored) {}
    }
    
    /**
     * Initializes protected regions in a list
     */
    private void initializeLostRegions() {
        try {
            this.lostRegions = this.database.getLostRegions();
        } catch (SQLException ignored) {}
    }
    
    /**
     * Checks if vault (soft dependency) is enabled
     */
    private void initializeVault() {
        if (vaultEnabled = getServer().getPluginManager().isPluginEnabled("Vault")) {
            initializePermissions();
            getLogWrapper().logSuccessfulAction("Vault detected. Permission tppets.tpanywhere will work with online and offline players.");
        } else {
            getLogWrapper().logSuccessfulAction("Vault not detected on this server. Permission tppets.tpanywhere will only work with online players.");
        }
    }
    
    /**
     * Initializes vault permissions object
     * @return if the operation was successful
     */
    private boolean initializePermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    /**
     * Initializes allowed players {@link Hashtable}
     */
    private void initializeAllowedPlayers() {
        try {
            this.allowedPlayers = this.database.getAllAllowedPlayers();
        } catch (SQLException ignored) {}
    }
    
    @Override
    public void onEnable() {
        // Config setup and pulling
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        updateConfig();
        initializeLogWrapper();
        initializeStorageLimit();
        initializeCommandAliases();
        initializeAllowTP();
        initializeAllowUntamingPets();

        // Database setup
        getLogWrapper().logSuccessfulAction("Setting up database.");
        initializeDBC();
        updateDBC();
        createTables();
        initializeAllowedPlayers();

        // Database pulling
        getLogWrapper().logSuccessfulAction("Getting data from database.");
        initializeLostRegions();
        initializeProtectedRegions();
        initializePetIndex();
        
        // Register events + commands
        getLogWrapper().logSuccessfulAction("Registering commands and events.");
        getServer().getPluginManager().registerEvents(new ListenerPetPosition(this), this);
        getServer().getPluginManager().registerEvents(new ListenerPetAccess(this), this);
        getServer().getPluginManager().registerEvents(new ListenerProtectedRegion(this), this);
        getServer().getPluginManager().registerEvents(new ListenerPlayerInteractPetExamine(this), this);
        getServer().getPluginManager().registerEvents(new ListenerPlayerInteractPetRelease(this), this);
        getServer().getPluginManager().registerEvents(new ListenerPlayerInteractRegionSelection(this), this);
        getServer().getPluginManager().registerEvents(new ListenerEntityTamed(this), this);
        getServer().getPluginManager().registerEvents(new ListenerEntityDamage(this), this);
        getServer().getPluginManager().registerEvents(new ListenerEntityDeath(this), this);
        initializeCommandAliases();
        this.getCommand("tpp").setExecutor(new CommandTPP(commandAliases, this));

        initializeLostRegions();
        initializeVault();
    }
    
    /*
     * PROTECTED REGIONS
     * 
     */
    
    /**
     * Adds protected region to list in memory of protected regions actively being protected
     * @param pr The {@link ProtectedRegion} to be added.
     */
    public void addProtectedRegion (ProtectedRegion pr) {
        protectedRegions.put(pr.getRegionName(), pr);
    }
    
    /**
     * 
     * @param lc The location to be checked
     * @return {@link ProtectedRegion} that the location is in, null otherwise
     */
    public ProtectedRegion getProtectedRegionWithin(Location lc) {
        for (String key : protectedRegions.keySet()) { 
            if (protectedRegions.get(key).isInRegion(lc)) {
                return protectedRegions.get(key);
            }
        }
        return null;
    }
    
    /**
     * 
     * @param lc The location to be checked
     * @return if location is in a {@link ProtectedRegion}
     */
    public boolean isInProtectedRegion(Location lc) {
        return getProtectedRegionWithin(lc) != null;
    }

    /**
     * Returns a protected region with a given name
     * @param name Name of {@link ProtectedRegion}
     * @return the referenced {@link ProtectedRegion}, null otherwise.
     */
    public ProtectedRegion getProtectedRegion(String name) {
        return protectedRegions.get(name);
    }

    /**
     * Removes a protected region from memory, but not from disk.
     * @param name Name of the protected region
     */
    public void removeProtectedRegion(String name) {
        protectedRegions.remove(name);
    }
    
    /**
     * Updates the lfReference property of {@link ProtectedRegion}s that have {@link LostAndFoundRegion} of name lfRegionName
     * @param lfRegionName {@link LostAndFoundRegion}'s name that should be refreshed within all {@link ProtectedRegion}s.
     */
    public void updateLFReference(String lfRegionName) {
        for (String key : protectedRegions.keySet()) {
            ProtectedRegion pr = protectedRegions.get(key);
            if (pr != null && pr.getLfName().equals(lfRegionName)) {
                pr.updateLFReference(this);
            }
        }
    }
    
    /**
     * Removes all lfRefernece properties of {@link ProtectedRegion}s that have name lfRegionName
     * @param lfRegionName The name of the {@link LostAndFoundRegion} that is being removed
     */
    public void removeLFReference(String lfRegionName) {
        for (String key : protectedRegions.keySet()) {
            ProtectedRegion pr = protectedRegions.get(key);
            if (pr != null && pr.getLfName().equals(lfRegionName)) {
                pr.setLfReference(this, null);
            }
        }
    }
    
    /*
     * LOST REGIONS
     * 
     */
    
    /**
     * Tests if a location is in a lost region
     * @param lc Location to be tested.
     * @return Boolean representing if the location is in a lost region.
     */
    public boolean isInLostRegion(Location lc) {
        for (String lfKey : lostRegions.keySet()) {
            if (lostRegions.get(lfKey).isInRegion(lc)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAllowedToPet(String petUUID, String playerUUID) {
        String trimmedPetUUID = UUIDUtils.trimUUID(petUUID);
        String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
        return this.getAllowedPlayers().containsKey(trimmedPetUUID) && this.getAllowedPlayers().get(trimmedPetUUID).contains(trimmedPlayerUUID);
    }
    
    /**
     * Adds {@link LostAndFoundRegion} to active {@link LostAndFoundRegion} list
     * @param lfr {@link LostAndFoundRegion} to add.
     */
    public void addLostRegion(LostAndFoundRegion lfr) {
        lostRegions.put(lfr.getRegionName(), lfr);
    }
    
    /**
     * Removes {@link LostAndFoundRegion} from active {@link LostAndFoundRegion} list
     */
    public void removeLostRegion(String regionName) {
        lostRegions.remove(regionName);
    }

    // TODO: ADD JAVADOC
    public boolean canTpThere(Player pl) {
        ProtectedRegion tempPr = getProtectedRegionWithin(pl.getLocation());
        boolean ret = pl.hasPermission("tppets.tpanywhere") || tempPr == null;
        if (!ret) {
            pl.sendMessage(tempPr.getEnterMessage());
        }
        return ret;
    }

    /*
     * GETTERS/SETTERS
     * 
     */
    public RegionSelectionManager getRegionSelectionManager() {
        return regionSelectionManager;
    }

    public SQLWrapper getDatabase() {
        return database;
    }
    
    public Hashtable<String, ProtectedRegion> getProtectedRegions() {
        return protectedRegions;
    }
    
    public Hashtable<String, LostAndFoundRegion> getLostRegions() {
        return lostRegions;
    }

    public LostAndFoundRegion getLostRegion(String name) {
        return lostRegions.get(name);
    }
    
    public Permission getPerms() {
        return perms;
    }

    public ToolsManager getToolsManager() {
        return toolsManager;
    }
    
    public boolean getVaultEnabled() {
        return vaultEnabled;
    }
    
    public boolean getAllowTpBetweenWorlds() {
        return allowTpBetweenWorlds;
    }
    
    public boolean getAllowUntamingPets() {
        return allowUntamingPets;
    }
    
    public PetLimitChecker getPetIndex() {
        return petIndex;
    }

    public DBUpdater getDatabaseUpdater() {
        return databaseUpdater;
    }

    public ConfigUpdater getConfigUpdater() {
        return configUpdater;
    }

    public Hashtable<String, List<String>> getAllowedPlayers() {
        return allowedPlayers;
    }

    public int getStorageLimit() {
        return storageLimit;
    }

    public LogWrapper getLogWrapper() {
        return logWrapper;
    }

    public MobDamageManager getMobDamageManager() {
        return mobDamageManager;
    }
}
