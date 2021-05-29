package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

/**
 * Used to update the TPPets database schema.
 * @author GatheringExp
 */
public class DBUpdater {
    /** A reference to the active TPPets instance. */
    private final TPPets thisPlugin;
    /** The current schema version represented in the database. */
    private int schemaVersion;
    /** The newest schema version. */
    private final int updatedVersion = 4;

    /**
     * Initializes instance variables. Initializes {@link DBUpdater#schemaVersion} from the provided plugin instance's
     * database.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     * @throws SQLException If getting the current schema version from the database fails.
     */
    public DBUpdater(TPPets thisPlugin) throws SQLException {
        this.thisPlugin = thisPlugin;
        this.schemaVersion = getSchemaVersionFromDB(thisPlugin.getDatabase());
    }

    /**
     * Starts the updating process. Cascades updates as needed until fully up to date.
     * @param sqlWrapper The SQLWrapper to update.
     * @throws SQLException If updating to the newest schema version in the database fails.
     */
    public void update(SQLWrapper sqlWrapper) throws SQLException {
        int initialVersion = this.schemaVersion;
        if (this.schemaVersion != this.updatedVersion) {
            if (this.schemaVersion == 1) {
                OneToTwo oneToTwo = new OneToTwo();
                oneToTwo.run(sqlWrapper);
            }
            if (this.schemaVersion == 2) {
                TwoToThree twoToThree = new TwoToThree();
                twoToThree.run(sqlWrapper);
            }
            if (this.schemaVersion == 3) {
                ThreeToFour threeToFour = new ThreeToFour();
                threeToFour.run(sqlWrapper);
            }
            if (this.schemaVersion == this.updatedVersion) {
                this.thisPlugin.getLogWrapper().logPluginInfo("Updated database version from version " + initialVersion + " to " + this.schemaVersion);
            }
        }
    }

    /**
     * Determines if a given table name exists in the given {@link SQLWrapper}. Does not determine if the table is populated.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @param tableName The table name to search for. Must be precisely what is represented in the database.
     * @return true if the table exists in the database, false if not.
     * @throws SQLException If retrieving the table's existence fails.
     */
    private boolean doesTableExist(SQLWrapper sqlWrapper, String tableName) throws SQLException {
        try (Connection dbConn = sqlWrapper.getConnection();
             ResultSet resultSet = dbConn.getMetaData().getTables(null, null, tableName, null)) {
            return resultSet.next() && resultSet.getString("TABLE_NAME").equals(tableName);
        }
    }

    /**
     * Determines if the tpp_db_version table exists in the given {@link SQLWrapper}. Does not determine if the table is populated.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @return true if tpp_db_version exists in the database, false if not.
     * @throws SQLException If retrieving the table's existence fails.
     */
    private boolean doesTppDbVersionExist(SQLWrapper sqlWrapper) throws SQLException {
        return doesTableExist(sqlWrapper, "tpp_db_version");
    }

    /**
     * Determines if the tpp_protected_regions table exists in the given {@link SQLWrapper}. Used to determine if a {@link TPPets} database has been instantiated in the past.
     * Does not determine if the table is populated.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @return true if tpp_db_version exists in the database, false if not.
     * @throws SQLException If retrieving the table's existence fails.
     */
    private boolean doesHaveInitializedTables(SQLWrapper sqlWrapper) throws SQLException {
        return doesTableExist(sqlWrapper, "tpp_protected_regions");
    }

    /**
     * Gets the schema version int from tpp_db_version, if it exists. Returns 0 if the db version isn't in the database.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @return The schema version integer from tpp_db_version, or 0 if none exists.
     * @throws SQLException If retrieving the schema version fails.
     */
    private int getDbVersionFromDbTable(SQLWrapper sqlWrapper) throws SQLException {
        String getVersionNumber = "SELECT version FROM tpp_db_version";
        try (Connection dbConn = sqlWrapper.getConnection();
             PreparedStatement preparedStatement = dbConn.prepareStatement(getVersionNumber);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            return resultSet.getInt("version");
        }
    }

