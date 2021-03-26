package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.sun.istack.internal.NotNull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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

    // Initializers

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

    // Pet actions

    public boolean isNameUnique(@NotNull String ownerId, @NotNull String petName) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String selectIsNameUnique = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?";

        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectIsNameUnique), trimmedownerId, petName);
             ResultSet resultSet = selectStatement.executeQuery()) {
            return resultSet.next();
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("SQL Exception checking if pet name is unique: " + exception.getMessage());
            throw exception;
        }
    }

    public String generateUniquePetName(@NotNull String ownerId, PetType.Pets petType) throws SQLException {
        List<PetStorage> currentPetsList = this.getAllPetsFromOwner(ownerId);
        int lastIndexChecked = currentPetsList.size();
        String ret;

        do {
            ret = petType.toString() + lastIndexChecked++;
        } while (!this.isNameUnique(ownerId, ret));

        return ret;
    }

    public boolean insertPet(@NotNull Entity entity, @NotNull String ownerId, @NotNull String petName) throws SQLException {
        if (!PetType.isPetTypeTracked(entity) || !this.isNameUnique(ownerId, petName)) {
            return false;
        }

        String insertPet = "INSERT INTO tpp_unloaded_pets(pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id, pet_name, effective_pet_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String trimmedPetId = UUIDUtils.trimUUID(entity.getUniqueId());
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        int petTypeIndex = PetType.getIndexFromPet(PetType.getEnumByEntity(entity));

        return this.insertPrepStatement(insertPet, trimmedPetId, petTypeIndex, entity.getLocation().getBlockX(), entity.getLocation().getBlockY(), entity.getLocation().getBlockZ(), entity.getWorld().getName(), trimmedOwnerId, petName, petName.toLowerCase());
    }

    public boolean updatePetLocation(@NotNull Entity entity) throws SQLException {
        if (!PetType.isPetTracked(entity)) {
            return false;
        }

        String updatePetLocation = "UPDATE tpp_unloaded_pets SET pet_x = ?, pet_y = ?, pet_z = ?, pet_world = ? WHERE pet_id = ? AND owner_id = ?";

        Tameable pet = (Tameable) entity;
        String trimmedPetId = UUIDUtils.trimUUID(entity.getUniqueId());
        String trimmedOwnerId = UUIDUtils.trimUUID(Objects.requireNonNull(pet.getOwner()).getUniqueId());

        return this.updatePrepStatement(updatePetLocation, pet.getLocation().getBlockX(), pet.getLocation().getBlockY(), pet.getLocation().getBlockZ(), pet.getWorld().getName(), trimmedPetId, trimmedOwnerId);
    }

    public boolean renamePet(@NotNull String ownerId, @NotNull String oldName, @NotNull String newName) throws SQLException {
        String updatePetName = "UPDATE tpp_unloaded_pets SET pet_name = ?, effective_pet_name = ? WHERE owner_id = ? AND effective_pet_name = ?";
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        return this.updatePrepStatement(updatePetName, newName, newName.toLowerCase(), trimmedOwnerId, oldName.toLowerCase());
    }
    
    public List<PetStorage> getSpecificPet(@NotNull String ownerId, @NotNull String petName) throws SQLException {
        String selectSpecificPet = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?";
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        try (Connection dbConn = this.getConnection();
            PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectSpecificPet), trimmedOwnerId, petName);
            ResultSet resultSet = selectStatement.executeQuery()) {
            List<PetStorage> ret = new ArrayList<>();
            while (resultSet.next()) {
                ret.add(new PetStorage(resultSet.getString("pet_id"), resultSet.getInt("pet_type"), resultSet.getInt("pet_x"), resultSet.getInt("pet_y"), resultSet.getInt("pet_z"), resultSet.getString("pet_world"), resultSet.getString("owner_id"), resultSet.getString("pet_name"), resultSet.getString("effective_pet_name")));
            }
            return ret;
        }
    }

    public List<PetStorage> getAllPetsFromOwner(@NotNull String ownerId) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String selectPetsFromOwner = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectPetsFromOwner), trimmedOwnerId);
             ResultSet resultSet = selectStatement.executeQuery()) {
            List<PetStorage> ret = new ArrayList<>();
            while (resultSet.next()) {
                ret.add(new PetStorage(resultSet.getString("pet_id"), resultSet.getInt("pet_type"), resultSet.getInt("pet_x"), resultSet.getInt("pet_y"), resultSet.getInt("pet_z"), resultSet.getString("pet_world"), resultSet.getString("owner_id"), resultSet.getString("pet_name"), resultSet.getString("effective_pet_name")));
            }
            return ret;
        }
    }

    public List<PetStorage> getPetFromOwnerWorld(@NotNull String ownerId, @NotNull String worldName) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String selectPetsGeneric = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND pet_world = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectPetsGeneric), trimmedOwnerId, worldName);
             ResultSet resultSet = selectStatement.executeQuery()) {
            List<PetStorage> ret = new ArrayList<>();
            while (resultSet.next()) {
                ret.add(new PetStorage(resultSet.getString("pet_id"), resultSet.getInt("pet_type"), resultSet.getInt("pet_x"), resultSet.getInt("pet_y"), resultSet.getInt("pet_z"), resultSet.getString("pet_world"), resultSet.getString("owner_id"), resultSet.getString("pet_name"), resultSet.getString("effective_pet_name")));
            }
            return ret;
        }
    }

    // Allowed players

    public boolean insertAllowedPlayer(@NotNull String petId, @NotNull String playerId) throws SQLException {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        String trimmedPlayerId = UUIDUtils.trimUUID(playerId);
        String insertAllowedPlayer = "INSERT INTO tpp_allowed_players (pet_id, user_id) VALUES (?, ?)";
        return this.insertPrepStatement(insertAllowedPlayer, trimmedPetId, trimmedPlayerId);
    }

    public boolean removeAllowedPlayer(@NotNull String petId, @NotNull String playerId) throws SQLException {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        String trimmedPlayerId = UUIDUtils.trimUUID(playerId);
        String deleteAllowedPlayer = "DELETE FROM tpp_allowed_players WHERE pet_id = ? AND user_id = ?";
        return this.deletePrepStatement(deleteAllowedPlayer, trimmedPetId, trimmedPlayerId);
    }
}
