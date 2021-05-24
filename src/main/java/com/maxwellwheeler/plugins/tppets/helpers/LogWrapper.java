package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.util.logging.Level;

/**
 * A wrapper between all TPPets logging actions. Prefixes logs, and disables them when certain configuration options are
 * active.
 * @author GatheringExp
 */
public class LogWrapper {
    /** A reference to the active TPPets instance */
    private final TPPets thisPlugin;
    /** The setting for whether or not updated pets should be logged. */
    private final boolean updatedPets;
    /** The setting for whether or not successful actions should be logged. */
    private final boolean successfulActions;
    /** The setting for whether or not unsuccessful actions should be logged. */
    private final boolean unsuccessfulActions;
    /** The setting for whether or not prevented damage should be logged. */
    private final boolean preventedDamage;
    /** The setting for whether or not errors should be logged. */
    private final boolean errors;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param updatedPets The setting for whether or not updated pets should be logged.
     * @param successfulActions The setting for whether or not successful actions should be logged.
     * @param unsuccessfulActions The setting for whether or not unsuccessful actions should be logged.
     * @param preventedDamage The setting for whether or not prevented damage should be logged.
     * @param errors The setting for whether or not errors should be logged.
     */
    public LogWrapper(TPPets thisPlugin, boolean updatedPets, boolean successfulActions, boolean unsuccessfulActions, boolean preventedDamage, boolean errors) {
        this.thisPlugin = thisPlugin;
        this.updatedPets = updatedPets;
        this.successfulActions = successfulActions;
        this.unsuccessfulActions = unsuccessfulActions;
        this.preventedDamage = preventedDamage;
        this.errors = errors;
    }

    /**
     * Logs a pet update as info, if the setting is enabled. This can include pet positions and pet deaths.
     * @param message The updated pet message to log.
     */
    public void logUpdatedPet(String message) {
        if (this.updatedPets) {
            this.thisPlugin.getLogger().info("Updated Pet: " + message);
        }
    }

    /**
     * Logs a successful action as info, if the setting is enabled. This can include intentional actions from players.
     * @param message The successful action message to log.
     */
    public void logSuccessfulAction(String message) {
        if (this.successfulActions) {
            this.thisPlugin.getLogger().info("Successful Action: " + message);
        }
    }

    /**
     * Logs an unsuccessful action as info, if the setting is enabled. This can include unsuccessful intentional actions
     * from players.
     * @param message The unsuccessful action message to log.
     */
    public void logUnsuccessfulAction(String message) {
        if (this.unsuccessfulActions) {
            this.thisPlugin.getLogger().info("Unsuccessful Action: " + message);
        }
    }

    /**
     * Logs an error as severe, if the setting is enabled. This can include all critical errors that impact the plugin's
     * function.
     * @param message The error message to log.
     */
    public void logErrors(String message) {
        if (this.errors) {
            this.thisPlugin.getLogger().log(Level.SEVERE, "Error: " +  message);
        }
    }

    /**
     * Logs prevented damage as info, if the setting is enabled. This can include environmental or player damage.
     * @param message The prevented damage message to log.
     */
    public void logPreventedDamage(String message) {
        if (this.preventedDamage) {
            this.thisPlugin.getLogger().info("Prevented Damage: " + message);
        }
    }

    /**
     * Logs essential plugin info as info.
     * @param message The essential plugin message to log.
     */
    public void logPluginInfo(String message) {
        this.thisPlugin.getLogger().info("TPPets Plugin: " + message);
    }
}
