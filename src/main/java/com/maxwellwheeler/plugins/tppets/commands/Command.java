package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class Command {
    public Player sender;
    public OfflinePlayer commandFor;
    public String[] args;
    public TPPets thisPlugin;
    protected CommandStatus commandStatus = CommandStatus.SUCCESS;

    public Command(TPPets thisPlugin, CommandSender sender, String[] args) {
        this.thisPlugin = thisPlugin;
        this.sender = null;
        this.commandFor = null;
        if (sender instanceof Player) {
            this.sender = (Player) sender;
        }
        this.args = args;
    }

    public Command(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        this.thisPlugin = thisPlugin;
        this.sender = sender;
        this.commandFor = commandFor;
        this.args = args;
    }

    @SuppressWarnings("deprecation")
    public OfflinePlayer getOfflinePlayer(String username) {
        if (ArgValidator.softValidateUsername(username)) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(username);
            if (player.hasPlayedBefore()) {
                return player;
            }
        }
        return null;
    }

    protected void logSuccessfulAction(String commandType, String details) {
        this.thisPlugin.getLogWrapper().logSuccessfulAction((this.sender == null ? "Unknown Sender" : this.sender.getName()) + " - " + commandType + (details == null ? "" : " - " + details));
    }

    protected void logUnsuccessfulAction(String commandType, String errorType) {
        this.thisPlugin.getLogWrapper().logUnsuccessfulAction((this.sender == null ? "Unknown Sender" : this.sender.getName()) + " - " + commandType + " - " + errorType);
    }

    public abstract void processCommand();

    public boolean isForSelf() {
        return this.sender.equals(this.commandFor);
    }

    public CommandStatus getCommandStatus() {
        return this.commandStatus;
    }
}
