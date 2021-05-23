package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Class representing a /tpp position2 command.
 * @author GatheringExp
 */
public class CommandPosition2 extends BaseCommand {
    /**
     * Relays data to {@link BaseCommand} for processing.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param args A truncated list of arguments. Includes all arguments after the /tpp position2.
     */
    public CommandPosition2(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    /**
     * Calling this method indicates that all necessary data is in the instance and the command can be processed.
     */
    public void processCommand() {
        if (this.commandStatus == CommandStatus.SUCCESS && hasValidForSelfFormat(0)) {
            processCommandGeneric();
        }

        displayStatus();
    }

    /**
     * Sets the region end location in {@link CommandPosition2#thisPlugin}'s {@link com.maxwellwheeler.plugins.tppets.regions.RegionSelectionManager}.
     */
    private void processCommandGeneric() {
        this.thisPlugin.getRegionSelectionManager().setEndLocation(this.sender, this.sender.getLocation());
    }

    /**
     * Returns if {@link CommandPosition2#sender}'s selection is complete.
     * @return true if the selection is complete, false if not.
     */
    private boolean isSelectionComplete() {
        return this.thisPlugin.getRegionSelectionManager().getSelectionSession(this.sender).isCompleteSelection();
    }

    /**
     * Messages the command status to the {@link CommandPosition2#sender}.
     */
    private void displayStatus() {
        switch(this.commandStatus) {
            case INVALID_SENDER:
                break;
            case SUCCESS:
                this.sender.sendMessage(ChatColor.BLUE + "Second position set!" + (isSelectionComplete() ? " Selection is complete." : ""));
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
