package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.WorldEditHelper;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Objects;

public class CommandLostAdd extends Command {
    private final WorldEditPlugin worldEditPlugin;

    CommandLostAdd(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        super(thisPlugin, sender, commandFor, args);
        this.worldEditPlugin = (WorldEditPlugin) this.thisPlugin.getServer().getPluginManager().getPlugin("WorldEdit");
    }

    @Override
    public void processCommand() {
        processCommandGeneric();
        displayStatus();
    }

    private void processCommandGeneric() {
        if (!ArgValidator.validateArgsLength(this.args, 1)) {
            this.commandStatus = CommandStatus.SYNTAX_ERROR;
            return;
        }

        if (!ArgValidator.softValidateRegionName(this.args[0])) {
            this.commandStatus = CommandStatus.INVALID_NAME;
            return;
        }

        Location[] locations = WorldEditHelper.getWePoints(this.worldEditPlugin, this.sender);

        if (locations == null) {
            this.commandStatus = CommandStatus.NO_REGION;
            return;
        }

        try {
            LostAndFoundRegion lfr = this.thisPlugin.getDatabase().getLostRegion(this.args[0]);

            if (lfr != null) {
                this.commandStatus = CommandStatus.ALREADY_DONE;
                return;
            }

        } catch (SQLException e) {
            this.commandStatus = CommandStatus.DB_FAIL;
            return;

        }

        try {
            LostAndFoundRegion lostAndFoundRegion = new LostAndFoundRegion(this.args[0], Objects.requireNonNull(locations[0].getWorld()).getName(), locations[0], locations[1]);
            if (this.thisPlugin.getDatabase().insertLostRegion(lostAndFoundRegion)) {
                this.thisPlugin.addLostRegion(lostAndFoundRegion);
                this.thisPlugin.updateLFReference(lostAndFoundRegion.getRegionName());
                this.thisPlugin.getLogWrapper().logSuccessfulAction("Player " + this.sender.getName() + " added lost and found region " + lostAndFoundRegion.getRegionName());
            } else {
                this.commandStatus = CommandStatus.DB_FAIL;
            }
        } catch (NullPointerException e) {
            this.commandStatus = CommandStatus.UNEXPECTED_ERROR;
        }

    }

    private void displayStatus() {
        switch(this.commandStatus) {
            case SUCCESS:
                this.sender.sendMessage("You have added lost and found region " + ChatColor.WHITE + this.args[0]);
                break;
            case DB_FAIL:
                this.sender.sendMessage(ChatColor.RED + "Could not add lost and found region");
                break;
            case NO_REGION:
                this.sender.sendMessage(ChatColor.RED + "Can't add region without a square WorldEdit selection");
                break;
            case INVALID_NAME:
                this.sender.sendMessage(ChatColor.RED + "Invalid region name: " + ChatColor.WHITE + this.args[0]);
                break;
            case ALREADY_DONE:
                this.sender.sendMessage(ChatColor.RED + "Region " + ChatColor.WHITE + this.args[0] + ChatColor.RED + " already exists");
                break;
            case SYNTAX_ERROR:
                this.sender.sendMessage(ChatColor.RED + "Syntax Error! Usage: /tpp lost add [region name]");
                break;
            default:
                this.sender.sendMessage(ChatColor.RED + "An unknown error occurred");
                break;
        }
    }
}
