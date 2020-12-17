package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CommandStorageRemove implements Command {
    private final TPPets tpPets;
    private final Player sender;
    private final OfflinePlayer commandFor;
    private final String[] args;
    private CommandStatus commandStatus;
    private enum CommandStatus{SUCCESS, SYNTAX_ERROR, DB_FAIL, ALREADY_DONE}

    CommandStorageRemove(TPPets tpPets, Player sender, OfflinePlayer commandFor, String[] args) {
        this.tpPets = tpPets;
        this.sender = sender;
        this.commandFor = commandFor;
        this.args = args;
        this.commandStatus = CommandStatus.SUCCESS;
    }

    public boolean isForSelf() {
        return this.sender.equals(this.commandFor);
    }

    @Override
    public void processCommand() {
        if (!ArgValidator.validateArgsLength(this.args, 1)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return;
        }

        if (this.tpPets.getDatabase() == null) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;
        }

        if (!ArgValidator.validateStorageName(this.args[0]) || this.tpPets.getDatabase().getStorageLocation(commandFor.getUniqueId().toString(), this.args[0]) == null) {
            this.commandStatus = CommandStatus.ALREADY_DONE;
            return;
        }

        if (this.tpPets.getDatabase().removeStorageLocation(commandFor.getUniqueId().toString(), this.args[0])) {
            this.tpPets.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " has removed location " + this.args[0] + " from " + commandFor.getName());
            this.commandStatus = CommandStatus.SUCCESS;
            return;
        }

        this.commandStatus = CommandStatus.DB_FAIL;
    }

    @Override
    public void displayStatus() {
        // SUCCESS, SYNTAX_ERROR, DB_FAIL, ALREADY_DONE
        switch(this.commandStatus) {
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not remove storage location");
                break;
            case ALREADY_DONE:
                this.sender.sendMessage((this.isForSelf() ? ChatColor.RED + "Storage" : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.RED + " storage") + " location " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " already does not exist");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage remove [storage name]");
                break;
            case SUCCESS:
                this.sender.sendMessage((this.isForSelf() ? ChatColor.BLUE + "Storage" : ChatColor.WHITE + this.commandFor.getName() + "'s" + ChatColor.BLUE) + " location " + ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " has been removed");
                break;
        }
    }
}
