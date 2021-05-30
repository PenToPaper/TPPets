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
 */
public class TPPets extends JavaPlugin {
    /** Command aliases for TPPets subcommands. Stored in &lt; Real command &lt; Aliases &gt; &gt; */
    private final Hashtable<String, List<String>> commandAliases = new Hashtable<>();

    /** The plugin's {@link LostRegionManager} instance for all of its {@link com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion}. */
    private LostRegionManager lostRegionManager = null;
    /** The plugin's {@link ProtectedRegionManager} instance for all of its {@link com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion}. */
    private ProtectedRegionManager protectedRegionManager = null;
    /** The plugin's {@link GuestManager} for its guest/pet associations. */
    private GuestManager guestManager = null;
    /** The plugin's {@link ToolsManager} for its tool aliases. */
    private final ToolsManager toolsManager = new ToolsManager(getConfig().getConfigurationSection("tools"));
    /** The plugin's {@link RegionSelectionManager} for admin region selections. */
    private final RegionSelectionManager regionSelectionManager = new RegionSelectionManager();
    /** The plugin's {@link MobDamageManager} for its pet protections. */
    private MobDamageManager mobDamageManager = null;
    /** The plugin's {@link PetLimitChecker} for its pet taming limits. */
    private PetLimitChecker petLimitChecker;

    /** The plugin's database. */
    private SQLWrapper database;
    /** The plugin's database schema updater. */
    private DBUpdater databaseUpdater;
    /** The plugin's config updater. */
    private ConfigUpdater configUpdater;

    /** The plugin's logger. **/
    private LogWrapper logWrapper;
    
    /** Vault's permission object. */
    private Permission perms;

    /** Represents if Vault is enabled on the server. */
    private boolean vaultEnabled;

    /** Represents config setting allowing teleporting of pets between worlds. */
    private boolean allowTpBetweenWorlds;
    /** Represents config setting allowing untaming of pets through the command and tool. */
    private boolean allowUntamingPets;
    /** Represents config setting limiting the total number of {@link com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation}s each player can have. */
    private int storageLimit;



    // VARIABLE INITIALIZERS

    /**
     * Initializes {@link TPPets#logWrapper} from config logging section.
     */
    private void initializeLogWrapper() {
        this.logWrapper = new LogWrapper(this, getConfig().getBoolean("logging.updated_pets", true), getConfig().getBoolean("logging.successful_actions", true), getConfig().getBoolean("logging.unsuccessful_actions", true), getConfig().getBoolean("logging.prevented_damage", true), getConfig().getBoolean("logging.errors", true));
    }

    /**
     * Initializes {@link TPPets#logWrapper} from config storage_limit.
     */
    private void initializeStorageLimit() {
        this.storageLimit = getConfig().getInt("storage_limit", 0);
    }

    /**
     * Initializes {@link TPPets#petLimitChecker} from config total_pet_limit, dog_limit, cat_limit, bird_limit, horse_limit, mule_limit, llama_limit, donkey_limit.
     */
    private void initializePetIndex() {
        this.petLimitChecker = new PetLimitChecker(this, getConfig().getInt("total_pet_limit"), getConfig().getInt("dog_limit"), getConfig().getInt("cat_limit"), getConfig().getInt("bird_limit"), getConfig().getInt("horse_limit"), getConfig().getInt("mule_limit"), getConfig().getInt("llama_limit"), getConfig().getInt("donkey_limit"));
    }

    /**
     * Initializes {@link TPPets#mobDamageManager} from config protect_pets_from list.
     */
    private void initializeMobDamageManager() {
        this.mobDamageManager = new MobDamageManager(this, getConfig().getStringList("protect_pets_from"));
    }

