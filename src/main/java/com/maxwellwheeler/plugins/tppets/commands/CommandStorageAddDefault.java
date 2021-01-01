package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CommandStorageAddDefault implements Command {
    private final TPPets tpPets;
    private final Player sender;
    private final String[] args;
    private CommandStatus commandStatus;
    private enum CommandStatus{SUCCESS, DB_FAIL, ALREADY_DONE, SYNTAX_ERROR}

    CommandStorageAddDefault(TPPets tpPets, Player sender, String[] args) {
        this.tpPets = tpPets;
        this.sender = sender;
        this.args = args;
        this.commandStatus = CommandStatus.SUCCESS;
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

        if (this.tpPets.getDatabase().getServerStorageLocation(this.args[0], this.sender.getWorld()) != null) {
            this.commandStatus = CommandStatus.ALREADY_DONE;
            return;
        }

        if (this.tpPets.getDatabase().addServerStorageLocation(this.args[0], this.sender.getLocation())) {
            this.tpPets.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " has added server location " + this.args[0] + " " + TeleportCommand.formatLocation(this.sender.getLocation()));
            this.commandStatus = CommandStatus.SUCCESS;
        } else {
            this.commandStatus = CommandStatus.DB_FAIL;
        }

    }

    @Override
    public void displayStatus() {
        // SUCCESS, DB_FAIL, ALREADY_DONE, SYNTAX_ERROR
        switch (this.commandStatus) {
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not add sever storage location");
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.RED + "Server storage " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " already exists");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp storage add default");
                break;
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "You have added server storage location " + ChatColor.WHITE + this.args[0]);
                break;
        }
    }
}
