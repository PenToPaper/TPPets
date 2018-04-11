package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.util.logging.Level;

public class LogWrapper {
    private TPPets thisPlugin;
    private boolean updatedPets;
    private boolean successfulActions;
    private boolean unsuccessfulActions;
    private boolean preventedDamage;
    private boolean errors;

    public LogWrapper(TPPets thisPlugin, boolean updatedPets, boolean successfulActions, boolean unsuccessfulActions, boolean preventedDamage, boolean errors) {
        this.thisPlugin = thisPlugin;
        this.updatedPets = updatedPets;
        this.successfulActions = successfulActions;
        this.unsuccessfulActions = unsuccessfulActions;
        this.preventedDamage = preventedDamage;
        this.errors = errors;
    }

    public void logUpdatedPet(String message) {
        if (updatedPets) {
            thisPlugin.getLogger().info(message);
        }
    }

    public void logSuccessfulAction(String message) {
        if (successfulActions) {
            thisPlugin.getLogger().info(message);
        }
    }

    public void logUnsuccessfulAction(String message) {
        if (unsuccessfulActions) {
            thisPlugin.getLogger().info(message);
        }
    }

    public void logErrors(String message) {
        if (errors) {
            thisPlugin.getLogger().log(Level.SEVERE, message);
        }
    }

    public void logPreventedDamage(String message) {
        if (preventedDamage) {
            thisPlugin.getLogger().info(message);
        }
    }

    public boolean getUpdatedPets() {
        return updatedPets;
    }

    public boolean getSuccessfulActions() {
        return successfulActions;
    }

    public boolean getUnsuccessfulAction() {
        return unsuccessfulActions;
    }

    public boolean getErrors() {
        return errors;
    }

    public boolean getPreventedDamage() {
        return preventedDamage;
    }
}
