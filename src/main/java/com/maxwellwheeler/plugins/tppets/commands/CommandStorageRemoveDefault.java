package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CommandStorageRemoveDefault extends Command {
    CommandStorageRemoveDefault(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }

    @Override
    public void processCommand() {
        removeDefaultStorage();
        displayStatus();
    }

    private void removeDefaultStorage() {
        try {
            if (!ArgValidator.validateArgsLength(this.args, 1)) {
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                return;
            }

            if (this.thisPlugin.getDatabase().getServerStorageLocation(this.args[0], this.sender.getWorld()) == null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

            if (!this.thisPlugin.getDatabase().removeServerStorageLocation(this.args[0], this.sender.getWorld())) {
                this.commandStatus = CommandStatus.DB_FAIL;
                return;
            }

            this.thisPlugin.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " has removed " + this.args[0] + " server location in world " + this.sender.getWorld().getName());

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private void displayStatus() {
        // SUCCESS, DB_FAIL, ALREADY_DONE, SYNTAX_ERROR
        switch (this.commandStatus) {
            case INVALID_SENDER:
                break;
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "Server storage " + ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " in " + ChatColor.WHITE + this.sender.getWorld().getName() + ChatColor.BLUE + " has been removed");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not remove sever storage location" + ChatColor.WHITE + this.args[0]);
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.RED + "Server storage " + ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " in" + ChatColor.WHITE + this.sender.getWorld().getName() + ChatColor.RED + " already does not exist");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage remove default");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