    /**
     * Gets the current schema version from the database. Returns 0 if the database doesn't appear to have been
     * initialized yet.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @return The schema version integer, or 0 if the database doesn't conform to an existing schema.
     * @throws SQLException If retrieving the schema version fails.
     */
    private int getSchemaVersionFromDB(SQLWrapper sqlWrapper) throws SQLException {
        if (doesTppDbVersionExist(sqlWrapper)) {
            return getDbVersionFromDbTable(sqlWrapper);
        } else if (doesHaveInitializedTables(sqlWrapper)) {
            return 1;
        }
        return 0;
    }

    /**
     * Records the newest schema version in the {@link SQLWrapper}'s tpp_db_version table.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @return true if setting the schema version in the database was successful, false if not.
     * @throws SQLException If recording the schema version fails.
     */
    public boolean updateSchemaVersion(SQLWrapper sqlWrapper) throws SQLException {
        return setCurrentSchemaVersion(sqlWrapper, this.updatedVersion);
    }

    /**
     * Creates the tpp_db_version table if it doesn't exist.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @return true if creating the table was successful, false if not.
     * @throws SQLException If creating the table fails.
     */
    private boolean createDbVersionTable(SQLWrapper sqlWrapper) throws SQLException {
        String makeTableDBVersion = "CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);";
        return sqlWrapper.createStatement(makeTableDBVersion);
    }

    /**
     * Inserts a schema version into the tpp_db_version table.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @param schemaVersion The schema version to insert.
     * @return true if inserting the schema version was successful, false if not.
     * @throws SQLException If inserting the value fails.
     */
    private boolean insertDbVersion(SQLWrapper sqlWrapper, int schemaVersion) throws SQLException {
        String insertDbVersion = "INSERT INTO tpp_db_version (version) VALUES(?)";
        return sqlWrapper.insertPrepStatement(insertDbVersion, schemaVersion);
    }

    /**
     * Updates the schema version in the tpp_db_version. Technically updates all rows, but there should only be one.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @param schemaVersion The schema version to update to.
     * @return true if updating the schema version was successful, false if not
     * @throws SQLException If updating the value fails.
     */
    private boolean updateDbVersion(SQLWrapper sqlWrapper, int schemaVersion) throws SQLException {
        String updateDbVersion = "UPDATE tpp_db_version SET version = ?";
        return sqlWrapper.updatePrepStatement(updateDbVersion, schemaVersion);
    }

    /**
     * Records the specified schema version in the {@link SQLWrapper}'s tpp_db_version table.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @return true if setting the schema version in the database was successful, false if not.
     * @throws SQLException If recording the schema version fails.
     */
    private boolean setCurrentSchemaVersionInDb(SQLWrapper sqlWrapper, int schemaVersion) throws SQLException {
        if (this.schemaVersion == 0 || this.schemaVersion == 1) {
            return createDbVersionTable(sqlWrapper) && insertDbVersion(sqlWrapper, schemaVersion);
        }
        return updateDbVersion(sqlWrapper, schemaVersion);
    }

    /**
     * Records the specified schema version in the {@link SQLWrapper}'s tpp_db_version table,
     * and updates {@link DBUpdater#schemaVersion} if successful.
     * @param sqlWrapper The {@link SQLWrapper} to query.
     * @return true if setting the schema version in the database was successful, false if not.
     * @throws SQLException If recording the schema version fails.
     */
    private boolean setCurrentSchemaVersion(SQLWrapper sqlWrapper, int schemaVersion) throws SQLException {
        if (setCurrentSchemaVersionInDb(sqlWrapper, schemaVersion)) {
            this.schemaVersion = schemaVersion;
            return true;
        }
        return false;
    }

    /**
     * Handles updating from schema version 1 to 2, or downgrading from schema version 2 to 1.
     * @author GatheringExp
     */
    private class OneToTwo {
        private OneToTwo(){}

