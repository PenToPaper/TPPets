package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;

import java.sql.*;

public abstract class SQLWrapper {
    protected final TPPets thisPlugin;

    protected SQLWrapper(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    // Generic database helper methods
    abstract public Connection getConnection() throws SQLException;

    protected boolean insertPrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return 1 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute insert statement: " + e.getMessage());
            throw e;
        }
    }

    protected boolean deletePrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute delete statement: " + e.getMessage());
            throw e;
        }
    }

    protected boolean updatePrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute update statement: " + e.getMessage());
            throw e;
        }
    }

    protected boolean createStatement(String statement) throws SQLException {
        try (Connection dbConn = getConnection();
             Statement stmt = dbConn.createStatement()) {
            return stmt.executeUpdate(statement) == 0;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute create statement: " + exception.getMessage());
            throw exception;
        }
    }

    protected PreparedStatement setPreparedStatementArgs(PreparedStatement preparedStatement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Integer) {
                preparedStatement.setInt(i + 1, (Integer) args[i]);
            } else if (args[i] instanceof String) {
                preparedStatement.setString(i + 1, (String) args[i]);
            } else {
                throw new SQLException("Invalid argument to prepared statement");
            }
        }
        return preparedStatement;
    }

    protected int executeUpdate(String prepStatement, Object... args) throws SQLException {
        try (Connection dbConn = getConnection();
             PreparedStatement preparedStatement = setPreparedStatementArgs(dbConn.prepareStatement(prepStatement), args)) {
            return preparedStatement.executeUpdate();
        }
    }

    // Public methods

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
        return this.createStatement(makeTableUnloadedPets)
                && this.createStatement(makeTableLostRegions)
                && this.createStatement(makeTableProtectedRegions)
                && this.createStatement(makeTableDBVersion)
                && this.createStatement(makeTableAllowedPlayers)
                && this.createStatement(makeTableUserStorageLocations)
                && this.createStatement(makeTableDefaultStorageLocations)
                && this.thisPlugin.getDatabaseUpdater().updateSchemaVersion(this);
    }

    public boolean isNameUnique(String ownerUUID, String petName) throws SQLException {
        String trimmedOwnerUUID = UUIDUtils.trimUUID(ownerUUID);
        String selectIsNameUnique = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?";

        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectIsNameUnique), trimmedOwnerUUID, petName);
             ResultSet resultSet = selectStatement.executeQuery()) {
            return resultSet.next();

        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("SQL Exception checking if pet name is unique: " + exception.getMessage());
            throw exception;

        }
    }
}