    /**
     * Initializes {@link TPPets#protectedRegionManager} from database. If the database fails, disables the plugin with
     * message: Database cannot fetch protected regions. Plugin cannot run.
     */
    private void initializeProtectedRegionManager() {
        try {
            this.protectedRegionManager = new ProtectedRegionManager(this);
        } catch (SQLException exception) {
            getLogWrapper().logErrors("Database cannot fetch protected regions. Plugin cannot run.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initializes {@link TPPets#lostRegionManager} from database. If the database fails, disables the plugin with
     * message: Database cannot fetch lost regions. Plugin cannot run.
     */
    private void initializeLostRegionManager() {
        try {
            this.lostRegionManager = new LostRegionManager(this);
        } catch (SQLException exception) {
            getLogWrapper().logErrors("Database cannot fetch lost regions. Plugin cannot run.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initializes {@link TPPets#database} as either:
     * {@link MySQLWrapper} if config setting mysql.enable is true.
     * {@link SQLiteWrapper} if config setting mysql.enable is false.
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
     * Initializes {@link TPPets#databaseUpdater} and runs {@link DBUpdater#update(SQLWrapper)}. If the database fails
     * while updating, disables the plugin with message: Database is unable to be updated. Plugin cannot run.
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
     * Initializes {@link TPPets#database}'s tables (if they don't exist) through {@link SQLWrapper#createTables()}.
     * If the database fails, disables the plugin with message: Database is unable to be initialized. Plugin cannot run.
     */
    private void createTables() {
        try {
            if (!this.database.createTables()) {
                getLogWrapper().logErrors("Database is unable to be initialized. Plugin cannot run.");
                getServer().getPluginManager().disablePlugin(this);
            }
        } catch (SQLException exception) {
            getLogWrapper().logErrors("Database is unable to be initialized. Plugin cannot run." + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initializes {@link TPPets#configUpdater} and runs {@link ConfigUpdater#update()}.
     */
    private void updateConfig() {
        this.configUpdater = new ConfigUpdater(this);
        this.configUpdater.update();
    }
    
    /**
     * Initializes {@link TPPets#allowTpBetweenWorlds} with config option tp_pets_between_worlds.
     */
    private void initializeAllowTP() {
        this.allowTpBetweenWorlds = getConfig().getBoolean("tp_pets_between_worlds");
    }

    /**
     * Initializes {@link TPPets#allowUntamingPets} with config option allow_untaming_pets.
     */
    private void initializeAllowUntamingPets() {
        this.allowUntamingPets = getConfig().getBoolean("allow_untaming_pets");
    }
    
    /**
     * Initializes {@link TPPets#commandAliases} with values from config section command_aliases.
     * @see TPPets#commandAliases
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
     * Initializes {@link TPPets#vaultEnabled} based on whether or not vault is enabled. Uses the Bukkit plugin manager.
     */
    private void initializeVault() {
        if (this.vaultEnabled = getServer().getPluginManager().isPluginEnabled("Vault") && initializePermissions()) {
            getLogWrapper().logPluginInfo("Vault detected. Permission tppets.tpanywhere will work with online and offline players.");
        } else {
            getLogWrapper().logPluginInfo("Vault not detected on this server. Permission tppets.tpanywhere will only work with online players.");
        }
    }
    
    /**
     * Initializes {@link TPPets#perms}. This method assumes that Vault is enabled.
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
     * Initializes {@link TPPets#guestManager} from database. If the database fails, disables the plugin with message:
     * Database cannot fetch guests. Plugin cannot run.
     */
    private void initializeGuestManager() {
        try {
            this.guestManager = new GuestManager(this.database);
        } catch (SQLException exception) {
            getLogWrapper().logErrors("Database cannot fetch guests. Plugin cannot run.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initializes the base /pp command listener with a new {@link CommandTPP} object. If the method fails to set the
     * command listener, disables the plugin with message: Plugin could not access its root command. Plugin cannot run.
     */
    private void initializeCommandListener() {
        PluginCommand tpp = this.getCommand("tpp");
        if (tpp != null) {
            tpp.setExecutor(new CommandTPP(this.commandAliases, this));
        } else {
            getLogWrapper().logErrors("Plugin could not access its root command. Plugin cannot run.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Initializes the plugin.
     */
    @Override
    public void onEnable() {
        // Config setup and pulling
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        initializeLogWrapper();
        updateConfig();
        initializeStorageLimit();
        initializeCommandAliases();
        initializeAllowTP();
        initializeAllowUntamingPets();
        initializeMobDamageManager();

        // Database setup
        getLogWrapper().logPluginInfo("Setting up database.");
        initializeDBC();
        updateDBC();
        createTables();
        initializeGuestManager();

        // Database pulling
        getLogWrapper().logPluginInfo("Getting data from database.");
        initializeLostRegionManager();
        initializeProtectedRegionManager();
        initializePetIndex();
        
        // Register events + commands
        getLogWrapper().logPluginInfo("Registering commands and events.");
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

    /** Gets the plugin's database. */
    public SQLWrapper getDatabase() {
        return database;
    }

    /** Gets the plugin's {@link RegionSelectionManager} for admin region selections. */
    public RegionSelectionManager getRegionSelectionManager() {
        return regionSelectionManager;
    }

    /** Gets the plugin's {@link ProtectedRegionManager} with all active {@link com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion}s. */
    public ProtectedRegionManager getProtectedRegionManager() {
        return this.protectedRegionManager;
    }

    /** Gets the plugin's {@link LostRegionManager} with all active {@link com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion}s. */
    public LostRegionManager getLostRegionManager() {
        return this.lostRegionManager;
    }

    /** Gets the plugin's {@link ToolsManager} for tool aliases.*/
    public ToolsManager getToolsManager() {
        return toolsManager;
    }

    /** Gets the plugin's {@link GuestManager} for guest/pet associations. */
    public GuestManager getGuestManager() {
        return guestManager;
    }

    /** Gets the plugin's {@link MobDamageManager} for its pet protections. */
    public MobDamageManager getMobDamageManager() {
        return mobDamageManager;
    }

    /** Gets the plugin's {@link PetLimitChecker} for its pet taming limits. */
    public PetLimitChecker getPetLimitChecker() {
        return petLimitChecker;
    }

    /** Gets the plugin's {@link ConfigUpdater}. */
    public ConfigUpdater getConfigUpdater() {
        return configUpdater;
    }

    /** Gets the plugin's {@link DBUpdater}. */
    public DBUpdater getDatabaseUpdater() {
        return databaseUpdater;
    }

    /** Gets the plugin's {@link LogWrapper} for all logging. */
    public LogWrapper getLogWrapper() {
        return logWrapper;
    }

    /** Gets the plugin's config setting allowing teleporting of pets between worlds. */
    public boolean getAllowTpBetweenWorlds() {
        return allowTpBetweenWorlds;
    }

    /** Gets the plugin's config setting allowing untaming of pets through the command and tool. */
    public boolean getAllowUntamingPets() {
        return allowUntamingPets;
    }

    /** Gets the plugin's config setting limiting the total number of {@link com.maxwellwheeler.plugins.tppets.regions.PlayerStorageLocation}s each player can have. */
    public int getStorageLimit() {
        return storageLimit;
    }

    /** Gets Vault's permission object, if Vault is active and seen by TPPets. */
    public Permission getPerms() {
        return perms;
    }

    /** Gets if Vault is active on the server and seen by TPPets. */
    public boolean getVaultEnabled() {
        return vaultEnabled;
    }
}
