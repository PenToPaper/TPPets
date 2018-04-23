package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.util.logging.Level;

/**
 * A wrapper around all logging actions. Takes into account configurations regarding log spam
 * @author GatheringExp
 */
public class LogWrapper {
    private TPPets thisPlugin;
    private boolean updatedPets;
    private boolean successfulActions;
    private boolean unsuccessfulActions;
    private boolean preventedDamage;
    private boolean errors;

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
        if (updatedPets) {
            thisPlugin.getLogger().info(message);
        }
    }

    /**
     * Logs a successful action
     * @param message The message to log
     */
    public void logSuccessfulAction(String message) {
        if (successfulActions) {
            thisPlugin.getLogger().info(message);
        }
    }

    /**
     * Logs an unsuccessful action
     * @param message The message to log
     */
    public void logUnsuccessfulAction(String message) {
        if (unsuccessfulActions) {
            thisPlugin.getLogger().info(message);
        }
    }

    /**
     * Logs an error
     * @param message The message to log
     */
    public void logErrors(String message) {
        if (errors) {
            thisPlugin.getLogger().log(Level.SEVERE, message);
        }
    }

    /**
     * Logs prevented damage
     * @param message The message to log
     */
    public void logPreventedDamage(String message) {
        if (preventedDamage) {
            thisPlugin.getLogger().info(message);
        }
    }

    /**
     * @return If the updatedPets logging config option is set
     */
    public boolean getUpdatedPets() {
        return updatedPets;
    }

    /**
     * @return If the successful actions logging config option is set
     */
    public boolean getSuccessfulActions() {
        return successfulActions;
    }

    /**
     * @return If the unsuccessful actions logging config option is set
     */
    public boolean getUnsuccessfulAction() {
        return unsuccessfulActions;
    }

    /**
     * @return If the error logging config option is set
     */
    public boolean getErrors() {
        return errors;
    }

    /**
     * @return If the prevented damage logging config option is set
     */
    public boolean getPreventedDamage() {
        return preventedDamage;
    }
}
