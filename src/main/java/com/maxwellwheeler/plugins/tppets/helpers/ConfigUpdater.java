package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.util.ArrayList;
import java.util.List;

/**
 * Updates the spigot/bukkit config file, so that expected values should always(tm) be there
 * @author GatheringExp
 *
 */
public class ConfigUpdater {
    private final TPPets thisPlugin;
    private int schemaVersion;

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
        return this.thisPlugin.getConfig().getInt("schema_version", 1);
    }

    /**
     * Sets the schema version in memory and in the config file
     * @param version The version to set the config to
     */
    private void setSchemaVersion(int version) {
        this.thisPlugin.getConfig().set("schema_version", version);
        this.schemaVersion = version;
    }

    /**
     * Core method for updating the config. Compares the schema version found with the most up-to-date one, and updates as needed
     */
    public void update() {
        int updatedVersion = 4;
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
        }
    }

    /**
     * Updates the schema version from one to two
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
     * Updates the schema from two to three
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

    private void threeToFourIntelligentlyUpdateProtectPetsFrom() {
        List<?> oldProtectPetsFrom = this.thisPlugin.getConfig().getList("protect_pets_from");
        List<String> newProtectPetsFrom = new ArrayList<>();
        String[] defaultProtectPetsFrom = new String[] {"GuestDamage", "StrangerDamage", "OwnerDamage", "EnvironmentalDamage", "MobDamage"};

        if (oldProtectPetsFrom == null) {
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
     * Updates the schema from three to four
     */
    private void threeToFour() {
        threeToFourIntelligentlyUpdateProtectPetsFrom();

        this.thisPlugin.getConfig().set("tools.select_region", new String[]{"BLAZE_ROD"});
        this.thisPlugin.getConfig().set("command_aliases.tp", new String[]{"teleport", "find", "get"});
        this.thisPlugin.getConfig().set("command_aliases.allowed", new String[]{"permitted", "guests", "guest", "g"});
        this.thisPlugin.getConfig().set("command_aliases.all", new String[]{"findall", "getall"});
        this.thisPlugin.getConfig().set("command_aliases.position1", new String[]{"1", "pos1", "startpos", "start"});
        this.thisPlugin.getConfig().set("command_aliases.position2", new String[]{"2", "pos2", "endpos", "end"});
        this.thisPlugin.getConfig().set("command_aliases.release", new String[]{"untame"});

        for (String pet : new String[]{"dogs", "cats", "birds", "horses", "mules", "llamas", "donkeys"}) {
            this.thisPlugin.getConfig().set("command_aliases." + pet, null);
        }
    }
}
