package com.maxwellwheeler.plugins.tppets.commands;


import com.maxwellwheeler.plugins.tppets.TPPets;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Template for {@link CommandProtected} and {{@link CommandLost}
 * @author GatheringExp
 *
 */
abstract class RegionCommand {
    protected TPPets thisPlugin;
    protected WorldEditPlugin we;
    
    /**
     * Gets plugin instance and worldedit instance from Bukkit object
     */
    RegionCommand() {
        thisPlugin = (TPPets) Bukkit.getServer().getPluginManager().getPlugin("TPPets");
        we = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
    }
    
    /**
     * Generates an array of locations with length 2, containing endpoints of a player's CuboidSelection
     * @param pl Player to check WorldEdit data for.
     * @return An array of length 2 containing the minimum point, maximum point data for a player if they have a CuboidSelection. Null otherwise.
     */
    protected Location[] getWePoints(Player pl) {
        Selection playerSelection = we.getSelection(pl);
        Location[] ret = null;
        if (playerSelection instanceof CuboidSelection) {
            ret = new Location[] {playerSelection.getMinimumPoint(), playerSelection.getMaximumPoint()};
        }
        return ret;
    }
    
    /**
     * Creates a formatted string of {@link Location} data
     * @param lc The location data from which a string will be created.
     * @return The formatted string.
     */
    protected String getLocationString(Location lc) {
        return Integer.toString(lc.getBlockX()) + ", " + Integer.toString(lc.getBlockY()) + ", " + Integer.toString(lc.getBlockZ());
    }
    
    /**
     * Processes the command passed to it
     * @param sender The CommandSender object that originally sent the command.
     * @param args The arguments passed with the command - doesn't include the "tpp [command type]" in command. That's implied by the implementing object.
     * Ex: /tpp protected add PrimaryProtected PrimaryLost You can't do that here, String args[] would have {add PrimaryProtected PrimaryLost You can't do that here}.
     */
    abstract public void processCommand(CommandSender sender, String[] args);
    
    /**
     * Performs all region adding actions, including adding the region to memory and disk.
     * @param sender The {@link CommandSender} that ran the command.
     * @param truncatedArgs A truncated list of arguments passed with the addRegion command. For command /tpp lostandfound add PrimaryLost, this includes only PrimaryLost
     */
    abstract protected void addRegion(CommandSender sender, String[] truncatedArgs);
    
    /**
     * Performs all region removing actions, including removing the region from memory and disk.
     * @param sender The {@link CommandSender} that ran the command.
     * @param truncatedArgs A truncated list of arguments passed with the addRegion command. For command /tpp lostandfound remove PrimaryLost, this includes only PrimaryLost
     */
    abstract protected void removeRegion(CommandSender sender, String[] truncatedArgs);
    
    /**
     * Lists regions to sender in chat. If args[0] is specified, it will only list that {@link com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion}'s data
     * @param sender The {@link CommandSender} that ran the command.
     * @param truncatedArgs A truncated list of arguments passed with the addRegion command. For command /tpp lostandfound list PrimaryLost, it would include PrimaryLost.
     */
    abstract protected void listRegions(CommandSender sender, String[] truncatedArgs);
}