        /**
         * During the update from schema version 1 to 2, adds the pet_name column, unpopulated.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if adding the column was successful, false if not.
         * @throws SQLException If adding the pet_name column fails.
         */
        private boolean oneToTwoAddPetNameColumn(SQLWrapper sqlWrapper) throws SQLException {
            String alterTableStatement = "ALTER TABLE tpp_unloaded_pets ADD pet_name VARCHAR(64)";
            return sqlWrapper.updatePrepStatement(alterTableStatement);
        }

        /**
         * During the update from schema version 1 to 2, creates the tpp_allowed_players table, unpopulated.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if creating the table was successful, false if not.
         * @throws SQLException If creating the tpp_allowed_players table fails.
         */
        private boolean oneToTwoCreateAllowedPlayersTable(SQLWrapper sqlWrapper) throws SQLException {
            String createAllowedPlayersTable = "CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);";
            return sqlWrapper.createStatement(createAllowedPlayersTable);
        }

        /**
         * During the update from schema version 1 to 2, creates the tpp_db_version table, unpopulated.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if creating the table was successful, false if not.
         * @throws SQLException If creating the tpp_db_version table fails.
         */
        private boolean oneToTwoCreateDbVersionTable(SQLWrapper sqlWrapper) throws SQLException {
            String createDbVersionTable = "CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);";
            return sqlWrapper.createStatement(createDbVersionTable);
        }

        /**
         * During the update from schema version 1 to 2, updates a single pet's pet_name column with the default name for
         * its pet type. The default name is determined by its pet type, and the number of other pets with that type,
         * specified by the petIndex argument. Ex: HORSE6
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @param petId The pet id to update. Used to retrieve the exact pet from the database.
         * @param petType The pet type of the pet we're updating. Used to generate a default name.
         * @param petIndex The number of other pets with this type that exist and have had a name generated. Used to
         *                generate a unique default name.
         * @return true if updating the pet's name was successful, false if not.
         * @throws SQLException If updating the pet's name table fails.
         */
        private boolean oneToTwoSetDefaultPetName(SQLWrapper sqlWrapper, String petId, PetType.Pets petType, int petIndex) throws SQLException {
            String updatePetName = "UPDATE tpp_unloaded_pets SET pet_name = ? WHERE pet_id = ?";
            String petName = petType.toString().toUpperCase() + petIndex;
            return sqlWrapper.updatePrepStatement(updatePetName, petName, petId);
        }

        /**
         * During the update from schema version 1 to 2, updates all pet's pet_name column with a default name for its
         * pet type.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if updating all pet names was successful, false if not.
         * @throws SQLException If updating any pet's name table fails.
         * @see OneToTwo#oneToTwoSetDefaultPetName(SQLWrapper, String, PetType.Pets, int)
         */
        private boolean oneToTwoFillPetName(SQLWrapper sqlWrapper) throws SQLException {
            String selectAllPets = "SELECT * FROM tpp_unloaded_pets";
            try (Connection dbConn = sqlWrapper.getConnection();
                 PreparedStatement preparedStatement = dbConn.prepareStatement(selectAllPets);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                Hashtable<String, Integer> numPetsByPlayer = new Hashtable<>();
                while (resultSet.next()) {
                    PetStorage pet = new PetStorage(resultSet.getString("pet_id"), resultSet.getInt("pet_type"), resultSet.getInt("pet_x"), resultSet.getInt("pet_y"), resultSet.getInt("pet_z"), resultSet.getString("pet_world"), resultSet.getString("owner_id"), null, null);
                    int currentOwnerPetCount = numPetsByPlayer.getOrDefault(pet.ownerId, 0);

                    if (!oneToTwoSetDefaultPetName(sqlWrapper, pet.petId, pet.petType, currentOwnerPetCount)) {
                        return false;
                    }
                    numPetsByPlayer.put(pet.ownerId, currentOwnerPetCount + 1);
                }
            }
            return true;
        }

