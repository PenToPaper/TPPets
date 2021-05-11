package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.PermissionChecker;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CommandTeleportAll extends TeleportCommand {
    private PetType.Pets petType;
    private List<PetStorage> petList;
    private final List<PetStorage> errorPetList = new ArrayList<>();

    public CommandTeleportAll(TPPets thisPlugin, CommandSender sender, String[] args) {
        super(thisPlugin, sender, args);
    }

    private boolean initializePetList() throws SQLException {
        try {
            this.petType = PetType.Pets.valueOf(this.args[0].toUpperCase());

            List<PetStorage> petList = this.thisPlugin.getDatabase().getAllPetsFromOwner(this.commandFor.getUniqueId().toString());

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

    private boolean isValidSyntax() {
        // Remember that correctForSelfSyntax() will not run if correctForOtherPlayerSyntax() is true
        return (this.isIntendedForSomeoneElse && hasValidForOtherPlayerFormat("tppets.teleportother", 1)) || (!this.isIntendedForSomeoneElse && hasValidForSelfFormat(1));
    }

    public void processCommand() {
        if (this.commandStatus == CommandStatus.SUCCESS && isValidSyntax()) {
            processCommandGeneric();
        }

        displayStatus();
    }

    private void processCommandGeneric() {
        try {
            if (!initializePetList()) {
                return;
            }

            if (!PermissionChecker.hasPermissionToTeleportType(this.petType, this.sender)) {
                this.commandStatus = CommandStatus.INSUFFICIENT_PERMISSIONS;
                return;
            }

            if (!this.thisPlugin.canTpThere(this.sender, this.sender.getLocation())) {
                this.commandStatus = CommandStatus.CANT_TELEPORT_IN_PR;
                return;
            }

            teleportAllPets();

        } catch (SQLException exception) {
            this.commandStatus = CommandStatus.DB_FAIL;
        }
    }

    private void teleportAllPets() throws SQLException {
        for (PetStorage petStorage : this.petList) {
            if (!canTpToWorld(this.sender, petStorage.petWorld) || !teleportPetsFromStorage(this.sender.getLocation(), petStorage, this.isIntendedForSomeoneElse, !this.isIntendedForSomeoneElse || this.sender.hasPermission("tppets.teleportother"))) {
                this.commandStatus = CommandStatus.CANT_TELEPORT;
                this.errorPetList.add(petStorage);
            }
        }
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
                this.sender.sendMessage(ChatColor.RED + "Teleported all pets except: " + ChatColor.WHITE + getErrorPetNames());
                break;
            case CANT_TELEPORT_IN_PR:
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }

    private String getErrorPetNames() {
        StringBuilder errorPetNames = new StringBuilder();
        for (PetStorage errorPet : this.errorPetList) {
            errorPetNames.append(errorPet.petName).append(", ");
        }
        errorPetNames.delete(errorPetNames.lastIndexOf(", "), errorPetNames.length());
        return errorPetNames.toString();
    }
}
