package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class CommandTeleportAll extends TeleportCommand {
    private PetType.Pets petType;
    private List<PetStorage> petList;

    public CommandTeleportAll(TPPets thisPlugin, CommandSender sender, String[] args) {
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
        if (!initializePetList()) {
            return;
        }

        if (!PermissionChecker.hasPermissionToTeleportType(this.petType, this.sender)) {
            this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
            return;
        }

        if (!this.thisPlugin.canTpThere(this.sender)) {
            this.commandStatus = CommandStatus.CANT_TELEPORT_IN_PR;
            return;
        }

        if (!this.teleportPetsFromStorage(this.sender.getLocation(), this.petList, this.isIntendedForSomeoneElse, !this.isIntendedForSomeoneElse || this.sender.hasPermission("tppets.teleportother"))) {
            this.commandStatus = CommandStatus.CANT_TELEPORT;
        }
    }

    private boolean initializePetList() {
        try {
            this.petType = PetType.Pets.valueOf(this.args[0].toUpperCase());

            List<PetStorage> petList = new ArrayList<>();

            for (World world : Bukkit.getWorlds()) {
                List<PetStorage> petListWorld = thisPlugin.getDatabase().getPetsGeneric(this.commandFor.getUniqueId().toString(), world.getName(), this.petType);
                if (petListWorld == null) {
                    this.commandStatus = CommandStatus.DB_FAIL;
                    return false;
                }
                petList.addAll(petListWorld);
            }

            if (petList.size() == 0) {
                this.commandStatus = CommandStatus.NO_PET;
                return false;
            }

            this.petList = petList;

        } catch (IllegalArgumentException ignored) {
            this.commandStatus = CommandStatus.NO_PET_TYPE;
            return false;
        }
        return true;
    }

    private void displayStatus() {
        // SUCCESS, INVALID_SENDER, INSUFFICIENT_PERMISSIONS, NO_PLAYER, SYNTAX_ERROR, NO_PET, NO_PET_TYPE, DB_FAIL, CANT_TELEPORT
        switch (this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage((this.isIntendedForSomeoneElse ? ChatColor.WHITE + this.commandFor.getName() + "'s " : ChatColor.BLUE + "Your ") + ChatColor.WHITE + this.args[0] + "s " + ChatColor.BLUE + "have been teleported to you");
                break;
            case INSUFFICIENT_PERMISSIONS:
                this.sender.sendMessage(ChatColor.RED + "You don't have permission to do that");
                break;
            case NO_PLAYER:
                this.sender.sendMessage(ChatColor.RED + "Can't find player: " + ChatColor.WHITE + ArgValidator.isForSomeoneElse(this.args[0]));
                break;
            case SYNTAX_ERROR:
            case NO_PET_TYPE:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp [pet type] all");
                break;
            case NO_PET:
                this.sender.sendMessage(ChatColor.RED + "Could not find any " + ChatColor.WHITE + this.petType.toString().toLowerCase() + "s");
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not get any pets");
                break;
            case CANT_TELEPORT:
                this.sender.sendMessage(ChatColor.RED + "Could not teleport pets");
                break;
            case CANT_TELEPORT_IN_PR:
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
        }
    }
}
