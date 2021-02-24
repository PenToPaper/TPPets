package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

public class CommandAllowList extends BaseCommand{
    public CommandAllowList(TPPets thisPlugin, CommandSender sender, String[] args) {
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
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.allowother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    private void processCommandGeneric() {
        if (!ArgValidator.softValidatePetName(this.args[0])) {
            this.commandStatus = CommandStatus.NO_PET;
            return;
        }

        List<PetStorage> petList = this.thisPlugin.getDatabase().getPetByName(this.commandFor.getUniqueId().toString(), this.args[0]);

        if (petList == null) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;
        }

        if (petList.size() == 0) {
            this.commandStatus = CommandStatus.NO_PET;
            return;
        }

        // TODO: Remove untrim and trim from UUIDs. Kinda a silly idea in the first place lol
        List<String> playerUUIDs = thisPlugin.getAllowedPlayers().get(petList.get(0).petId);

        this.announceAllowedPlayers(playerUUIDs);
    }

    private void announceAllowedPlayers(List<String> playerUUIDs) {
        this.sender.sendMessage(ChatColor.GRAY + "---------" + ChatColor.BLUE + "[ Allowed Players for " + ChatColor.WHITE +  this.commandFor.getName() + "'s " + this.args[0] + ChatColor.BLUE + " ]" + ChatColor.GRAY + "---------");

        for (String playerUUID : playerUUIDs) {
            String untrimmedUUID = UUIDUtils.unTrimUUID(playerUUID);
            if (untrimmedUUID != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(untrimmedUUID));
                if (offlinePlayer.hasPlayedBefore()) {
                    this.sender.sendMessage(ChatColor.WHITE + offlinePlayer.getName());
                }
            }
        }

        this.sender.sendMessage(ChatColor.GRAY + "-------------------------------------------");
    }

    private void displayStatus() {
        switch (this.commandStatus) {
            case SUCCESS:
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp list [pet name]");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find pet: " + ChatColor.WHITE +  this.args[0]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not find allowed users");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
