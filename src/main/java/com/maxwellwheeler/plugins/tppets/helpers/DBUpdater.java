package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;

import java.sql.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;

/**
 * Used to update the database
 * @author GatheringExp
 *
 */
public class DBUpdater {
    private TPPets thisPlugin;
    private int schemaVersion;
    private final int updatedVersion = 2;

    /**
     * General constructor, gets schema version from the database
     * @param thisPlugin TPPets plugin instance
     */
    public DBUpdater(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
        this.schemaVersion = getSchemaVersionFromDB(thisPlugin.getDatabase());
    }

    /**
     * Core update method. Updates database schema if necessary
     * @param dbw The DBWrapper object to apply the changes to
     * @return True if successful or already up-to-date, false if not
     */
    public boolean update(DBWrapper dbw) {
        if (dbw != null) {
            if (schemaVersion != updatedVersion) {
                if (schemaVersion == 1) {
                    return oneToTwo(dbw);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * If the database is currently up-to-date
     * @return True if yes (or no database tables made yet, so that the DBWrapper will create them itself), false if not
     */
    public boolean isUpToDate() {
        return schemaVersion == updatedVersion || schemaVersion == 0;
    }

    /**
     * Grabs the schema version from the provided {@link DBWrapper} object
     * @param dbw The {@link DBWrapper} to check
     * @return -1 if could not get version from database, 0 if no tables present, database schema version otherwise
     */
    private int getSchemaVersionFromDB(DBWrapper dbw) {
        if (dbw != null) {
            try {
                Connection dbConn = dbw.getRealDatabase().getConnection();
                if (dbConn != null) {
                    // Checks if tpp_db_version exists
                    ResultSet tables = dbConn.getMetaData().getTables(null, null, "tpp_db_version", null);
                    if (tables.next() && tables.getString("TABLE_NAME").equals("tpp_db_version")) {
                        tables.close();
                        // Grabs version data from tpp_db_version, defaults to 1
                        ResultSet versionData = dbw.getRealDatabase().selectPrepStatement(dbConn, "SELECT version FROM tpp_db_version");
                        if (versionData == null) {
                            dbConn.close();
                            return 1;
                        }
                        if (versionData.next()) {
                            int returnInt = versionData.getInt("version");
                            dbConn.close();
                            return returnInt;
                        }
                    // tpp_db_version exists, checking if any initialized tables exist
                    } else {
                        ResultSet oneTables = dbConn.getMetaData().getTables(null, null, "tpp_protected_regions", null);
                        boolean oneTablesExists = oneTables.next() && oneTables.getString("TABLE_NAME").equals("tpp_protected_regions");
                        dbConn.close();
                        return oneTablesExists ? 1 : 0;
                    }
                    dbConn.close();
                // dbConn == null
                } else {
                    return -1;
                }
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception finding current database version: " + e.getMessage());
            }
        }
        // DBWrapper == null
        return -1;
    }

    /**
     * Updates the schema version recorded in the database. Note: Does not actually update the database, just tells the database that it is updated. Use with caution
     * @param dbw The {@link DBWrapper} to check
     * @return True if successful, false if not
     */
    public boolean updateSchemaVersion(DBWrapper dbw) {
        return setCurrentSchemaVersion(dbw, updatedVersion);
    }

    /**
     * Sets the schema version in memory and in the database. Note: Does not actually update the database, just tells the database that it is updated. Use with extreme caution
     * @param dbw The {@link DBWrapper} to check
     * @param setSchemaVersion The schema version to set in memory and database
     * @return True if successful, false if not
     */
    private boolean setCurrentSchemaVersion(DBWrapper dbw, int setSchemaVersion) {
        if (dbw != null) {
            if (schemaVersion == 0) {
                initializeVersion(dbw,2);
            }
            boolean databaseUpdate = dbw.getRealDatabase().updatePrepStatement("UPDATE tpp_db_version SET version = ?", setSchemaVersion);
            if (databaseUpdate) {
                this.schemaVersion = setSchemaVersion;
            }
            return databaseUpdate;
        }
        return false;
    }

    /**
     * Updates the database schema from version one to version two
     * @param dbw The {@link DBWrapper} to update
     * @return True if successful, false if not
     */
    private boolean oneToTwo(DBWrapper dbw) {
        if (dbw != null) {
            boolean addColumn = dbw.getRealDatabase().updatePrepStatement("ALTER TABLE tpp_unloaded_pets ADD pet_name VARCHAR(64)");

            boolean fillColumn = false;
            if (addColumn) {
                fillColumn = oneToTwoFillColumns(dbw);
            }

            boolean createAllowedPlayersTable = false;
            boolean createVersionTable = false;
            if (fillColumn) {
                createAllowedPlayersTable = dbw.getRealDatabase().createStatement("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
                createVersionTable = dbw.getRealDatabase().createStatement("CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY)");
            }

            if (createAllowedPlayersTable && createVersionTable) {
                return initializeVersion(dbw, 1) && setCurrentSchemaVersion(dbw, 2);
            } else {
                // If some part of this failed, revert to schema version one so that an update can be better attempted next time
                twoToOne(dbw);
            }
        }
        return false;
    }

    /**
     * Fills the new petName column with values, so that existing pets will be named by the default naming scheme
     * @param dbw The {@link DBWrapper} to update
     * @return True if successful, false if not
     */
    private boolean oneToTwoFillColumns(DBWrapper dbw) {
        if (dbw != null) {
            try {
                // Hashtable<owner_id, Set<pet_id>>, used to establish a list of how many pets a player owns, so that they can be assigned default names later.
                Hashtable<String, Set<PetStorage>> tempPetStorage = new Hashtable<>();

                Connection conn = dbw.getRealDatabase().getConnection();
                ResultSet getPets = dbw.getRealDatabase().selectPrepStatement(conn, "SELECT * FROM tpp_unloaded_pets");
                while (getPets.next()) {
                    if (!tempPetStorage.containsKey(getPets.getString("owner_id"))) {
                        tempPetStorage.put(getPets.getString("owner_id"), new HashSet<>());
                    }
                    tempPetStorage.get(getPets.getString("owner_id")).add(new PetStorage(getPets.getString("pet_id"), getPets.getInt("pet_type"), getPets.getInt("pet_x"), getPets.getInt("pet_y"), getPets.getInt("pet_z"), getPets.getString("pet_world"), getPets.getString("owner_id"),null));
                }
                conn.close();

                for(String key : tempPetStorage.keySet()) {
                    int numPetsUpdated = 0;
                    for (PetStorage ps : tempPetStorage.get(key)) {
                        if (!dbw.getRealDatabase().updatePrepStatement("UPDATE tpp_unloaded_pets SET pet_name = ? WHERE pet_id = ?", ps.petType.toString() + Integer.toString(++numPetsUpdated), ps.petId)) {
                            return false;
                        }
                    }
                }
                return true;
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception updating database from version one to version two: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Reverts the update from version 2 to version 1
     * @param dbw The {@link DBWrapper} to revert
     * @return True if successful, false if not
     */
    private boolean twoToOne(DBWrapper dbw) {
        if (dbw != null) {
            boolean renameTable = dbw.getRealDatabase().updatePrepStatement("ALTER TABLE tpp_unloaded_pets RENAME TO tpp_unloaded_pets_temp");
            boolean createTable = dbw.getRealDatabase().createStatement("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                    + "pet_id CHAR(32) PRIMARY KEY,\n"
                    + "pet_type TINYINT NOT NULL,\n"
                    + "pet_x INT NOT NULL,\n"
                    + "pet_y INT NOT NULL,\n"
                    + "pet_z INT NOT NULL,\n"
                    + "pet_world VARCHAR(25) NOT NULL,\n"
                    + "owner_id CHAR(32) NOT NULL"
                    + ");");
            boolean transferData = dbw.getRealDatabase().insertPrepStatement("INSERT INTO tpp_unloaded_pets SELECT pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id FROM tpp_unloaded_pets_temp");
            boolean dropTempTable = dbw.getRealDatabase().updatePrepStatement("DROP TABLE tpp_unloaded_pets_temp");
            dbw.getRealDatabase().updatePrepStatement("DROP TABLE IF EXISTS tpp_allowed_players");
            dbw.getRealDatabase().updatePrepStatement("DROP TABLE IF EXISTS tpp_db_version");
            return renameTable && createTable && transferData && dropTempTable;
        }
        return false;
    }

    /**
     * Initializes a version row within the tpp_db_version table, so it can be UPDATE'd later to reflect new values, without having to check it is there
     * @param dbw The {@link DBWrapper} to initialize version on
     * @param version The version to initialize at
     * @return True if successful, false if not
     */
    private boolean initializeVersion(DBWrapper dbw, int version) {
        return dbw != null && dbw.getRealDatabase().insertPrepStatement("INSERT INTO tpp_db_version (version) VALUES(?)", version);
    }
}
