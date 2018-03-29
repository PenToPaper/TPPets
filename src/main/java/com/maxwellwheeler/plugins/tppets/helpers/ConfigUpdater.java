package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class ConfigUpdater {
    private TPPets thisPlugin;
    private int schemaVersion;
    private int updatedVersion = 2;

    public ConfigUpdater(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
        this.schemaVersion = getSchemaVersionFromConfig();
    }

    public int getSchemaVersionFromConfig() {
        return thisPlugin.getConfig().getInt("schema_version", 1);
    }

    public void setSchemaVersion(int version) {
        thisPlugin.getConfig().set("schema_version", version);
        thisPlugin.saveConfig();
        schemaVersion = version;
    }

    public void update() {
        if (schemaVersion != updatedVersion) {
            if (schemaVersion == 1) {
                oneToTwo();
                setSchemaVersion(2);
            }
        }
    }

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
        thisPlugin.saveConfig();
    }
}
