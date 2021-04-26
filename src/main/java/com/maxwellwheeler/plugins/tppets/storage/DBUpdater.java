package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;

/**
 * Used to update the database
 * @author GatheringExp
 *
 */
public class DBUpdater {
    private int schemaVersion;
    private final int updatedVersion = 4;

    public DBUpdater(TPPets thisPlugin) throws SQLException {
        this.schemaVersion = getSchemaVersionFromDB(thisPlugin.getDatabase());
    }

    public void update(SQLWrapper sqlWrapper) throws SQLException {
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
        }
    }

    private boolean doesTableExist(SQLWrapper sqlWrapper, String tableName) throws SQLException {
        try (Connection dbConn = sqlWrapper.getConnection();
             ResultSet resultSet = dbConn.getMetaData().getTables(null, null, tableName, null)) {
            return resultSet.next() && resultSet.getString("TABLE_NAME").equals(tableName);
        }
    }

    private boolean doesTppDbVersionExist(SQLWrapper sqlWrapper) throws SQLException {
        return doesTableExist(sqlWrapper, "tpp_db_version");
    }

    private boolean doesHaveInitializedTables(SQLWrapper sqlWrapper) throws SQLException {
        return doesTableExist(sqlWrapper, "tpp_protected_regions");
    }

    private int getDbVersionFromDbTable(SQLWrapper sqlWrapper) throws SQLException {
        String getVersionNumber = "SELECT version FROM tpp_db_version";
        try (Connection dbConn = sqlWrapper.getConnection();
             PreparedStatement preparedStatement = dbConn.prepareStatement(getVersionNumber);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            return resultSet.getInt("version");
        }
    }

    private int getSchemaVersionFromDB(SQLWrapper sqlWrapper) throws SQLException {
        if (doesTppDbVersionExist(sqlWrapper)) {
            return getDbVersionFromDbTable(sqlWrapper);
        } else if (doesHaveInitializedTables(sqlWrapper)) {
            return 1;
        }
        return 0;
    }

    public boolean updateSchemaVersion(SQLWrapper sqlWrapper) throws SQLException {
        return setCurrentSchemaVersion(sqlWrapper, this.updatedVersion);
    }

    private boolean createDbVersionTable(SQLWrapper sqlWrapper) throws SQLException {
        String makeTableDBVersion = "CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);";
        return sqlWrapper.createStatement(makeTableDBVersion);
    }

    private boolean insertDbVersion(SQLWrapper sqlWrapper, int schemaVersion) throws SQLException {
        String insertDbVersion = "INSERT INTO tpp_db_version (version) VALUES(?)";
        return sqlWrapper.insertPrepStatement(insertDbVersion, schemaVersion);
    }

    private boolean updateDbVersion(SQLWrapper sqlWrapper, int schemaVersion) throws SQLException {
        String updateDbVersion = "UPDATE tpp_db_version SET version = ?";
        return sqlWrapper.updatePrepStatement(updateDbVersion, schemaVersion);
    }

    private boolean setCurrentSchemaVersionInDb(SQLWrapper sqlWrapper, int schemaVersion) throws SQLException {
        if (this.schemaVersion == 0 || this.schemaVersion == 1) {
            return createDbVersionTable(sqlWrapper) && insertDbVersion(sqlWrapper, schemaVersion);
        }
        return updateDbVersion(sqlWrapper, schemaVersion);
    }

    private boolean setCurrentSchemaVersion(SQLWrapper sqlWrapper, int schemaVersion) throws SQLException {
        if (setCurrentSchemaVersionInDb(sqlWrapper, schemaVersion)) {
            this.schemaVersion = schemaVersion;
            return true;
        }
        return false;
    }

    private class OneToTwo {
        private OneToTwo(){}

        private boolean oneToTwoAddPetNameColumn(SQLWrapper sqlWrapper) throws SQLException {
            String alterTableStatement = "ALTER TABLE tpp_unloaded_pets ADD pet_name VARCHAR(64)";
            return sqlWrapper.updatePrepStatement(alterTableStatement);
        }

        private boolean oneToTwoCreateAllowedPlayersTable(SQLWrapper sqlWrapper) throws SQLException {
            String createAllowedPlayersTable = "CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);";
            return sqlWrapper.createStatement(createAllowedPlayersTable);
        }

        private boolean oneToTwoCreateDbVersionTable(SQLWrapper sqlWrapper) throws SQLException {
            String createDbVersionTable = "CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);";
            return sqlWrapper.createStatement(createDbVersionTable);
        }

        private boolean oneToTwoSetDefaultPetName(SQLWrapper sqlWrapper, String petId, PetType.Pets petType, int petIndex) throws SQLException {
            String updatePetName = "UPDATE tpp_unloaded_pets SET pet_name = ? WHERE pet_id = ?";
            String petName = petType.toString().toUpperCase() + petIndex;
            return sqlWrapper.updatePrepStatement(updatePetName, petName, petId);
        }

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

        private boolean twoToOneDropAllowedPlayersTable(SQLWrapper sqlWrapper) throws SQLException {
            String dropAllowedPlayersTable = "DROP TABLE IF EXISTS tpp_allowed_players";
            return sqlWrapper.updatePrepStatement(dropAllowedPlayersTable);
        }

        private void twoToOneDropDbVersionTable(SQLWrapper sqlWrapper) throws SQLException {
            String dropDbVersionTable = "DROP TABLE IF EXISTS tpp_db_version";
            sqlWrapper.updatePrepStatement(dropDbVersionTable);
        }

        private void twoToOne(SQLWrapper sqlWrapper) throws SQLException {
            if (twoToOneRemovePetNameColumn(sqlWrapper) && twoToOneDropAllowedPlayersTable(sqlWrapper)) {
                twoToOneDropDbVersionTable(sqlWrapper);
            }
        }

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

    private class TwoToThree {
        private TwoToThree(){}

        private boolean twoToThreeAddEffectivePetNameColumn(SQLWrapper sqlWrapper) throws SQLException {
            String alterTableStatement = "ALTER TABLE tpp_unloaded_pets ADD COLUMN effective_pet_name VARCHAR(64)";
            return sqlWrapper.updatePrepStatement(alterTableStatement);
        }

        private boolean twoToThreePopulateEffectivePetNameColumn(SQLWrapper sqlWrapper) throws SQLException {
            String populateTableStatement = "UPDATE tpp_unloaded_pets SET effective_pet_name = lower(pet_name)";
            return sqlWrapper.updatePrepStatement(populateTableStatement);
        }

        private boolean twoToThreeRenamePetWithId(SQLWrapper sqlWrapper, String petId, String petName) throws SQLException {
            String updatePetName = "UPDATE tpp_unloaded_pets SET pet_name = ?, effective_pet_name = ? WHERE pet_id = ?";
            return sqlWrapper.updatePrepStatement(updatePetName, petName, petName.toLowerCase(), petId);
        }

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

        private void threeToTwo(SQLWrapper sqlWrapper) throws SQLException {
            threeToTwoRemoveEffectivePetNameColumn(sqlWrapper);
        }

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

    private class ThreeToFour {
        private ThreeToFour(){}

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

        private boolean fourToThreeDropUserStorageLocations(SQLWrapper sqlWrapper) throws SQLException {
            String dropUserStorageTable = "DROP TABLE IF EXISTS tpp_user_storage_locations";
            return sqlWrapper.updatePrepStatement(dropUserStorageTable);
        }

        private void fourToThreeDropServerStorageLocations(SQLWrapper sqlWrapper) throws SQLException {
            String dropServerStorageTable = "DROP TABLE IF EXISTS tpp_server_storage_locations";
            sqlWrapper.updatePrepStatement(dropServerStorageTable);
        }

        private void fourToThree(SQLWrapper sqlWrapper) throws SQLException {
            if (fourToThreeDropUserStorageLocations(sqlWrapper)) {
                fourToThreeDropServerStorageLocations(sqlWrapper);
            }
        }

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

    public boolean isUpToDate() {
        return this.schemaVersion == this.updatedVersion || this.schemaVersion == 0;
    }
}