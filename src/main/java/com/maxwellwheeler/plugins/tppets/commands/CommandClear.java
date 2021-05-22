package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandClear extends BaseCommand {
    public CommandClear(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    public void processCommand() {
        if (this.commandStatus == CommandStatus.SUCCESS && hasValidForSelfFormat(0)) {
            processCommandGeneric();
        }

        displayStatus();
    }

    private void processCommandGeneric() {
        this.thisPlugin.getRegionSelectionManager().clearPlayerSession(this.sender);
    }

    private void displayStatus() {
        switch(this.commandStatus) {
            case INVALID_SENDER:
                break;
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "Selection cleared.");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
