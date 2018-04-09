package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;

/**
 * Updates the spigot/bukkit config file, so that expected values should always(tm) be there
 * @author GatheringExp
 *
 */
public class ConfigUpdater {
    private TPPets thisPlugin;
    private int schemaVersion;
    private final int updatedVersion = 3;

    /**
     * General constructor. Gets current schema version from config
     * @param thisPlugin TPPets plugin instance
     */
    public ConfigUpdater(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
        this.schemaVersion = getSchemaVersionFromConfig();
    }

    /**
     * Gets the current schema version from the active config
     * @return the schema version
     */
    private int getSchemaVersionFromConfig() {
        return thisPlugin.getConfig().getInt("schema_version", 1);
    }

    /**
     * Sets the schema version in memory and in the config file
     * @param version The version to set the config to
     */
    private void setSchemaVersion(int version, boolean save) {
        thisPlugin.getConfig().set("schema_version", version);
        if (save) {
            thisPlugin.saveConfig();
        }
        schemaVersion = version;
    }

    /**
     * Is the config up to date?
     * @return True if yes, false if no
     */
    public boolean isUpToDate() {
        return thisPlugin.getConfig().getInt("schema_version", -1) == updatedVersion;
    }

    /**
     * Core method for updating the config. Compares the schema version found with the most up-to-date one, and updates as needed
     */
    public void update() {
        if (schemaVersion != updatedVersion) {
            // Updates are necessary
            if (schemaVersion == 1) {
                oneToTwo();
                setSchemaVersion(2, false);
            }
            if (schemaVersion == 2) {
                twoToThree();
                setSchemaVersion(3, false);
            }
            thisPlugin.saveConfig();
        }
    }

    /**
     * Updates the schema version from one to two
     */
    private void oneToTwo() {
        thisPlugin.getConfig().set("horse_limit", -1);
        thisPlugin.getConfig().set("mule_limit", -1);
        thisPlugin.getConfig().set("llama_limit", -1);
        thisPlugin.getConfig().set("donkey_limit", -1);
        thisPlugin.getConfig().set("horse_limit", -1);
        thisPlugin.getConfig().set("tools.untame_pets", new String[]{"SHEARS"});
        thisPlugin.getConfig().set("tools.get_owner", new String[]{"BONE"});
        thisPlugin.getConfig().set("command_aliases.horses", new String[]{"horse"});
        thisPlugin.getConfig().set("command_aliases.mules", new String[]{"mule"});
        thisPlugin.getConfig().set("command_aliases.llamas", new String[]{"llama"});
        thisPlugin.getConfig().set("command_aliases.donkeys", new String[]{"donkey"});
        thisPlugin.getConfig().set("command_aliases.rename", new String[]{"setname"});
        thisPlugin.getConfig().set("command_aliases.allow", new String[]{"add"});
        thisPlugin.getConfig().set("command_aliases.remove", new String[]{"take"});
        thisPlugin.getConfig().set("command_aliases.list", new String[]{"show"});
    }

    private void twoToThree() {
        thisPlugin.getConfig().set("command_aliases.store", new String[]{"keep", "stable"});
        thisPlugin.getConfig().set("command_aliases.store", new String[]{"keep", "stable"});
    }
}
