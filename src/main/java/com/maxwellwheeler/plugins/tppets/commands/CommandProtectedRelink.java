package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class CommandProtectedRelink extends Command {
    CommandProtectedRelink(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
    }
    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
    }

    private void processCommandGeneric() {
        try {
            if (!ArgValidator.validateArgsLength(this.args, 2)) {
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                return;
            }

            if (!ArgValidator.softValidateRegionName(this.args[0])) {
                this.commandStatus = CommandStatus.INVALID_PR_NAME;
                return;
            }

            if (!ArgValidator.softValidateRegionName(this.args[1])) {
                this.commandStatus = CommandStatus.INVALID_LR_NAME;
                return;
            }

            ProtectedRegion protectedRegion = this.thisPlugin.getProtectedRegionManager().getProtectedRegion(this.args[0]);

            if (protectedRegion == null) {
                this.commandStatus = CommandStatus.NO_REGION;
                return;
            }

            if (this.thisPlugin.getDatabase().relinkProtectedRegion(this.args[0], this.args[1])) {
                protectedRegion.setLfName(this.args[1]);
                protectedRegion.updateLFReference(this.thisPlugin);
                this.thisPlugin.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " relinked protected region " + protectedRegion.getRegionName() + " to " + protectedRegion.getLfName());
            } else {
                this.commandStatus = CommandStatus.DB_FAIL;
            }

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private void displayStatus() {
        switch(this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "You have relinked protected region " + ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " to lost and found region " + ChatColor.WHITE + this.args[1]);
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp protected relink [protected region name] [lost and found region name]");
                break;
            case INVALID_PR_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid protected region name: " + ChatColor.WHITE + this.args[0]);
                break;
            case INVALID_LR_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid lost and found region name: " + ChatColor.WHITE + this.args[1]);
                break;
            case NO_REGION:
                this.sender.sendMessage(ChatColor.RED + "Can't find protected region: " + ChatColor.WHITE + this.args[0]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not relink regions");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