        /**
         * During the revert from 2 to 1, removes the pet_name column. Does this internally by creating a new table
         * without the column, and populating it with all values from the previous table.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if removing the column was successful, false if not.
         * @throws SQLException If removing the pet_name column fails.
         */
        private boolean twoToOneRemovePetNameColumn(SQLWrapper sqlWrapper) throws SQLException {
            String renameTableStatement = "ALTER TABLE tpp_unloaded_pets RENAME TO tpp_unloaded_pets_temp";
            String createVersionOneTableStatement = "CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                    + "pet_id CHAR(32) PRIMARY KEY,\n"
                    + "pet_type TINYINT NOT NULL,\n"
                    + "pet_x INT NOT NULL,\n"
                    + "pet_y INT NOT NULL,\n"
                    + "pet_z INT NOT NULL,\n"
                    + "pet_world VARCHAR(25) NOT NULL,\n"
                    + "owner_id CHAR(32) NOT NULL"
                    + ");";
            String insertOldDataStatement = "INSERT INTO tpp_unloaded_pets SELECT pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id FROM tpp_unloaded_pets_temp";
            String dropOldTableStatement = "DROP TABLE tpp_unloaded_pets_temp";

            // Insert statement called in updatePrepStatement, because 0 lines returned is valid for this type of insert.
            return sqlWrapper.updatePrepStatement(renameTableStatement)
                    && sqlWrapper.createStatement(createVersionOneTableStatement)
                    && sqlWrapper.updatePrepStatement(insertOldDataStatement)
                    && sqlWrapper.updatePrepStatement(dropOldTableStatement);
        }

        /**
         * During the revert from 2 to 1, drops the tpp_allowed_players table.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if dropping the table was successful, false if not.
         * @throws SQLException If dropping the tpp_allowed_players column fails.
         */
        private boolean twoToOneDropAllowedPlayersTable(SQLWrapper sqlWrapper) throws SQLException {
            String dropAllowedPlayersTable = "DROP TABLE IF EXISTS tpp_allowed_players";
            return sqlWrapper.updatePrepStatement(dropAllowedPlayersTable);
        }

        /**
         * During the revert from 2 to 1, drops the tpp_db_version table.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @throws SQLException If dropping the tpp_db_version column fails.
         */
        private void twoToOneDropDbVersionTable(SQLWrapper sqlWrapper) throws SQLException {
            String dropDbVersionTable = "DROP TABLE IF EXISTS tpp_db_version";
            sqlWrapper.updatePrepStatement(dropDbVersionTable);
        }

        /**
         * Reverts the update from 2 back to 1. If a revert option fails, the revert stops.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @throws SQLException If any reverting option fails.
         */
        private void twoToOne(SQLWrapper sqlWrapper) throws SQLException {
            if (twoToOneRemovePetNameColumn(sqlWrapper) && twoToOneDropAllowedPlayersTable(sqlWrapper)) {
                twoToOneDropDbVersionTable(sqlWrapper);
            }
        }

        /**
         * Runs the update from 1 to 2. If the update is unable to be completed, reverts the schema version back to 1.
         * If reverting fails, stops.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @throws SQLException If any updating or reverting option fails.
         */
        private void run(SQLWrapper sqlWrapper) throws SQLException {
            try {
                if (oneToTwoAddPetNameColumn(sqlWrapper) && oneToTwoCreateAllowedPlayersTable(sqlWrapper) && oneToTwoFillPetName(sqlWrapper) && oneToTwoCreateDbVersionTable(sqlWrapper)) {
                    setCurrentSchemaVersion(sqlWrapper, 2);
                    return;
                }
            } catch (SQLException exception) {
                twoToOne(sqlWrapper);
                throw exception;
            }
            twoToOne(sqlWrapper);
        }
    }

    /**
     * Handles updating from schema version 2 to 3, or downgrading from schema version 3 to 2.
     * @author GatheringExp
     */
    private class TwoToThree {
        private TwoToThree(){}

