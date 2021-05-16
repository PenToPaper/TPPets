package com.maxwellwheeler.plugins.tppets;

import com.maxwellwheeler.plugins.tppets.commands.CommandTPP;
import com.maxwellwheeler.plugins.tppets.helpers.*;
import com.maxwellwheeler.plugins.tppets.listeners.*;
import com.maxwellwheeler.plugins.tppets.regions.LostRegionManager;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegionManager;
import com.maxwellwheeler.plugins.tppets.regions.RegionSelectionManager;
import com.maxwellwheeler.plugins.tppets.storage.*;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

/**
 * The plugin's main class.
 * @author GatheringExp
 *
 */
public class TPPets extends JavaPlugin {
    private final Hashtable<String, List<String>> commandAliases = new Hashtable<>();

    private LostRegionManager lostRegionManager = null;
    private ProtectedRegionManager protectedRegionManager = null;
    private GuestManager guestManager = null;
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
        this.logWrapper = new LogWrapper(this, getConfig().getBoolean("logging.updated_pets", true), getConfig().getBoolean("logging.successful_actions", true), getConfig().getBoolean("logging.unsuccessful_actions", true), getConfig().getBoolean("logging.prevented_damage", true), getConfig().getBoolean("logging.errors", true));
    }

    private void initializeStorageLimit() {
        this.storageLimit = getConfig().getInt("storage_limit", 0);
    }

    /**
     * Initializes the {@link PetLimitChecker} based on total_pet_limit, dog_limit, cat_limit, and bird_limit integers in the config.
     */
    private void initializePetIndex() {
        this.petIndex = new PetLimitChecker(this, getConfig().getInt("total_pet_limit"), getConfig().getInt("dog_limit"), getConfig().getInt("cat_limit"), getConfig().getInt("bird_limit"), getConfig().getInt("horse_limit"), getConfig().getInt("mule_limit"), getConfig().getInt("llama_limit"), getConfig().getInt("donkey_limit"));
    }

    private void initializeProtectedRegionManager() {
        try {
            this.protectedRegionManager = new ProtectedRegionManager(this);
        } catch (SQLException exception) {
            getLogWrapper().logErrors("Database cannot fetch protected regions. Plugin cannot run.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeLostRegionManager() {
        try {
            this.lostRegionManager = new LostRegionManager(this);
        } catch (SQLException exception) {
            getLogWrapper().logErrors("Database cannot fetch lost regions. Plugin cannot run.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

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
     * Updates the database if it can, otherwise disables the plugin
     */
    private void updateDBC() {
        try {
            this.databaseUpdater = new DBUpdater(this);
            this.databaseUpdater.update(this.getDatabase());
            if (!databaseUpdater.isUpToDate()) {
                getLogWrapper().logErrors("Database is unable to be updated. Plugin cannot run.");
                getServer().getPluginManager().disablePlugin(this);
            }
        } catch (SQLException exception) {
            getLogWrapper().logErrors("Database is unable to be updated. Plugin cannot run." + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Creates the tables if they don't exist, using the SQLWrapper
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
        this.configUpdater = new ConfigUpdater(this);
        this.configUpdater.update();
    }
    
    /**
     * Loads configuration option tp_pets_between_worlds into memory.
     */
    private void initializeAllowTP() {
        this.allowTpBetweenWorlds = getConfig().getBoolean("tp_pets_between_worlds");
    }
    
    /**
     * Loads configuration option allow_untaming_pets into memory.
     */
    private void initializeAllowUntamingPets() {
        this.allowUntamingPets = getConfig().getBoolean("allow_untaming_pets");
    }
    
    /**
     * Initializes local variables of command aliases.
     */
    private void initializeCommandAliases() {
        ConfigurationSection commandAliases = getConfig().getConfigurationSection("command_aliases");
        if (commandAliases != null) {
            Set<String> configKeyList = commandAliases.getKeys(false);
            for (String key : configKeyList) {
                List<String> aliasList = commandAliases.getStringList(key);
                aliasList.replaceAll(String::toLowerCase);
                this.commandAliases.put(key.toLowerCase(), aliasList);
            }
        }

    }
    
    /**
     * Checks if vault (soft dependency) is enabled
     */
    private void initializeVault() {
        if (this.vaultEnabled = getServer().getPluginManager().isPluginEnabled("Vault") && initializePermissions()) {
            getLogWrapper().logSuccessfulAction("Vault detected. Permission tppets.tpanywhere will work with online and offline players.");
        } else {
            getLogWrapper().logSuccessfulAction("Vault not detected on this server. Permission tppets.tpanywhere will only work with online players.");
        }
    }
    
    /**
     * Initializes vault permissions object
     */
    private boolean initializePermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp != null) {
            this.perms = rsp.getProvider();
            return true;
        }
        return false;
    }

    /**
     * Initializes allowed players {@link Hashtable}
     */
    private void initializeGuestManager() {
        try {
            this.guestManager = new GuestManager(this.database);
        } catch (SQLException exception) {
            getLogWrapper().logErrors("Database cannot fetch guests. Plugin cannot run.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeCommandListener() {
        PluginCommand tpp = this.getCommand("tpp");
        if (tpp != null) {
            tpp.setExecutor(new CommandTPP(this.commandAliases, this));
        } else {
            getLogWrapper().logErrors("Plugin could not access its root command. Plugin cannot run.");
            getServer().getPluginManager().disablePlugin(this);
        }
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
        initializeGuestManager();

        // Database pulling
        getLogWrapper().logSuccessfulAction("Getting data from database.");
        initializeLostRegionManager();
        initializeProtectedRegionManager();
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
        initializeCommandListener();

        initializeVault();
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

    public ProtectedRegionManager getProtectedRegionManager() {
        return this.protectedRegionManager;
    }

    public LostRegionManager getLostRegionManager() {
        return this.lostRegionManager;
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

    public GuestManager getGuestManager() {
        return guestManager;
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
