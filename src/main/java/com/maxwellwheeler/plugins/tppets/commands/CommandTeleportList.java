package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.List;

public class CommandTeleportList extends TeleportCommand {
    private List<PetStorage> petList;
    private PetType.Pets petType;

    public CommandTeleportList(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    public void processCommand() {
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayStatus();
    }

    private boolean isValidSyntax() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.teleportother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    private void processCommandGeneric() {
        if (!initializePetType()) {
            return;
        }

        if (!PermissionChecker.hasPermissionToTeleportType(this.petType, this.sender)) {
            this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
            return;
        }

        if (!initializePetList()) {
            return;
        }

        announcePetsFromList();
    }

    private boolean initializePetType() {
        try {
            this.petType = PetType.Pets.valueOf(this.args[0].toUpperCase());
            return true;
        } catch (IllegalArgumentException ignored) {
            this.commandStatus = CommandStatus.NO_PET_TYPE;
            return false;
        }
    }

    private boolean initializePetList() {
        try {
            List<PetStorage> petList = this.thisPlugin.getDatabase().getAllPetsFromOwner(this.commandFor.getUniqueId().toString());

            if (petList.size() == 0) {
                this.commandStatus = CommandStatus.NO_PET;
                return false;
            }

            this.petList = petList;
            return true;
        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return false;
        }
    }

    private void announcePetsFromList() {
        this.sender.sendMessage(ChatColor.DARK_GRAY + "---------" + ChatColor.BLUE + "[ " + ChatColor.WHITE + this.commandFor.getName() + "'s " + this.petType.toString().toLowerCase() + ChatColor.BLUE + " names ]" + ChatColor.DARK_GRAY + "---------");
        for (int i = 0; i < this.petList.size(); i++) {
            this.sender.sendMessage(ChatColor.WHITE + "  " + (i + 1) + ") " + this.petList.get(i).petName + (canTpToWorld(this.sender, this.petList.get(i).petWorld) ? "" : ChatColor.RED + " (In: " + this.petList.get(i).petWorld + ")"));
        }
        this.sender.sendMessage(ChatColor.DARK_GRAY + "----------------------------------");
    }

    private void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, SYNTAX_ERROR, NO_PET, NO_PET_TYPE, DB_FAIL
        switch (this.commandStatus) {
            case SUCCESS:
            case INVALID_SENDER:
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case SYNTAX_ERROR:
            case NO_PET_TYPE:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp all [pet type]");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find any " + ChatColor.WHITE + this.petType.toString().toLowerCase() + "s");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not allow user to pet");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
