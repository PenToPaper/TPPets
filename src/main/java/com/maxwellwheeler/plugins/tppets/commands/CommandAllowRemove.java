package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CommandAllowRemove extends BaseCommand {
    public CommandAllowRemove(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    public void processCommand() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayStatus();
    }

    private boolean isValidSyntax() {
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.allowother", 2)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(2));
    }

    private void processCommandGeneric() {
        OfflinePlayer playerToAllow = this.getOfflinePlayer(this.args[0]);

        if (playerToAllow == null) {
            this.commandStatus = CommandStatus.NO_TARGET_PLAYER;
            return;
        }

        if (!ArgValidator.softValidatePetName(this.args[1])) {
            this.commandStatus = CommandStatus.NO_PET;
            return;
        }

        List<PetStorage> petList = this.thisPlugin.getDatabase().getPetByName(this.commandFor.getUniqueId().toString(), this.args[1]);

        if (petList == null) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;
        }

        if (petList.size() == 0) {
            this.commandStatus = CommandStatus.NO_PET;
            return;
        }

        String trimmedPlayerId = UUIDUtils.trimUUID(playerToAllow.getUniqueId().toString());

        if (!this.thisPlugin.getAllowedPlayers().containsKey(petList.get(0).petId) || !this.thisPlugin.getAllowedPlayers().get(petList.get(0).petId).contains(trimmedPlayerId)) {
            this.commandStatus = CommandStatus.ALREADY_DONE;
            return;
        }

        if (this.removePlayer(petList.get(0).petId, trimmedPlayerId)) {
            this.thisPlugin.getLogWrapper().logSuccessfulAction(this.sender.getName() + " removed permission from " + this.args[0] + " to use " + this.commandFor.getName() + "'s pet named " + this.args[1]);
            this.commandStatus = CommandStatus.SUCCESS;
        } else {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private boolean removePlayer(String petId, String playerId) {
        if (this.thisPlugin.getDatabase().removeAllowedPlayer(petId, playerId)) {
            // Technically shouldn't need the following if block, but it's a good failsafe
            if (!this.thisPlugin.getAllowedPlayers().containsKey(petId)) {
                this.thisPlugin.getAllowedPlayers().put(petId, new ArrayList<>());
            }
            this.thisPlugin.getAllowedPlayers().get(petId).remove(playerId);

            return true;
        }

        return false;
    }

    private void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, NO_TARGET_PLAYER, SYNTAX_ERROR, NO_PET, DB_FAIL
        switch (this.commandStatus) {
            case INVALID_SENDER:
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case NO_TARGET_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + this.args[0]);
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp remove [player name] [pet name]");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Can't find pet: " + ChatColor.WHITE +  this.args[1]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not allow user to pet");
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.WHITE + this.args[0] + ChatColor.RED + " is already not allowed to " + (this.isForSelf() ? "" : ChatColor.WHITE + this.commandFor.getName() + "'s ") + ChatColor.WHITE + this.args[1]);
                break;
            case SUCCESS:
                this.sender.sendMessage(ChatColor.WHITE + this.args[0] + ChatColor.BLUE + " is no longer allowed to " + (this.isForSelf() ? "" : ChatColor.WHITE + this.commandFor.getName() + "'s ") + ChatColor.WHITE + this.args[1]);
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}