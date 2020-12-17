package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class BaseCommand {
    protected boolean isIntendedForSomeoneElse;
    public OfflinePlayer commandFor;
    public Player sender;
    public String[] args;
    public TPPets thisPlugin;

    BaseCommand(TPPets thisPlugin, CommandSender sender, String[] args) {
        this.thisPlugin = thisPlugin;
        this.sender = null;
        this.commandFor = null;
        if (sender instanceof Player) {
            this.sender = (Player) sender;
        }
        this.args = args;
        initializeCommandFor();
    }

    private void initializeCommandFor() {
        if (ArgValidator.validateArgsLength(this.args, 1)) {
            String isForSomeoneElse = ArgValidator.isForSomeoneElse(this.args[0]);
            if (isForSomeoneElse != null) {
                this.isIntendedForSomeoneElse = true;
                this.commandFor = getOfflinePlayer(isForSomeoneElse);
                if (this.commandFor != null) {
                    this.args = Arrays.copyOfRange(this.args, 1, this.args.length);
                }
                return;
            }
        } else {
            this.isIntendedForSomeoneElse = false;
        }
        this.commandFor = this.sender;
    }

    @SuppressWarnings("deprecation")
    public OfflinePlayer getOfflinePlayer(String username) {
        if (ArgValidator.validateUsername(username)) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(username);
            if (player.hasPlayedBefore()) {
                return player;
            }
        }
        return null;
    }

}
