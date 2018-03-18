package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;

/**
 * A wrapper class that abstracts away the specific database being used.
 * @author GatheringExp
 *
 */
public class DBWrapper {
    private DBFrame database;
    private TPPets thisPlugin;
    /*
     *      TABLES
     */
    private String makeTableUnloadedPets = "CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
            + "pet_id CHAR(32) PRIMARY KEY,\n"
            + "pet_type TINYINT NOT NULL,\n"
            + "pet_x INT NOT NULL,\n"
            + "pet_y INT NOT NULL,\n"
            + "pet_z INT NOT NULL,\n"
            + "pet_world VARCHAR(25) NOT NULL,\n"
            + "owner_id CHAR(32) NOT NULL\n"
            + ");";
    private String makeTableLostRegions = "CREATE TABLE IF NOT EXISTS tpp_lost_regions (\n"
            + "zone_name VARCHAR(64) PRIMARY KEY,\n"
            + "min_x INT NOT NULL,\n"
            + "min_y INT NOT NULL,\n"
            + "min_z INT NOT NULL,\n"
            + "max_x INT NOT NULL,\n"
            + "max_y INT NOT NULL,\n"
            + "max_z INT NOT NULL,\n"
            + "world_name VARCHAR(25) NOT NULL);";
    private String makeTableProtectedRegions = "CREATE TABLE IF NOT EXISTS tpp_protected_regions (\n"
            + "zone_name VARCHAR(64) PRIMARY KEY,\n"
            + "enter_message VARCHAR(255),\n"
            + "min_x INT NOT NULL,\n"
            + "min_y INT NOT NULL,\n"
            + "min_z INT NOT NULL,\n"
            + "max_x INT NOT NULL,\n"
            + "max_y INT NOT NULL,\n"
            + "max_z INT NOT NULL,\n"
            + "world_name VARCHAR(25) NOT NULL,\n"
            + "lf_zone_name VARCHAR(64));";

    /*
     *      UNLOADED_PETS STATEMENTS
     */
    private String insertPet = "INSERT INTO tpp_unloaded_pets(pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private String deletePet = "DELETE FROM tpp_unloaded_pets WHERE pet_id = ? AND owner_id = ?";
    private String updatePetLocation = "UPDATE tpp_unloaded_pets SET pet_x = ?, pet_y = ?, pet_z = ?, pet_world = ? WHERE pet_id = ? AND owner_id = ?";
    private String selectPetFromUuid = "SELECT * FROM tpp_unloaded_pets WHERE pet_id = ?";
    private String updatePetName = "UPDATE tpp_unloaded_pets SET pet_name = ? WHERE owner_id = ? AND pet_name = ?";
    
    private String selectPetsFromOwner = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ?";
    private String selectPetsFromUuids = "SELECT * FROM tpp_unloaded_pets WHERE pet_id = ? AND owner_id = ?";
    private String selectPetsGeneric = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND pet_world = ? AND pet_type = ?";
    private String selectPetsFromWorld = "SELECT * FROM tpp_unloaded_pets WHERE pet_world = ?";
    
    /*
     *      LOST AND FOUND REGION STATEMENTS
     */
    private String insertLost = "INSERT INTO tpp_lost_regions(zone_name, min_x, min_y, min_z, max_x, max_y, max_z, world_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private String deleteLost = "DELETE FROM tpp_lost_regions WHERE zone_name = ?";
    private String selectLost = "SELECT * FROM tpp_lost_regions";
    
    /*
     *      PROTECTED REGION STATEMENTS
     */
    private String insertProtected = "INSERT INTO tpp_protected_regions(zone_name, enter_message, min_x, min_y, min_z, max_x, max_y, max_z, world_name, lf_zone_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private String deleteProtected = "DELETE FROM tpp_protected_regions WHERE zone_name = ?";
    private String selectProtected = "SELECT * FROM tpp_protected_regions";
    private String updateProtected = "UPDATE tpp_protected_regions SET lf_zone_name = ? WHERE zone_name = ?";

    
    /**
     * Creates a MySQLFrame object that executes subsequent sql operations.
     * @param host The host
     * @param port The port number, between 0 and 65535
     * @param dbName The name of the database
     * @param dbUsername The username to use the database
     * @param dbPassword The password of the user
     * @param thisPlugin A reference to the TPPets plugin instance
     */
    public DBWrapper(String host, int port, String dbName, String dbUsername, String dbPassword, TPPets thisPlugin) {
        database = new MySQLFrame(host, port, dbName, dbUsername, dbPassword, thisPlugin);
        this.thisPlugin = thisPlugin;
    }
    