        /**
         * During the update from schema version 2 to 3, adds the effective_pet_name column, unpopulated.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if adding the column was successful, false if not.
         * @throws SQLException If adding the effective_pet_name column fails.
         */
        private boolean twoToThreeAddEffectivePetNameColumn(SQLWrapper sqlWrapper) throws SQLException {
            String alterTableStatement = "ALTER TABLE tpp_unloaded_pets ADD COLUMN effective_pet_name VARCHAR(64)";
            return sqlWrapper.updatePrepStatement(alterTableStatement);
        }

        /**
         * During the update from schema version 2 to 3, populates the effective_pet_name column with the pet_name column
         * in lowercase.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if populating the column was successful, false if not.
         * @throws SQLException If populating the effective_pet_name column fails.
         */
        private boolean twoToThreePopulateEffectivePetNameColumn(SQLWrapper sqlWrapper) throws SQLException {
            String populateTableStatement = "UPDATE tpp_unloaded_pets SET effective_pet_name = lower(pet_name)";
            return sqlWrapper.updatePrepStatement(populateTableStatement);
        }

        /**
         * During the update from schema version 2 to 3, updates an individual pet's pet_name and effective_pet_name.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @param petId The pet's id to update.
         * @param petName The new pet name.
         * @return true if updating the names was successful, false if not.
         * @throws SQLException If updating either column fails.
         */
        private boolean twoToThreeRenamePetWithId(SQLWrapper sqlWrapper, String petId, String petName) throws SQLException {
            String updatePetName = "UPDATE tpp_unloaded_pets SET pet_name = ?, effective_pet_name = ? WHERE pet_id = ?";
            return sqlWrapper.updatePrepStatement(updatePetName, petName, petName.toLowerCase(), petId);
        }

