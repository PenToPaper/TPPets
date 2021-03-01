package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

public class CommandLost extends BaseCommand{

    public CommandLost(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    public void processCommand() {
        if (this.commandStatus == CommandStatus.SUCCESS && hasValidForSelfFormat(1)) {
            processCommandGeneric();
        }

        displayErrors();
    }

    public void processCommandGeneric() {
        Command commandToRun = null;

        switch(this.args[0].toLowerCase()) {
            case "add":
                commandToRun = new CommandLostAdd(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            case "remove":
                commandToRun = new CommandLostRemove(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            case "list":
                commandToRun = new CommandLostList(this.thisPlugin, this.sender, this.commandFor, Arrays.copyOfRange(this.args, 1, this.args.length));
                break;
            default:
                this.commandStatus = CommandStatus.SYNTAX_ERROR;
                break;
        }

        if (commandToRun != null) {
            commandToRun.processCommand();
        }
    }

    private void displayErrors() {
        switch(this.commandStatus) {
            case SUCCESS:
            case INVALID_SENDER:
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp lost [add/remove/list]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}