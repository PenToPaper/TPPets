package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * A wrapper class that abstracts away the specific database being used.
 * @author GatheringExp
 *
 */
public class DBWrapper {
    private final DBFrame database;
    private final TPPets thisPlugin;


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
        this.database = new MySQLFrame(host, port, dbName, dbUsername, dbPassword, thisPlugin);
        this.thisPlugin = thisPlugin;
    }
    
    /**
     * Creates an SQLiteFrame object that executes subsequent sql operations.
     * @param dbPath The path to the object, most often the plugin's directory
     * @param dbName The name of the database
     * @param thisPlugin A reference to the TPPets plugin instance.
     */
    public DBWrapper(String dbPath, String dbName, TPPets thisPlugin) {
        this.database = new SQLiteFrame(dbPath, dbName, thisPlugin);
        this.thisPlugin = thisPlugin;
    }
    
    /**
     * Creates all needed tables if they don't exist.
     * @return True if successful, false if not.
     */
    public boolean initializeTables() throws SQLException {
        String makeTableAllowedPlayers = "CREATE TABLE IF NOT EXISTS tpp_allowed_players(" +
                "pet_id CHAR(32),\n" +
                "user_id CHAR(32),\n" +
                "PRIMARY KEY(pet_id, user_id),\n" +
                "FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);";
        String makeTableDBVersion = "CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);";
        String makeTableProtectedRegions = "CREATE TABLE IF NOT EXISTS tpp_protected_regions (\n"
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
        String makeTableLostRegions = "CREATE TABLE IF NOT EXISTS tpp_lost_regions (\n"
                + "zone_name VARCHAR(64) PRIMARY KEY,\n"
                + "min_x INT NOT NULL,\n"
                + "min_y INT NOT NULL,\n"
                + "min_z INT NOT NULL,\n"
                + "max_x INT NOT NULL,\n"
                + "max_y INT NOT NULL,\n"
                + "max_z INT NOT NULL,\n"
                + "world_name VARCHAR(25) NOT NULL);";
        String makeTableUnloadedPets = "CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)\n,"
                + "effective_pet_name VARCHAR(64)"
                + ");";
        String makeTableUserStorageLocations = "CREATE TABLE IF NOT EXISTS tpp_user_storage_locations (\n" +
                "user_id CHAR(32) NOT NULL, \n" +
                "storage_name VARCHAR(64) NOT NULL, \n" +
                "effective_storage_name VARCHAR(64) NOT NULL," +
                "loc_x INT NOT NULL, \n" +
                "loc_y INT NOT NULL, \n" +
                "loc_z INT NOT NULL, \n" +
                "world_name VARCHAR(25) NOT NULL, \n" +
                "PRIMARY KEY (user_id, effective_storage_name))";
        String makeTableDefaultStorageLocations = "CREATE TABLE IF NOT EXISTS tpp_server_storage_locations (\n" +
                "storage_name VARCHAR(64) NOT NULL, \n" +
                "effective_storage_name VARCHAR(64) NOT NULL, \n" +
                "loc_x INT NOT NULL, \n" +
                "loc_y INT NOT NULL, \n" +
                "loc_z INT NOT NULL, \n" +
                "world_name VARCHAR(25) NOT NULL, \n" +
                "PRIMARY KEY (effective_storage_name, world_name))";
        return database.createStatement(makeTableUnloadedPets)
                && database.createStatement(makeTableLostRegions)
                && database.createStatement(makeTableProtectedRegions)
                && database.createStatement(makeTableDBVersion)
                && database.createStatement(makeTableAllowedPlayers)
                && database.createStatement(makeTableUserStorageLocations)
                && database.createStatement(makeTableDefaultStorageLocations)
                && thisPlugin.getDatabaseUpdater().updateSchemaVersion(this);
    }
    
    /*
     *      UNLOADED_PETS METHODS
     */

    /**
     * Checks if a name is unique to a player
     * @param ownerUUID The player to check
     * @param petName The pet name to check
     * @return True if it's unique, false if it's not
     */
    public boolean isNameUnique(String ownerUUID, String petName) throws SQLException {
        String trimmedOwnerUUID = UUIDUtils.trimUUID(ownerUUID);
        String selectIsNameUnique = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?";

        try (Connection dbConn = this.database.getConnection();
             PreparedStatement selectStatement = this.database.setPreparedStatementArgs(dbConn.prepareStatement(selectIsNameUnique), trimmedOwnerUUID, petName);
             ResultSet resultSet = selectStatement.executeQuery()) {
            return resultSet.next();

        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("SQL Exception checking if pet name is unique: " + exception.getMessage());
            throw exception;

        }
    }

    /**
     * Inserts a pet into the tpp_unloaded_pets table
     * @param ent The entity to be inserted, implementing Tameable
     * @return True if successful, false if not
     */
    public boolean insertPet(Entity ent) {
        if (ent instanceof Tameable && !PetType.getEnumByEntity(ent).equals(PetType.Pets.UNKNOWN)) {
            Tameable tameableTemp = (Tameable) ent;
            if (tameableTemp.isTamed() && tameableTemp.getOwner() != null) {
                return insertPet(ent, tameableTemp.getOwner().getUniqueId().toString()) != null;
            }
        }
        return false;
    }

    public String insertPet(Entity ent, String ownerUUID) {
        if (ent instanceof Tameable && !PetType.getEnumByEntity(ent).equals(PetType.Pets.UNKNOWN)) {
            if (ownerUUID != null) {
                String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
                String trimmedPlayerUUID = UUIDUtils.trimUUID(ownerUUID);
                int petTypeIndex = PetType.getIndexFromPet(PetType.getEnumByEntity(ent));
                String uniqueName = generateUniqueName(trimmedPlayerUUID, PetType.getEnumByEntity((ent)));
                /*
                 *      UNLOADED_PETS STATEMENTS
                 */
                String insertPet = "INSERT INTO tpp_unloaded_pets(pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id, pet_name, effective_pet_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                if (uniqueName != null && database.insertPrepStatement(insertPet, trimmedEntUUID, petTypeIndex, ent.getLocation().getBlockX(), ent.getLocation().getBlockY(), ent.getLocation().getBlockZ(), ent.getWorld().getName(), trimmedPlayerUUID, uniqueName, uniqueName.toLowerCase())) {
                    thisPlugin.getLogWrapper().logUpdatedPet("Pet with UUID " + trimmedEntUUID + " added to database.");
                    return uniqueName;
                } else {
                    thisPlugin.getLogWrapper().logUnsuccessfulAction("Pet with UUID " + trimmedEntUUID + " can't be added to database.");
                }
            }
        }
        return null;
    }

    /**
     * Generates a unique name for a player's pet. Used for default name creation.
     * @param ownerUUID The Owner of the pet to generate a name for
     * @param pt The type of pet to generate a name for
     * @return A name of the format pt.toString() + an integer
     */
    public String generateUniqueName(String ownerUUID, PetType.Pets pt) {
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            List<PetStorage> psList = getPetsFromOwner(ownerUUID);
            int lastIndexChecked = psList.size();
            String ret;
            do {
                ret = pt.toString() + Integer.toString(lastIndexChecked);
                lastIndexChecked++;
            } while (!isNameUnique(ownerUUID, ret));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception generating name: " + e.getMessage());
            }
            return ret;
        }
        return null;
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
            String deletePet = "DELETE FROM tpp_unloaded_pets WHERE pet_id = ? AND owner_id = ?";
            if (database.deletePrepStatement(deletePet, trimmedEntUUID, trimmedPlayerUUID)) {
                thisPlugin.getLogWrapper().logSuccessfulAction("Pet with UUID " + trimmedEntUUID + " removed from database.");
                return true;
            } else {
                thisPlugin.getLogWrapper().logUnsuccessfulAction("Pet with UUID " + trimmedEntUUID + " can't be removed from database.");
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
            String updatePetLocation = "UPDATE tpp_unloaded_pets SET pet_x = ?, pet_y = ?, pet_z = ?, pet_world = ? WHERE pet_id = ? AND owner_id = ?";
            if (database.updatePrepStatement(updatePetLocation, ent.getLocation().getBlockX(), ent.getLocation().getBlockY(), ent.getLocation().getBlockZ(), ent.getLocation().getWorld().getName(), trimmedEntUUID, trimmedPlayerUUID)) {
                thisPlugin.getLogWrapper().logUpdatedPet("Pet with UUID " + trimmedEntUUID + " updated in database.");
                return true;
            } else {
                thisPlugin.getLogWrapper().logUnsuccessfulAction("Pet with UUID " + trimmedEntUUID + " can't be updated in database.");
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
    private boolean petInTable(Entity ent) {
        String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                String selectPetFromUuid = "SELECT * FROM tpp_unloaded_pets WHERE pet_id = ?";
                boolean ret = database.selectPrepStatement(dbConn, selectPetFromUuid, trimmedEntUUID).next();
                dbConn.close();
                return ret;
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception selecting pet from table: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Renames the pet by updating the pet's entry in the database.
     * @param ownerUUID The owner of the pet's UUID
     * @param oldName The pet's old name, used for finding the existing pet to update
     * @param newName The pet's new name
     * @return True if successful, false if not
     */
    public boolean renamePet(String ownerUUID, String oldName, String newName) {
        String trimmedOwnerUUID = UUIDUtils.trimUUID(ownerUUID);
        String updatePetName = "UPDATE tpp_unloaded_pets SET pet_name = ?, effective_pet_name = ? WHERE owner_id = ? AND effective_pet_name = ?";
        if (database.updatePrepStatement(updatePetName, newName, newName.toLowerCase(), trimmedOwnerUUID, oldName.toLowerCase())) {
            thisPlugin.getLogWrapper().logSuccessfulAction("Player with UUID " + ownerUUID + " has had their pet " + oldName + " renamed to " + newName);
            return true;
        } else {
            thisPlugin.getLogWrapper().logUnsuccessfulAction("Unable to execute: Player with UUID " + ownerUUID + " renamed pet with name " + oldName + " to " + newName);
            return false;
        }
    }

    /**
     * Gets a list of trimmed uuids of players that are allowed to a given pet
     * @param entityUUID The pet's UUID
     * @return A list of trimmed UUID strings of players allowed to the pet
     */
    public List<String> getAllowedPlayers(String entityUUID) {
        String trimmedEntityUUID = UUIDUtils.trimUUID(entityUUID);
        Connection dbConn = database.getConnection();
        List<String> uuidList = new ArrayList<>();
        if (dbConn != null) {
            try {
                String selectAllowedPlayers = "SELECT * FROM tpp_allowed_players WHERE pet_id = ?";
                ResultSet ret = database.selectPrepStatement(dbConn, selectAllowedPlayers, trimmedEntityUUID);
                while (ret.next()) {
                    uuidList.add(ret.getString("user_id"));
                }
                dbConn.close();
                return uuidList;
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting allowed players from table: " + e.getMessage());
            }
        }
        return uuidList;
    }

    /**
     * Gets a list of {@link PetStorage}s representing pets in unloaded chunks. Gets all pets with petName, ownerUUID, and type pt
     * @param ownerUUID The pet owner's UUID
     * @param petName The pet's name
     * @param pt The pet's type
     * @return A list of {@link PetStorage}s representing pets in unloaded chunks
     */
    public List<PetStorage> getPetsFromOwnerNamePetType(String ownerUUID, String petName, PetType.Pets pt) {
        String trimmedOwnerUUID = UUIDUtils.trimUUID(ownerUUID);
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            String selectPetsFromOwnerNamePetType = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ? AND pet_type = ?";
            List<PetStorage> ret = getPetsList(database.selectPrepStatement(dbConn, selectPetsFromOwnerNamePetType, trimmedOwnerUUID, petName.toLowerCase(), PetType.getIndexFromPet(pt)));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting pets from owner and name: " + e.getMessage());
            }
            return ret;
        }
        return null;
    }

    /**
     * Gets a trimmed version of the pet's UUID based on its owner's UUID and pet name
     * @param ownerUUID The owner of the pet's UUID represented as a string. Does not need to be trimmed.
     * @param petName The pet's name
     * @return A trimmed string version of the pet's UUID. Returns null if the database connection can't be made, returns empty string if can't find pet with that name from that owner
     */
    // Returns null if connection can't be made, returns "" if no pet by that name exists, returns pet UUID if a pet is found.
    public List<PetStorage> getPetByName(String ownerUUID, String petName) {
        String trimmedOwnerUUID = UUIDUtils.trimUUID(ownerUUID);
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            String selectPetFromName = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ? LIMIT 1";
            try (ResultSet rs = database.selectPrepStatement(dbConn, selectPetFromName, trimmedOwnerUUID, petName.toLowerCase())) {
                List<PetStorage> petsList = getPetsList(rs);
                dbConn.close();
                if (petsList.size() == 1) {
                    return petsList;
                }
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting pet UUID from name: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Inserts an allowed player into the tpp_allowed_players table
     * @param petUUID The pet's UUID
     * @param playerUUID The player's UUID
     * @return True if successful, false if not
     */
    public boolean insertAllowedPlayer(String petUUID, String playerUUID) {
        String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
        String trimmedPetUUID = UUIDUtils.trimUUID(petUUID);
        String insertAllowedPlayer = "INSERT INTO tpp_allowed_players (pet_id, user_id) VALUES (?, ?)";
        return database.insertPrepStatement(insertAllowedPlayer, trimmedPetUUID, trimmedPlayerUUID);
    }

    /**
     * Deletes an allowed player from the tpp_allowed_players table
     * @param petUUID The pet's UUID
     * @param playerUUID The player's UUID
     * @return True if successful, false if not
     */
    public boolean removeAllowedPlayer(String petUUID, String playerUUID) {
        String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
        String trimmedPetUUID = UUIDUtils.trimUUID(petUUID);
        String deleteAllowedPlayer = "DELETE FROM tpp_allowed_players WHERE pet_id = ? AND user_id = ?";
        return database.deletePrepStatement(deleteAllowedPlayer, trimmedPetUUID, trimmedPlayerUUID);
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
            String selectPetsFromOwner = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ?";
            List<PetStorage> ret = getPetsList(database.selectPrepStatement(dbConn, selectPetsFromOwner, trimmedUuid));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting pets from owner: " + e.getMessage());
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
            String selectPetsFromUuids = "SELECT * FROM tpp_unloaded_pets WHERE pet_id = ? AND owner_id = ?";
            List<PetStorage> ret = getPetsList(database.selectPrepStatement(dbConn, selectPetsFromUuids, trimmedPetUuid, trimmedPlayerUuid));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting pets from UUIDs: " + e.getMessage());
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
            String selectPetsGeneric = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND pet_world = ? AND pet_type = ?";
            List<PetStorage> ret = getPetsList(database.selectPrepStatement(dbConn, selectPetsGeneric, trimmedPlayerUuid, worldName, PetType.getIndexFromPet(petType)));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting pets: " + e.getMessage());
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
            String selectPetsFromWorld = "SELECT * FROM tpp_unloaded_pets WHERE pet_world = ?";
            List<PetStorage> ret = getPetsList(database.selectPrepStatement(dbConn, selectPetsFromWorld, worldName));
            try {
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting pets from world: " + e.getMessage());
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
        List<PetStorage> ret = new ArrayList<>();
        try {
            while (rs.next()) {
                ret.add(new PetStorage(rs.getString("pet_id"), rs.getInt("pet_type"), rs.getInt("pet_x"), rs.getInt("pet_y"), rs.getInt("pet_z"), rs.getString("pet_world"), rs.getString("owner_id"), rs.getString("pet_name"), rs.getString("effective_pet_name")));
            }
        } catch (SQLException e) {
            thisPlugin.getLogWrapper().logErrors("SQL Exception generating list from database results" + e.getMessage());
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
        /*
         *      LOST AND FOUND REGION STATEMENTS
         */
        String insertLost = "INSERT INTO tpp_lost_regions(zone_name, min_x, min_y, min_z, max_x, max_y, max_z, world_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        if (database.insertPrepStatement(insertLost, lfr.getRegionName(), lfr.getMinLoc().getBlockX(), lfr.getMinLoc().getBlockY(), lfr.getMinLoc().getBlockZ(), lfr.getMaxLoc().getBlockX(), lfr.getMaxLoc().getBlockY(), lfr.getMaxLoc().getBlockZ(), lfr.getWorldName())) {
            thisPlugin.getLogWrapper().logSuccessfulAction("Lost and found region " + lfr.getRegionName() + " added to database.");
            return true;
        } else {
            thisPlugin.getLogWrapper().logUnsuccessfulAction("Lost and found region " + lfr.getRegionName() + " can't be added to database.");
            return false;
        }
    }

    public boolean removeLostRegion(String regionName) {
        String deleteLost = "DELETE FROM tpp_lost_regions WHERE zone_name = ?";
        if (database.deletePrepStatement(deleteLost, regionName)) {
            thisPlugin.getLogWrapper().logSuccessfulAction("Lost and found region " + regionName + " removed from database.");
            return true;
        } else {
            thisPlugin.getLogWrapper().logUnsuccessfulAction("Lost and found region " + regionName + " can't be removed from database.");
            return false;
        }
    }

    public LostAndFoundRegion getLostRegion(String lfrName) throws SQLException {
        LostAndFoundRegion ret = null;
        Connection dbConn = database.getConnection();
        if (dbConn == null) {
            throw new SQLException();
        }

        try {
            String selectLostRegion = "SELECT * FROM tpp_lost_regions WHERE zone_name = ?";
            ResultSet rs = database.selectPrepStatement(dbConn, selectLostRegion, lfrName);
            if (rs.next()) {
                ret = new LostAndFoundRegion(rs.getString("zone_name"), rs.getString("world_name"), rs.getInt("min_x"), rs.getInt("min_y"), rs.getInt("min_z"), rs.getInt("max_x"), rs.getInt("max_y"), rs.getInt("max_z"));
            }
            dbConn.close();
        } catch (SQLException e) {
            thisPlugin.getLogWrapper().logErrors("SQL Exception getting lost and found regions: " + e.getMessage());
            throw e;
        }

        return ret;
    }

    public ProtectedRegion getProtectedRegion(String prName) throws SQLException {
        ProtectedRegion ret = null;
        Connection dbConn = database.getConnection();
        if (dbConn == null) {
            throw new SQLException();
        }

        try {
            String selectProtectedRegion = "SELECT * FROM tpp_protected_regions WHERE zone_name = ?";
            ResultSet rs = database.selectPrepStatement(dbConn, selectProtectedRegion, prName);
            if (rs.next()) {
                ret = new ProtectedRegion(rs.getString("zone_name"), rs.getString("enter_message"), rs.getString("world_name"), rs.getInt("min_x"), rs.getInt("min_y"), rs.getInt("min_z"), rs.getInt("max_x"), rs.getInt("max_y"), rs.getInt("max_z"), rs.getString("lf_zone_name"), this.thisPlugin);
            }
            dbConn.close();
        } catch (SQLException e) {
            thisPlugin.getLogWrapper().logErrors("SQL Exception getting lost and found regions: " + e.getMessage());
            throw e;
        }

        return ret;
    }

    /**
     * Gets the {@link Hashtable} used by the plugin internally to store the {@link LostAndFoundRegion} in memory from the database.
     * @return The {@link Hashtable} of &#60;LostAndFoundRegion's name, LostAndFoundRegion instance&#62;
     */
    public Hashtable<String, LostAndFoundRegion> getLostRegions() {
        Hashtable<String, LostAndFoundRegion> ret = new Hashtable<>();
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                String selectLost = "SELECT * FROM tpp_lost_regions";
                ResultSet rs = database.selectPrepStatement(dbConn, selectLost);
                while (rs.next()) {
                    ret.put(rs.getString("zone_name"), new LostAndFoundRegion(rs.getString("zone_name"), rs.getString("world_name"), rs.getInt("min_x"), rs.getInt("min_y"), rs.getInt("min_z"), rs.getInt("max_x"), rs.getInt("max_y"), rs.getInt("max_z")));
                }
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting lost and found regions: " + e.getMessage());
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
        /*
         *      PROTECTED REGION STATEMENTS
         */
        String insertProtected = "INSERT INTO tpp_protected_regions(zone_name, enter_message, min_x, min_y, min_z, max_x, max_y, max_z, world_name, lf_zone_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        if (database.insertPrepStatement(insertProtected, pr.getRegionName(), pr.getEnterMessage(), pr.getMinLoc().getBlockX(), pr.getMinLoc().getBlockY(), pr.getMinLoc().getBlockZ(), pr.getMaxLoc().getBlockX(), pr.getMaxLoc().getBlockY(), pr.getMaxLoc().getBlockZ(), pr.getWorldName(), pr.getLfName())) {
            thisPlugin.getLogWrapper().logSuccessfulAction("Protected region " + pr.getRegionName() + " added to database.");
            return true;
        } else {
            thisPlugin.getLogWrapper().logUnsuccessfulAction("Protected region " + pr.getRegionName() + " can't be added to database.");
            return false;
        }
    }
    
    /**
     * Removes a {@link ProtectedRegion} from the database.
     * @param pr The {@link ProtectedRegion} instance to remove from the database.
     * @return True if successful, false if not.
     */
    public boolean removeProtectedRegion(String regionName) {
        String removeProtected = "DELETE FROM tpp_protected_regions WHERE zone_name = ?";
        if (database.deletePrepStatement(removeProtected, regionName)) {
            thisPlugin.getLogWrapper().logSuccessfulAction("Protected region " + regionName + " removed from database.");
            return true;
        } else {
            thisPlugin.getLogWrapper().logUnsuccessfulAction("Protected region " + regionName + " can't be removed from database.");
            return false;
        }
    }
    
    /**
     * Gets the {@link Hashtable} used by the plugin internally to store the {@link ProtectedRegion} in memory from the database.
     * @return The {@link Hashtable} of &#60;ProtectedRegion's name, ProtectedRegion instance&#62;
     */
    public Hashtable<String, ProtectedRegion> getProtectedRegions() {
        Hashtable<String, ProtectedRegion> ret = new Hashtable<>();
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                String selectProtected = "SELECT * FROM tpp_protected_regions";
                ResultSet rs = database.selectPrepStatement(dbConn, selectProtected);
                while (rs.next()) {
                    ret.put(rs.getString("zone_name"), new ProtectedRegion(rs.getString("zone_name"), rs.getString("enter_message"), rs.getString("world_name"), rs.getInt("min_x"), rs.getInt("min_y"), rs.getInt("min_z"), rs.getInt("max_x"), rs.getInt("max_y"), rs.getInt("max_z"), rs.getString("lf_zone_name"), this.thisPlugin));
                }
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting protected regions: " + e.getMessage());
            }
        }
        return ret;
    }

    /**
     * Gets a {@link Hashtable} of &#60;Trimmed Pet UUID, List&#60;Trimmed Player UUID&#62;&#62; representing the players that are allowed to all pets. Used in TPPets initialization process
     * @return {@link Hashtable} of &#60;Trimmed Pet UUID, List&#60;Trimmed Player UUID&#62;&#62;. It will never be null, but it could be empty
     */
    public Hashtable<String, List<String>> getAllAllowedPlayers() {
        Hashtable<String, List<String>> ret = new Hashtable<>();
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                String selectAllAllowedPlayers = "SELECT * FROM tpp_allowed_players ORDER BY pet_id";
                ResultSet rs = database.selectPrepStatement(dbConn, selectAllAllowedPlayers);
                String petID = "";
                List<String> allowedPlayersID = new ArrayList<>();
                while (rs.next()) {
                    if (!petID.equals(rs.getString("pet_id"))) {
                        ret.put(petID, allowedPlayersID);
                        petID = rs.getString("pet_id");
                        allowedPlayersID = new ArrayList<>();
                    }
                    allowedPlayersID.add(rs.getString("user_id"));
                }
                ret.put(petID, allowedPlayersID);
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQL Exception getting allowed players: " + e.getMessage());
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
        String updateProtected = "UPDATE tpp_protected_regions SET lf_zone_name = ? WHERE zone_name = ?";
        if (database.updatePrepStatement(updateProtected, pr.getLfName(), pr.getRegionName())) {
            thisPlugin.getLogWrapper().logSuccessfulAction("Protected region " + pr.getRegionName() + " updated in database.");
            return true;
        } else {
            thisPlugin.getLogWrapper().logUnsuccessfulAction("Protected region " + pr.getRegionName() + " can't be updated in database.");
            return false;
        }
    }


    /**
     * Adds a storage location to the player storage database.
     * @param playerUUID The UUID of the player to add the storage for.
     * @param storageName The name of the storage.
     * @param loc The location of the storage.
     * @return True if successful, false if not.
     */
    public boolean addStorageLocation(String playerUUID, String storageName, Location loc) {
        String insertStorage = "INSERT INTO tpp_user_storage_locations (user_id, storage_name, effective_storage_name, loc_x, loc_y, loc_z, world_name) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
        return database.insertPrepStatement(insertStorage, trimmedPlayerUUID, storageName, storageName.toLowerCase(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
    }

    /**
     * Removes a storage location from the payer storage database.
     * @param playerUUID The UUID of the player to remove the storage from.
     * @param storageName The name of the storage.
     * @return True if successful, false if not.
     */
    public boolean removeStorageLocation(String playerUUID, String storageName) {
        String removeStorage = "DELETE FROM tpp_user_storage_locations WHERE user_id = ? AND effective_storage_name = ?";
        String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
        return database.deletePrepStatement(removeStorage, trimmedPlayerUUID, storageName.toLowerCase());
    }

    /**
     * Gets storage locations from a particular player and name.
     * @param playerUUID The player to retrieve storage locations from.
     * @param storageName The name of the storage location.
     * @return The {@link StorageLocation} specified, null otherwise.
     */
    public StorageLocation getStorageLocation(String playerUUID, String storageName) {
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
                String getStorage = "SELECT * FROM tpp_user_storage_locations WHERE user_id = ? AND effective_storage_name = ? LIMIT 1";
                ResultSet rs = database.selectPrepStatement(dbConn, getStorage, trimmedPlayerUUID, storageName.toLowerCase());
                StorageLocation ret = null;
                if (rs.next()) {
                    Location retLoc = new Location(Bukkit.getServer().getWorld(rs.getString("world_name")), rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"));
                    ret = new StorageLocation(rs.getString("user_id"), rs.getString("storage_name"), retLoc);
                }
                dbConn.close();
                return ret;
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQLException getting storage data: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Gets all storage locations of a particular player
     * @param playerUUID The player to get the storage locations from.
     * @return A list of storage locations that the player owns. If there was an error connecting with the database, this is null.
     */
    public List<StorageLocation> getStorageLocations(String playerUUID) {
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                String trimmedPlayerUUID = UUIDUtils.trimUUID(playerUUID);
                String getStorage = "SELECT * FROM tpp_user_storage_locations WHERE user_id = ?";
                ResultSet rs = database.selectPrepStatement(dbConn, getStorage, trimmedPlayerUUID);
                List<StorageLocation> ret = new ArrayList<>();
                while (rs.next()) {
                    Location retLoc = new Location(Bukkit.getServer().getWorld(rs.getString("world_name")), rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"));
                    ret.add(new StorageLocation(rs.getString("user_id"), rs.getString("storage_name"), retLoc));
                }
                dbConn.close();
                return ret;
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQLException getting storage data: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Adds a server-wide storage location.
     * @param storageName The name of the location.
     * @param loc The location of the storage.
     * @return True if successful, false if not.
     */
    public boolean addServerStorageLocation(String storageName, Location loc) {
        String insertStorage = "INSERT INTO tpp_server_storage_locations (storage_name, effective_storage_name, loc_x, loc_y, loc_z, world_name) VALUES (?, ?, ?, ?, ?, ?)";
        return database.insertPrepStatement(insertStorage, storageName, storageName.toLowerCase(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
    }

    /**
     * Removes a server-wide storage location.
     * @param storageName The name of the location to remove.
     * @param world The world from which to remove the location.
     * @return True if successful, false if not.
     */
    public boolean removeServerStorageLocation(String storageName, World world) {
        String removeStorage = "DELETE FROM tpp_server_storage_locations WHERE effective_storage_name = ? AND world_name = ?";
        return database.deletePrepStatement(removeStorage, storageName.toLowerCase(), world.getName());
    }

    /**
     * Gets a specific server storage location from its name and world.
     * @param storageName Name of the storage location
     * @param world The world the storage location is in
     * @return Object representing the storage location
     */
    public StorageLocation getServerStorageLocation(String storageName, World world) {
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                String getStorage = "SELECT * FROM tpp_server_storage_locations WHERE effective_storage_name = ? AND world_name = ? LIMIT 1";
                ResultSet rs = database.selectPrepStatement(dbConn, getStorage, storageName.toLowerCase(), world.getName());
                StorageLocation ret = null;
                if (rs.next()) {
                    Location retLoc = new Location(Bukkit.getServer().getWorld(rs.getString("world_name")), rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"));
                    ret = new StorageLocation("server", rs.getString("storage_name"), retLoc);
                }
                dbConn.close();
                return ret;
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQLException getting storage data: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Gets the default server storage location from the world. It selects the first database entry found if allowTpBetweenWorlds has been set. If not, it takes the storage location specific to the world.
     * @param world The world to get the default storage location from
     * @return The default storage location if found, null if can't be found.
     */
    public StorageLocation getDefaultServerStorageLocation(World world) {
        if (thisPlugin.getAllowTpBetweenWorlds()) {
            Connection dbConn = database.getConnection();
            if (dbConn != null) {
                try {
                    String getStorage = "SELECT * FROM tpp_server_storage_locations WHERE effective_storage_name = ? LIMIT 1";
                    ResultSet rs = database.selectPrepStatement(dbConn, getStorage, "default");
                    if (rs.next()) {
                        Location retLoc = new Location(Bukkit.getServer().getWorld(rs.getString("world_name")), rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"));
                        StorageLocation ret = new StorageLocation("server", rs.getString("storage_name"), retLoc);
                        dbConn.close();
                        return ret;
                    }
                    dbConn.close();
                } catch (SQLException e) {
                    thisPlugin.getLogWrapper().logErrors("SQLException getting storage data: " + e.getMessage());
                }
            }
        } else {
            return getServerStorageLocation("default", world);
        }
        return null;
    }

    /**
     * Gets all server storage locations in a world, used for listing existing server storage locations.
     * @param world The world to find storage locations in.
     * @return A list of storage locations.
     */
    public List<StorageLocation> getServerStorageLocations(World world) {
        Connection dbConn = database.getConnection();
        if (dbConn != null) {
            try {
                String getStorage = "SELECT * FROM tpp_server_storage_locations WHERE world_name = ?";
                ResultSet rs = database.selectPrepStatement(dbConn, getStorage, world.getName());
                List<StorageLocation> ret = new ArrayList<>();
                while (rs.next()) {
                    Location retLoc = new Location(Bukkit.getServer().getWorld(rs.getString("world_name")), rs.getInt("loc_x"), rs.getInt("loc_y"), rs.getInt("loc_z"));
                    ret.add(new StorageLocation(null, rs.getString("storage_name"), retLoc));
                }
                dbConn.close();
                return ret;
            } catch (SQLException e) {
                thisPlugin.getLogWrapper().logErrors("SQLException getting storage data: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Gets the number of pets a player owns by the owner's UUID and pet type
     * @param ownerUUID The owner's UUID
     * @param pt The pet's type
     * @return Number of pets a player owns of that type
     */
    public int getNumPetsByPT(String ownerUUID, PetType.Pets pt) throws SQLException {
        Connection dbConn = database.getConnection();
        if (dbConn == null) {
            throw new SQLException();
        }

        String getNumPets = "SELECT COUNT(pet_id) as count FROM tpp_unloaded_pets WHERE owner_id = ? AND pet_type = ?";
        ResultSet rs = database.selectPrepStatement(dbConn, getNumPets, UUIDUtils.trimUUID(ownerUUID), PetType.getIndexFromPet(pt));
        int ret;
        if (rs.next()) {
            ret = rs.getInt("count");
        } else {
            throw new SQLException();
        }
        dbConn.close();
        return ret;
    }

    /**
     * Gets the number of pets a player owns by the owner's UUID
     * @param ownerUUID The owner's UUID
     * @return Number of pets a player owns
     */
    public int getNumPets(String ownerUUID) throws SQLException {
        Connection dbConn = database.getConnection();
        if (dbConn == null) {
            throw new SQLException();
        }

        String getNumPets = "SELECT COUNT(pet_id) as count FROM tpp_unloaded_pets WHERE owner_id = ?";
        ResultSet rs = database.selectPrepStatement(dbConn, getNumPets, UUIDUtils.trimUUID(ownerUUID));
        int ret;
        if (rs.next()) {
            ret = rs.getInt("count");
        } else {
            throw new SQLException();
        }
        dbConn.close();
        return ret;
    }
    
    /**
     * @return The underlying database object, either of type {@link MySQLFrame} or {@link SQLiteFrame}
     */
    public DBFrame getRealDatabase() {
        return database;
    }
}
