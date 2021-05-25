package com.maxwellwheeler.plugins.tppets.commands;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.ArgValidator;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Class representing any TPP command.
 * Ex: In /tpp lost add, lost and add are both commands.
 * @author GatheringExp
 */
public abstract class Command {
    /** The player that sent the command */
    public Player sender;
    /** The player that the command is about. Is equal to {@link Command#sender} unless f:[username] syntax used. */
    public OfflinePlayer commandFor;
    /** A list of arguments relevant to the passed command. */
    public String[] args;
    /** A reference to the active TPPets instance */
    public TPPets thisPlugin;
    /** Represents the current status when processing the command */
    protected CommandStatus commandStatus = CommandStatus.SUCCESS;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command. Initializes {@link Command#sender} only if it's a player.
     * @param args A list of arguments relevant to the passed command.
     */
    public Command(TPPets thisPlugin, CommandSender sender, String[] args) {
        this.thisPlugin = thisPlugin;
        this.sender = null;
        this.commandFor = null;
        if (sender instanceof Player) {
            this.sender = (Player) sender;
        }
        this.args = args;
    }

    /**
     * Initializes instance variables more directly.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @param sender The sender of the command.
     * @param commandFor The player the command is intended to target.
     * @param args A list of arguments relevant to the passed command.
     */
    public Command(TPPets thisPlugin, Player sender, OfflinePlayer commandFor, String[] args) {
        this.thisPlugin = thisPlugin;
        this.sender = sender;
        this.commandFor = commandFor;
        this.args = args;
    }

    /**
     * Fetches an {@link OfflinePlayer} if they've played on the server before.
     * @param username The player's username.
     * @return The {@link OfflinePlayer} if they've played on the server before. Null if they are either not found, or
     * haven't played.
     */
    @SuppressWarnings("deprecation")
    public OfflinePlayer getOfflinePlayer(String username) {
        if (ArgValidator.softValidateUsername(username)) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(username);
            if (player.hasPlayedBefore()) {
                return player;
            }
        }
        return null;
    }

    /**
     * Loads the chunk a corresponding {@link PetStorage} is in.
     * @param petStorage The {@link PetStorage} to load the corresponding chunk of.
     */
    protected void loadChunkFromPetStorage(PetStorage petStorage) {
        World world = Bukkit.getWorld(petStorage.petWorld);
        if (world != null) {
            Chunk petChunk = world.getChunkAt(petStorage.petX >> 4, petStorage.petZ >> 4);
            petChunk.load();
        }
    }

    /**
     * Gets a given entity from provided {@link PetStorage}'s pet_id in already loaded chunks.
     * @param petStorage The {@link PetStorage} to find on the server.
     * @return The found {@link Entity} if found, null if not.
     */
    protected Entity getEntity(PetStorage petStorage) {
        try {
            String petId = UUIDUtils.unTrimUUID(petStorage.petId);

            if (petId != null) {
                UUID petUUID = UUID.fromString(petId);
                return this.thisPlugin.getServer().getEntity(petUUID);
            }

        } catch (IllegalArgumentException ignored) {}

        return null;
    }

    /**
     * Logs a successful command action to {@link Command#thisPlugin}'s {@link com.maxwellwheeler.plugins.tppets.helpers.LogWrapper}
     * @param commandType A string representing the type of command sent. Ex: "lost add"
     * @param details An optional string representing any further details to log.
     */
    protected void logSuccessfulAction(String commandType, String details) {
        this.thisPlugin.getLogWrapper().logSuccessfulAction((this.sender == null ? "Unknown Sender" : this.sender.getName()) + " - " + commandType + (details == null ? "" : " - " + details));
    }

    /**
     * Logs an unsuccessful command action to {@link Command#thisPlugin}'s {@link com.maxwellwheeler.plugins.tppets.helpers.LogWrapper}
     * @param commandType A string representing the type of command sent. Ex: "lost add"
     * @param errorType An string representing the type of error. Typically a {@link CommandStatus}
     */
    protected void logUnsuccessfulAction(String commandType, String errorType) {
        this.thisPlugin.getLogWrapper().logUnsuccessfulAction((this.sender == null ? "Unknown Sender" : this.sender.getName()) + " - " + commandType + " - " + errorType);
    }

    /**
     * Processes the specific command with its given data.
     */
    public abstract void processCommand();

    /**
     * Gets if the command is being run for the sender or for another player.
     * @return true if {@link Command#sender} is equal to {@link Command#commandFor}, false if not.
     */
    public boolean isForSelf() {
        return this.sender.equals(this.commandFor);
    }

    /**
     * @return Current command status.
     */
    public CommandStatus getCommandStatus() {
        return this.commandStatus;
    }
}
