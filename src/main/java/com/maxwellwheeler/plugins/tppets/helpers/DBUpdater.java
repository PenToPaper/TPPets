package com.maxwellwheeler.plugins.tppets.helpers;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.DBWrapper;
import com.maxwellwheeler.plugins.tppets.storage.PetStorage;

import java.sql.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;

public class DBUpdater {
    private TPPets thisPlugin;
    private int schemaVersion;
    private final int updatedVersion = 2;

    public DBUpdater(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
        this.schemaVersion = getSchemaVersionFromDB(thisPlugin.getDatabase());
    }

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

    public boolean isUpToDate() {
        return schemaVersion == updatedVersion;
    }

    private int getSchemaVersionFromDB(DBWrapper dbw) {
        if (dbw != null) {
            try {
                Connection dbConn = dbw.getRealDatabase().getConnection();
                ResultSet tables = dbConn.getMetaData().getTables(null, null, "tpp_db_version", null);
                if (tables.next() && tables.getString("TABLE_NAME").equals("tpp_db_version")) {
                    tables.close();
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
                }
                dbConn.close();
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception finding current database version: " + e.getMessage());
            }
            return 1;
        }
        return -1;
    }

    public boolean updateSchemaVersion(DBWrapper dbw) {
        return setCurrentSchemaVersion(dbw, updatedVersion);
    }

    private boolean setCurrentSchemaVersion(DBWrapper dbw, int schemaVersion) {
        if (dbw != null) {
            boolean databaseUpdate = dbw.getRealDatabase().updatePrepStatement("UPDATE tpp_db_version SET version = ?", schemaVersion);
            if (databaseUpdate) {
                this.schemaVersion = schemaVersion;
            }
            return databaseUpdate;
        }
        return false;
    }

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
                return oneToTwoInitializeVersion(dbw) && setCurrentSchemaVersion(dbw, 2);
            } else {
                twoToOne(dbw);
            }
        }
        return false;
    }

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

    public boolean twoToOne(DBWrapper dbw) {
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

    private boolean oneToTwoInitializeVersion(DBWrapper dbw) {
        return dbw != null && dbw.getRealDatabase().insertPrepStatement("INSERT INTO tpp_db_version (version) VALUES(?)", 1);
    }
}
