package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandPosition1 extends BaseCommand {
    public CommandPosition1(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    public void processCommand() {
        if (this.commandStatus == CommandStatus.SUCCESS && hasValidForSelfFormat(0)) {
            processCommandGeneric();
        }

        displayErrors();
    }

    private void processCommandGeneric() {
        this.thisPlugin.getRegionSelectionManager().setStartLocation(this.sender, this.sender.getLocation());
    }

    private boolean isSelectionComplete() {
        return this.thisPlugin.getRegionSelectionManager().getSelectionSession(this.sender).isCompleteSelection();
    }

    private void displayErrors() {
        switch(this.commandStatus) {
            case INVALID_SENDER:
                break;
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "First position set!" + (isSelectionComplete() ? " Selection is complete." : ""));
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
