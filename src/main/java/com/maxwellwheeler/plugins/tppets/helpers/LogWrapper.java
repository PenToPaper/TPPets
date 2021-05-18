package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.util.logging.Level;

/**
 * A wrapper around all logging actions. Takes into account configurations regarding log spam
 * @author GatheringExp
 */
public class LogWrapper {
    private final TPPets thisPlugin;
    private final boolean updatedPets;
    private final boolean successfulActions;
    private final boolean unsuccessfulActions;
    private final boolean preventedDamage;
    private final boolean errors;

    /**
     * General constructor with configuration options
     * @param thisPlugin The TPPets plugin instance
     * @param updatedPets The logging updated pets option
     * @param successfulActions The logging successful actions option
     * @param unsuccessfulActions The logging unsuccessful actions option
     * @param preventedDamage The logging prevented damage option
     * @param errors The logging errors option
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
     * Logs a pet update
     * @param message The message to log
     */
    public void logUpdatedPet(String message) {
        if (this.updatedPets) {
            this.thisPlugin.getLogger().info("Updated Pet: " + message);
        }
    }

    /**
     * Logs a successful action
     * @param message The message to log
     */
    public void logSuccessfulAction(String message) {
        if (this.successfulActions) {
            this.thisPlugin.getLogger().info("Successful Action: " + message);
        }
    }

    /**
     * Logs an unsuccessful action
     * @param message The message to log
     */
    public void logUnsuccessfulAction(String message) {
        if (this.unsuccessfulActions) {
            this.thisPlugin.getLogger().info("Unsuccessful Action: " + message);
        }
    }

    /**
     * Logs an error
     * @param message The message to log
     */
    public void logErrors(String message) {
        if (this.errors) {
            this.thisPlugin.getLogger().log(Level.SEVERE, "Error: " +  message);
        }
    }

    /**
     * Logs prevented damage
     * @param message The message to log
     */
    public void logPreventedDamage(String message) {
        if (this.preventedDamage) {
            this.thisPlugin.getLogger().info("Prevented Damage: " + message);
        }
    }

    public void logPluginInfo(String message) {
        this.thisPlugin.getLogger().info("TPPets Plugin: " + message);
    }
}