        /**
         * During the update from schema version 2 to 3, generates a new, unique name for each pet with restricted names
         * (all/list), and updates the pet in the database.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if all pets with invalid names are successfully renamed, false if not.
         * @throws SQLException If generating new names or updating any pet fails.
         */
        private boolean twoToThreeRemovePetsNamedAllList(SQLWrapper sqlWrapper) throws SQLException {
            String selectInvalidPets = "SELECT * FROM tpp_unloaded_pets WHERE effective_pet_name = \"all\" OR effective_pet_name = \"list\"";
            try (Connection dbConn = sqlWrapper.getConnection();
                 PreparedStatement preparedStatement = dbConn.prepareStatement(selectInvalidPets);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    PetStorage invalidPet = new PetStorage(resultSet.getString("pet_id"), resultSet.getInt("pet_type"), resultSet.getInt("pet_x"), resultSet.getInt("pet_y"), resultSet.getInt("pet_z"), resultSet.getString("pet_world"), resultSet.getString("owner_id"), resultSet.getString("pet_name"), null);
                    String generatedName = sqlWrapper.generateUniquePetName(invalidPet.ownerId, invalidPet.petType);
                    if (!twoToThreeRenamePetWithId(sqlWrapper, invalidPet.petId, generatedName)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * During the update from schema version 2 to 3, generates a new, unique name for each pet with a duplicate
         * effective_pet_name.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if all duplicates are successfully renamed, false if not.
         * @throws SQLException If generating new names or updating any pet fails.
         */
        private boolean twoToThreeRenameDuplicates(SQLWrapper sqlWrapper) throws SQLException {
            String selectDuplicates = "SELECT * \n"
                    + "FROM tpp_unloaded_pets upi\n"
                    + "WHERE EXISTS (\n"
                    + "SELECT 1\n"
                    + "FROM tpp_unloaded_pets upj\n"
                    + "WHERE upj.effective_pet_name = upi.effective_pet_name\n"
                    + "AND upj.owner_id = upi.owner_id\n"
                    + "LIMIT 1, 1)";
            try (Connection dbConn = sqlWrapper.getConnection();
                 PreparedStatement preparedStatement = dbConn.prepareStatement(selectDuplicates);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    PetStorage invalidPet = new PetStorage(resultSet.getString("pet_id"), resultSet.getInt("pet_type"), resultSet.getInt("pet_x"), resultSet.getInt("pet_y"), resultSet.getInt("pet_z"), resultSet.getString("pet_world"), resultSet.getString("owner_id"), resultSet.getString("pet_name"), null);
                    String generatedName = sqlWrapper.generateUniquePetName(invalidPet.ownerId, invalidPet.petType);
                    if (!twoToThreeRenamePetWithId(sqlWrapper, invalidPet.petId, generatedName)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * During the revert from 3 to 2, removes the effective_pet_name column. Does this internally by creating a new table
         * without the column, and populating it with all values from the previous table.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @throws SQLException If removing the effective_pet_name column fails.
         */
        private void threeToTwoRemoveEffectivePetNameColumn(SQLWrapper sqlWrapper) throws SQLException {
            String renameTableStatement = "ALTER TABLE tpp_unloaded_pets RENAME TO tpp_unloaded_pets_temp";
            String createVersionOneTableStatement = "CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                    + "pet_id CHAR(32) PRIMARY KEY,\n"
                    + "pet_type TINYINT NOT NULL,\n"
                    + "pet_x INT NOT NULL,\n"
                    + "pet_y INT NOT NULL,\n"
                    + "pet_z INT NOT NULL,\n"
                    + "pet_world VARCHAR(25) NOT NULL,\n"
                    + "owner_id CHAR(32) NOT NULL,\n"
                    + "pet_name VARCHAR(64)"
                    + ");";
            String insertOldDataStatement = "INSERT INTO tpp_unloaded_pets SELECT pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id, pet_name FROM tpp_unloaded_pets_temp";
            String dropOldTableStatement = "DROP TABLE tpp_unloaded_pets_temp";

            // updatePrepStatement used for insert statement because 0 results are fine
            if (sqlWrapper.updatePrepStatement(renameTableStatement) && sqlWrapper.createStatement(createVersionOneTableStatement) && sqlWrapper.updatePrepStatement(insertOldDataStatement)) {
                sqlWrapper.updatePrepStatement(dropOldTableStatement);
            }
        }

        /**
         * Reverts the update from 3 back to 2. If a revert option fails, the revert stops.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @throws SQLException If any reverting option fails.
         */
        private void threeToTwo(SQLWrapper sqlWrapper) throws SQLException {
            threeToTwoRemoveEffectivePetNameColumn(sqlWrapper);
        }

        /**
         * Runs the update from 2 to 3. If the update is unable to be completed, reverts the schema version back to 2.
         * If reverting fails, stops.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @throws SQLException If any updating or reverting option fails.
         */
        private void run(SQLWrapper sqlWrapper) throws SQLException {
            try {
                if (twoToThreeAddEffectivePetNameColumn(sqlWrapper) && twoToThreePopulateEffectivePetNameColumn(sqlWrapper) && twoToThreeRemovePetsNamedAllList(sqlWrapper) && twoToThreeRenameDuplicates(sqlWrapper)) {
                    setCurrentSchemaVersion(sqlWrapper, 3);
                    return;
                }
            } catch (SQLException exception) {
                threeToTwo(sqlWrapper);
                throw exception;
            }
            threeToTwo(sqlWrapper);
        }
    }

    /**
     * Handles updating from schema version 3 to 4, or downgrading from schema version 4 to 3.
     * @author GatheringExp
     */
    private class ThreeToFour {
        private ThreeToFour(){}

        /**
         * During the update from schema version 3 to 4, adds the tpp_user_storage_locations column.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if adding the column was successful, false if not.
         * @throws SQLException If adding the tpp_user_storage_locations column fails.
         */
        private boolean threeToFourCreateTppUserStorageLocations(SQLWrapper sqlWrapper) throws SQLException {
            String createUserStorageTable = "CREATE TABLE IF NOT EXISTS tpp_user_storage_locations (\n" +
                    "user_id CHAR(32) NOT NULL, \n" +
                    "storage_name VARCHAR(64) NOT NULL, \n" +
                    "effective_storage_name VARCHAR(64) NOT NULL," +
                    "loc_x INT NOT NULL, \n" +
                    "loc_y INT NOT NULL, \n" +
                    "loc_z INT NOT NULL, \n" +
                    "world_name VARCHAR(25) NOT NULL, \n" +
                    "PRIMARY KEY (user_id, effective_storage_name))";
            return sqlWrapper.createStatement(createUserStorageTable);
        }

        /**
         * During the update from schema version 3 to 4, adds the tpp_server_storage_locations column.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if adding the column was successful, false if not.
         * @throws SQLException If adding the tpp_server_storage_locations column fails.
         */
        private boolean threeToFourCreateTppServerStorageLocations(SQLWrapper sqlWrapper) throws SQLException {
            String createServerStorageTable = "CREATE TABLE IF NOT EXISTS tpp_server_storage_locations (\n" +
                    "storage_name VARCHAR(64) NOT NULL, \n" +
                    "effective_storage_name VARCHAR(64) NOT NULL, \n" +
                    "loc_x INT NOT NULL, \n" +
                    "loc_y INT NOT NULL, \n" +
                    "loc_z INT NOT NULL, \n" +
                    "world_name VARCHAR(25) NOT NULL, \n" +
                    "PRIMARY KEY (effective_storage_name, world_name))";
            return sqlWrapper.createStatement(createServerStorageTable);
        }

        /**
         * During the revert from 4 to 3, drops the tpp_user_storage_locations table.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @return true if dropping the table was successful, false if not.
         * @throws SQLException If dropping tpp_user_storage_locations fails.
         */
        private boolean fourToThreeDropUserStorageLocations(SQLWrapper sqlWrapper) throws SQLException {
            String dropUserStorageTable = "DROP TABLE IF EXISTS tpp_user_storage_locations";
            return sqlWrapper.updatePrepStatement(dropUserStorageTable);
        }

        /**
         * During the revert from 4 to 3, drops the tpp_server_storage_locations table.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @throws SQLException If dropping tpp_server_storage_locations fails.
         */
        private void fourToThreeDropServerStorageLocations(SQLWrapper sqlWrapper) throws SQLException {
            String dropServerStorageTable = "DROP TABLE IF EXISTS tpp_server_storage_locations";
            sqlWrapper.updatePrepStatement(dropServerStorageTable);
        }

        /**
         * Reverts the update from 4 back to 3. If a revert option fails, the revert stops.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @throws SQLException If any reverting option fails.
         */
        private void fourToThree(SQLWrapper sqlWrapper) throws SQLException {
            if (fourToThreeDropUserStorageLocations(sqlWrapper)) {
                fourToThreeDropServerStorageLocations(sqlWrapper);
            }
        }

        /**
         * Runs the update from 3 to 4. If the update is unable to be completed, reverts the schema version back to 3.
         * If reverting fails, stops.
         * @param sqlWrapper The {@link SQLWrapper} to query.
         * @throws SQLException If any updating or reverting option fails.
         */
        private void run(SQLWrapper sqlWrapper) throws SQLException {
            try {
                if (threeToFourCreateTppUserStorageLocations(sqlWrapper) && threeToFourCreateTppServerStorageLocations(sqlWrapper)) {
                    setCurrentSchemaVersion(sqlWrapper, 4);
                    return;
                }
            } catch (SQLException exception) {
                fourToThree(sqlWrapper);
                throw exception;
            }
            fourToThree(sqlWrapper);
        }
    }

    /**
     * Determines if the database schema is currently up to date, by comparing the detected schema version with the
     * newest schema version.
     * @return true if the database is at the newest schema version, false if not.
     */
    public boolean isUpToDate() {
        return this.schemaVersion == this.updatedVersion || this.schemaVersion == 0;
    }
}