    /**
     * Creates an SQLiteFrame object that executes subsequent sql operations.
     * @param dbPath The path to the object, most often the plugin's directory
     * @param dbName The name of the database
     * @param thisPlugin A reference to the TPPets plugin instance.
     */
    public DBWrapper(String dbPath, String dbName, TPPets thisPlugin) {
        database = new SQLiteFrame(dbPath, dbName, thisPlugin);
        this.thisPlugin = thisPlugin;
    }
    
    /**
     * Creates all needed tables if they don't exist.
     * @return True if successful, false if not.
     */
    public boolean initializeTables() {
        return database.createStatement(makeTableUnloadedPets)
                && database.createStatement(makeTableLostRegions)
                && database.createStatement(makeTableProtectedRegions);
    }
    
    /**
     *      UNLOADED_PETS METHODS
     */
    
    /**
     * Inserts a pet into the tpp_unloaded_pets table
     * @param ent The entity to be inserted, implementing Tameable and Sittable
     * @return True if successful, false if not
     */
    public boolean insertPet(Entity ent) {
        if (ent instanceof Tameable && !PetType.getEnumByEntity(ent).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) ent;
            if (tameableTemp.isTamed() && tameableTemp.getOwner() != null) {
                String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
                String trimmedPlayerUUID = UUIDUtils.trimUUID(tameableTemp.getOwner().getUniqueId());
                int petTypeIndex = PetType.getIndexFromPet(PetType.getEnumByEntity(ent));
                if (database.insertPrepStatement(insertPet, trimmedEntUUID, petTypeIndex, ent.getLocation().getBlockX(), ent.getLocation().getBlockY(), ent.getLocation().getBlockZ(), ent.getWorld().getName(), trimmedPlayerUUID)) {
                    thisPlugin.getLogger().info("Pet with UUID " + trimmedEntUUID + " added to database.");
                    return true;
                } else {
                    thisPlugin.getLogger().info("Pet with UUID " + trimmedEntUUID + " can't be added to database.");
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Deletes a pet from the tpp_unloaded_pets table
     * @param ent The entity to be deleted, implementing Tameable and Sittable
     * @return True if successful, false if not
     */
    public boolean deletePet(Entity ent) {
        if (ent instanceof Tameable && !PetType.getEnumByEntity(ent).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) ent;
            String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
            String trimmedPlayerUUID = UUIDUtils.trimUUID(tameableTemp.getOwner().getUniqueId());
            if (database.deletePrepStatement(deletePet, trimmedEntUUID, trimmedPlayerUUID)) {
                thisPlugin.getLogger().info("Pet with UUID " + trimmedEntUUID + " removed from database.");
                return true;
            } else {
                thisPlugin.getLogger().info("Pet with UUID " + trimmedEntUUID + " can't be removed from database.");
                return false;
            }
        }
        return false;
    }
    
    /**
     * Updates the pet in the tpp_unloaded_pets table
     * @param ent The entity to be updated 
     * @return True if successful, false if not
     */
    public boolean updatePet(Entity ent) {
        if (ent instanceof Tameable && !PetType.getEnumByEntity(ent).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) ent;
            String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
            String trimmedPlayerUUID = UUIDUtils.trimUUID(tameableTemp.getOwner().getUniqueId());
            if (database.updatePrepStatement(updatePetLocation, ent.getLocation().getBlockX(), ent.getLocation().getBlockY(), ent.getLocation().getBlockZ(), ent.getLocation().getWorld().getName(), trimmedEntUUID, trimmedPlayerUUID)) {
                thisPlugin.getLogger().info("Pet with UUID " + trimmedEntUUID + " updated in database.");
                return true;
            } else {
                thisPlugin.getLogger().info("Pet with UUID " + trimmedEntUUID + " can't be updated in database.");
                return false;
            }
        }
        return false;
    }
    
    /**
     * Updates OR inserts the pet, based on whether or not it's already in the table tpp_unloaded_pets. This is recommended to be used whenever possible.
     * @param ent The entity to be updated.
     * @return True if successful, false if not
     */
    public boolean updateOrInsertPet(Entity ent) {
        if (ent instanceof Tameable && !PetType.getEnumByEntity(ent).equals(PetType.Pets.UNKNOWN)) {
            if (petInTable(ent)) {
                return updatePet(ent);
            }
            return insertPet(ent);
        }
        return false;
    }
    
    /**
     * Checks if the pet is already in the table tpp_unloaded_pets
     * @param ent The entity to be checked
     * @return True if the entity is in the table, false if not
     */
    public boolean petInTable(Entity ent) {
        String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                boolean ret = database.selectPrepStatement(dbConn, selectPetFromUuid, trimmedEntUUID).next();
                dbConn.close();
                return ret;
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception selecting pet from table: " + e.getMessage());
            }
        }
        return false;
    }

    public boolean renamePet(String ownerUUID, String oldName, String newName) {
        String trimmedOwnerUUID = UUIDUtils.trimUUID(ownerUUID);
        if (database.updatePrepStatement(updatePetName, newName, trimmedOwnerUUID, oldName)) {
            thisPlugin.getLogger().info("Player with UUID " + ownerUUID + " renamed pet with name " + oldName + " to " + newName);
            return true;
        } else {
            thisPlugin.getLogger().info("Unable to execute: Player with UUID " + ownerUUID + " renamed pet with name " + oldName + " to " + newName);
            return false;
        }
    }

    /**
     * Gets a list of pets from storage based on the owner's UUID.
     * @param uuid The trimmed or non-trimmed UUID string of the owner.
     * @return A list of pets from storage that are owned by the provided UUID.
     */
    public List<PetStorage> getPetsFromOwner(String uuid) { 
        String trimmedUuid = UUIDUtils.trimUUID(uuid);
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            List<PetStorage> ret = getPetsList(database.selectPrepStatement(dbConn, selectPetsFromOwner, trimmedUuid));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception getting pets from owner: " + e.getMessage());
            }
            return ret;
        }
        return null;
    }
    
    /**
     * Gets a list of pets from storage based on the pet's UUID and owner's UUID
     * @param petUuid The trimmed or non-trimmed UUID string of the pet.
     * @param playerUuid The trimmed or non-trimmed UUID string of the owner.
     * @return A list of pets from storage that have the uuid PetUuid and are owned by playerUuid
     */
    public List<PetStorage> getPetsFromUUIDs(String petUuid, String playerUuid) {
        String trimmedPetUuid = UUIDUtils.trimUUID(petUuid);
        String trimmedPlayerUuid = UUIDUtils.trimUUID(playerUuid);
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            List<PetStorage> ret = getPetsList(database.selectPrepStatement(dbConn, selectPetsFromUuids, trimmedPetUuid, trimmedPlayerUuid));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception getting pets from UUIDs: " + e.getMessage());
            }
            return ret;
        }
        return null;
    }
    
    /**
     * Gets a list of pets from storage based on the owner's UUId, world, and type of pets. Useful for teleporting pets from unloaded chunks.
     * @param playerUuid The trimmed or non-trimmed UUID string of the owner.
     * @param worldName The name of the world to be searched.
     * @param petType The type of pet to be searched for.
     * @return A list of pets from storage that are owned by the playerUuid, are in the worldName, and are of type petType.
     */
    public List<PetStorage> getPetsGeneric(String playerUuid, String worldName, PetType.Pets petType) {
        String trimmedPlayerUuid = UUIDUtils.trimUUID(playerUuid);
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            List<PetStorage> ret = getPetsList(database.selectPrepStatement(dbConn, selectPetsGeneric, trimmedPlayerUuid, worldName, PetType.getIndexFromPet(petType)));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception getting pets: " + e.getMessage());
            }
            return ret;
        }
        return null;
    }
    
    /**
     * Gets a list of pets from storage based on the world.
     * @param worldName The name of the world to be searched.
     * @return A list of pets from storage that are in the worldName.
     */
    public List<PetStorage> getPetsFromWorld(String worldName) {
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            List<PetStorage> ret = getPetsList(database.selectPrepStatement(dbConn, selectPetsFromWorld, worldName));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception getting pets from world: " + e.getMessage());
            }
            return ret;
        }
        return null;
    }
    
    /**
     * Gets a list of pets from storage based on a given ResultSet. This is used in the processing of most other getPets-type methods.
     * @param rs The ResultSet to analyze, presumably from the tpp_unloaded_pets table.
     * @return A list of pets created from rs.
     */
    private List<PetStorage> getPetsList(ResultSet rs) {
        List<PetStorage> ret = new ArrayList<PetStorage>();
        try {
            while (rs.next()) {
                ret.add(new PetStorage(rs.getString("pet_id"), rs.getInt("pet_type"), rs.getInt("pet_x"), rs.getInt("pet_y"), rs.getInt("pet_z"), rs.getString("pet_world"), rs.getString("owner_id"), rs.getString("pet_name")));
            }
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception generating list from database results", e.getMessage());
        }
        return ret;
    }
    
    /*
     *      LOST AND FOUND REGION STATEMENTS
     */
    
    /**
     * Inserts a {@link LostAndFoundRegion} into the database.
     * @param lfr The {@link LostAndFoundRegion} instance to add to the database.
     * @return True if successful, false if not
     */
    public boolean insertLostRegion(LostAndFoundRegion lfr) {
        if (database.insertPrepStatement(insertLost, lfr.getZoneName(), lfr.getMinLoc().getBlockX(), lfr.getMinLoc().getBlockY(), lfr.getMinLoc().getBlockZ(), lfr.getMaxLoc().getBlockX(), lfr.getMaxLoc().getBlockY(), lfr.getMaxLoc().getBlockZ(), lfr.getWorldName())) {
            thisPlugin.getLogger().info("Lost and found region " + lfr.getZoneName() + " added to database.");
            return true;
        } else {
            thisPlugin.getLogger().info("Lost and found region " + lfr.getZoneName() + " can't be added to database.");
            return false;
        }
    }
    
    /**
     * Removes a {@link LostAndFoundRegion} from the database.
     * @param lfr The {@link LostAndFoundRegion} instance to remove from the database.
     * @return True if successful, false if not.
     */
    public boolean deleteLostRegion(LostAndFoundRegion lfr) {
        if (database.deletePrepStatement(deleteLost, lfr.getZoneName())) {
            thisPlugin.getLogger().info("Lost and found region " + lfr.getZoneName() + " removed from database.");
            return true;
        } else {
            thisPlugin.getLogger().info("Lost and found region " + lfr.getZoneName() + " can't be removed from database.");
            return false;
        }
    }
    
    /**
     * Gets the Hashtable used by the plugin internally to store the {@link LostAndFoundRegion} in memory from the database.
     * @return The hashtable of <LostAndFoundRegion's name, LostAndFoundRegion instance>
     */
    public Hashtable<String, LostAndFoundRegion> getLostRegions() {
        Hashtable<String, LostAndFoundRegion> ret = new Hashtable<String, LostAndFoundRegion>();
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                ResultSet rs = database.selectPrepStatement(dbConn, selectLost);
                while (rs.next()) {
                    ret.put(rs.getString("zone_name"), new LostAndFoundRegion(rs.getString("zone_name"), rs.getString("world_name"), rs.getInt("min_x"), rs.getInt("min_y"), rs.getInt("min_z"), rs.getInt("max_x"), rs.getInt("max_y"), rs.getInt("max_z")));
                }
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception getting lost and found regions: " + e.getMessage());
            }
        }
        return ret;
    }
    
    /*
     *      PROTECTED REGION STATEMENTS
     */
    
    /**
     * Inserts a {@link ProtectedRegion} into the database.
     * @param pr The {@link ProtectedRegion} instance to add to the database.
     * @return True if successful, false if not
     */
    public boolean insertProtectedRegion(ProtectedRegion pr) {
        if (database.insertPrepStatement(insertProtected, pr.getZoneName(), pr.getEnterMessage(), pr.getMinLoc().getBlockX(), pr.getMinLoc().getBlockY(), pr.getMinLoc().getBlockZ(), pr.getMaxLoc().getBlockX(), pr.getMaxLoc().getBlockY(), pr.getMaxLoc().getBlockZ(), pr.getWorldName(), pr.getLfName())) {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " added to database.");
            return true;
        } else {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " can't be added to database.");
            return false;
        }
    }
    
    /**
     * Removes a {@link ProtectedRegion} from the database.
     * @param pr The {@link ProtectedRegion} instance to remove from the database.
     * @return True if successful, false if not.
     */
    public boolean deleteProtectedRegion(ProtectedRegion pr) {
        if (database.deletePrepStatement(deleteProtected, pr.getZoneName())) {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " removed from database.");
            return true;
        } else {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " can't be removed from database.");
            return false;
        }
    }
    
    /**
     * Gets the Hashtable used by the plugin internally to store the {@link ProtectedRegion} in memory from the database.
     * @return The hashtable of <ProtectedRegion's name, ProtectedRegion instance>
     */
    public Hashtable<String, ProtectedRegion> getProtectedRegions() {
        Hashtable<String, ProtectedRegion> ret = new Hashtable<String, ProtectedRegion>();
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                ResultSet rs = database.selectPrepStatement(dbConn, selectProtected);
                while (rs.next()) {
                    ret.put(rs.getString("zone_name"), new ProtectedRegion(rs.getString("zone_name"), rs.getString("enter_message"), rs.getString("world_name"), rs.getInt("min_x"), rs.getInt("min_y"), rs.getInt("min_z"), rs.getInt("max_x"), rs.getInt("max_y"), rs.getInt("max_z"), rs.getString("lf_zone_name")));
                }
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception getting protected regions" + e.getMessage());
            }
        }
        return ret;
    }
    
    /**
     * Updates the given {@link ProtectedRegion}'s lfName in the database.
     * @param pr The {@link ProtectedRegion} whose lfName is to be updated.
     * @return True if successful, false if not.
     */
    public boolean updateProtectedRegion(ProtectedRegion pr) {
        if (database.updatePrepStatement(updateProtected, pr.getLfName(), pr.getZoneName())) {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " updated in database.");
            return true;
        } else {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " can't be updated in database.");
            return true;
        }
    }
    
    /**
     * @return The underlying database object, either of type {@link MySQLFrame} or {@link SQLiteFrame}
     */
    public DBFrame getRealDatabase() {
        return database;
    }
}
