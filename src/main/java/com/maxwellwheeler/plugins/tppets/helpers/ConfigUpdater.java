package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.util.ArrayList;
import java.util.List;

/**
 * Updates the spigot/bukkit config file.
 * @author GatheringExp
 */
public class ConfigUpdater {
    /** A reference to the active TPPets instance. */
    private final TPPets thisPlugin;
    /** The current schema version represented in the config. */
    private int schemaVersion;

    /**
     * Initializes instance variables. Gets schema version by reading from the config file.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public ConfigUpdater(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
        this.schemaVersion = getSchemaVersionFromConfig();
    }

    /**
     * Gets the current schema version from the config file.
     * @return schema_version from the config file, or 0 if schema_version isn't in the config.
     */
    private int getSchemaVersionFromConfig() {
        return this.thisPlugin.getConfig().getInt("schema_version", 1);
    }

    /**
     * Sets the schema version at {@link ConfigUpdater#schemaVersion} and in the config file.
     * @param version The version to set.
     */
    private void setSchemaVersion(int version) {
        this.thisPlugin.getConfig().set("schema_version", version);
        this.schemaVersion = version;
    }

    /**
     * Starts the updating process. Cascades updates as needed until fully up to date.
     */
    public void update() {
        int updatedVersion = 4;
        int initialVersion = this.schemaVersion;
        if (this.schemaVersion != updatedVersion) {
            // Updates are necessary
            if (this.schemaVersion == 1) {
                oneToTwo();
                setSchemaVersion(2);
            }
            if (this.schemaVersion == 2) {
                twoToThree();
                setSchemaVersion(3);
            }
            if (this.schemaVersion == 3) {
                threeToFour();
                setSchemaVersion(4);
            }
            this.thisPlugin.saveConfig();
            this.thisPlugin.getLogWrapper().logPluginInfo("Updated config version from version " + initialVersion + " to " + this.schemaVersion);
        }
    }

    /**
     * Updates the schema version from one to two.
     */
    private void oneToTwo() {
        this.thisPlugin.getConfig().set("horse_limit", -1);
        this.thisPlugin.getConfig().set("mule_limit", -1);
        this.thisPlugin.getConfig().set("llama_limit", -1);
        this.thisPlugin.getConfig().set("donkey_limit", -1);
        this.thisPlugin.getConfig().set("tools.untame_pets", new String[]{"SHEARS"});
        this.thisPlugin.getConfig().set("tools.get_owner", new String[]{"BONE"});
        this.thisPlugin.getConfig().set("command_aliases.horses", new String[]{"horse"});
        this.thisPlugin.getConfig().set("command_aliases.mules", new String[]{"mule"});
        this.thisPlugin.getConfig().set("command_aliases.llamas", new String[]{"llama"});
        this.thisPlugin.getConfig().set("command_aliases.donkeys", new String[]{"donkey"});
        this.thisPlugin.getConfig().set("command_aliases.rename", new String[]{"setname"});
        this.thisPlugin.getConfig().set("command_aliases.allow", new String[]{"add"});
        this.thisPlugin.getConfig().set("command_aliases.remove", new String[]{"take"});
        this.thisPlugin.getConfig().set("command_aliases.list", new String[]{"show"});
    }

    /**
     * Updates the schema version from two to three.
     */
    private void twoToThree() {
        this.thisPlugin.getConfig().set("storage_limit", 5);
        this.thisPlugin.getConfig().set("command_aliases.store", new String[]{"move", "stable"});
        this.thisPlugin.getConfig().set("command_aliases.storage", new String[]{"setstable"});
        this.thisPlugin.getConfig().set("logging.updated_pets", true);
        this.thisPlugin.getConfig().set("logging.successful_actions", true);
        this.thisPlugin.getConfig().set("logging.unsuccessful_actions", true);
        this.thisPlugin.getConfig().set("logging.prevented_damage", true);
        this.thisPlugin.getConfig().set("logging.errors", true);
    }

    /**
     * Updates config's protect_pets_from based on its version three values to its version four values.
     */
    private void threeToFourIntelligentlyUpdateProtectPetsFrom() {
        List<String> oldProtectPetsFrom = this.thisPlugin.getConfig().getStringList("protect_pets_from");
        List<String> newProtectPetsFrom = new ArrayList<>();
        String[] defaultProtectPetsFrom = new String[] {"GuestDamage", "StrangerDamage", "OwnerDamage", "EnvironmentalDamage", "MobDamage"};

        if (oldProtectPetsFrom.size() == 0) {
            this.thisPlugin.getConfig().set("protect_pets_from", defaultProtectPetsFrom);
            return;
        }

        for(String protect : defaultProtectPetsFrom) {
            if (oldProtectPetsFrom.contains(protect)) {
                newProtectPetsFrom.add(protect);
            }
        }

        if (oldProtectPetsFrom.contains("PlayerDamage")) {
            newProtectPetsFrom.add("StrangerDamage");
            newProtectPetsFrom.add("GuestDamage");
        }

        this.thisPlugin.getConfig().set("protect_pets_from", newProtectPetsFrom);
    }

    /**
     * Renames config's tools.untame_pets to tools.release_pets.
     */
    private void threeToFourIntelligentlyUpdateReleaseTool() {
        List<String> oldReleaseTools = this.thisPlugin.getConfig().getStringList("tools.untame_pets");
        if (oldReleaseTools.size() == 0) {
            this.thisPlugin.getConfig().set("tools.release_pets", new String[]{"SHEARS"});
        } else {
            this.thisPlugin.getConfig().set("tools.release_pets", oldReleaseTools.toArray());
        }
    }

    /**
     * Updates the schema version from three to four.
     */
    private void threeToFour() {
        threeToFourIntelligentlyUpdateProtectPetsFrom();
        threeToFourIntelligentlyUpdateReleaseTool();

        this.thisPlugin.getConfig().set("tools.select_region", new String[]{"BLAZE_ROD"});
        this.thisPlugin.getConfig().set("command_aliases.tp", new String[]{"teleport", "find", "get"});
        this.thisPlugin.getConfig().set("command_aliases.allowed", new String[]{"permitted", "guests", "guest", "g"});
        this.thisPlugin.getConfig().set("command_aliases.all", new String[]{"findall", "getall"});
        this.thisPlugin.getConfig().set("command_aliases.position1", new String[]{"1", "pos1", "startpos", "start"});
        this.thisPlugin.getConfig().set("command_aliases.position2", new String[]{"2", "pos2", "endpos", "end"});
        this.thisPlugin.getConfig().set("command_aliases.clear", new String[]{"wipe"});
        this.thisPlugin.getConfig().set("command_aliases.release", new String[]{"untame"});
        this.thisPlugin.getConfig().set("command_aliases.serverstorage", new String[]{"defstorage", "sstorage", "serverstable"});

        for (String pet : new String[]{"dogs", "cats", "birds", "horses", "mules", "llamas", "donkeys"}) {
            this.thisPlugin.getConfig().set("command_aliases." + pet, null);
        }
    }
}
