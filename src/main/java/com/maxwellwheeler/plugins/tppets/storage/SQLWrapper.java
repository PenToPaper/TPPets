package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.regions.*;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;

import java.sql.*;
import java.util.*;

/**
 * Represents {@link TPPets}'s connection to an SQL database.
 * @author GatheringExp
 */
public abstract class SQLWrapper {
    /** A reference to the active TPPets instance. */
    protected final TPPets thisPlugin;

    /**
     * Initializes instance variables.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    protected SQLWrapper(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    // Generic statement methods

    /**
     * Gets a database connection object directly.
     * @return A {@link Connection} object to the TPPets database.
     * @throws SQLException If generating a new connection to the database fails.
     */
    abstract public Connection getConnection() throws SQLException;

    /**
     * Executes an insert statement. At least one row is expected to have been inserted.
     * @param prepStatement The prepared statement to execute.
     * @param args An array of arguments to inject into the prepared statement for execution.
     * @return true if at least one row was inserted, false if not.
     * @throws SQLException If executing the insert statement fails. This is logged as an error.
     */
    protected boolean insertPrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return 1 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute insert statement - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Executes a delete statement. Does not expect any rows to be deleted.
     * @param prepStatement The prepared statement to execute.
     * @param args An array of arguments to inject into the prepared statement for execution.
     * @return true if the delete statement was successful, false if not.
     * @throws SQLException If executing the insert statement fails. This is logged as an error.
     */
    protected boolean deletePrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute delete statement - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Executes an update statement. Does not expect any rows to be updated.
     * @param prepStatement The prepared statement to execute.
     * @param args An array of arguments to inject into the prepared statement for execution.
     * @return true if the update statement was successful, false if not.
     * @throws SQLException If executing the update statement fails. This is logged as an error.
     */
    protected boolean updatePrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute update statement - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Executes a create statement.
     * @param statement The create statement to execute.
     * @return true if the create statement was successful, false if not.
     * @throws SQLException If executing the create statement fails. This is logged as an error.
     */
    protected boolean createStatement(String statement) throws SQLException {
        try (Connection dbConn = getConnection();
             Statement stmt = dbConn.createStatement()) {
            return stmt.executeUpdate(statement) == 0;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute create statement: " + exception.getMessage());
            throw exception;
        }
    }

    /**
     * Sets provided array of args individually to the prepared statement, in the order that they appear in the provided arguments array.
     * @param preparedStatement The prepared statement.
     * @param args An array of arguments to set to the prepared statement. Allows integers and strings. They will be set in order.
     * @return The same prepared statement supplied as an argument, but with arguments set.
     * @throws SQLException If preparing the statement fails.
     */
    protected PreparedStatement setPreparedStatementArgs(PreparedStatement preparedStatement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Integer) {
                preparedStatement.setInt(i + 1, (Integer) args[i]);
            } else if (args[i] instanceof String) {
                preparedStatement.setString(i + 1, (String) args[i]);
            } else {
                throw new SQLException("Invalid argument type in prepared statement");
            }
        }
        return preparedStatement;
    }

    /**
     * Executes an SQL update prepared statement (insert/update/delete/etc) and returns the integer that the database returns.
     * @param prepStatement The prepared statement to execute.
     * @param args An array of arguments to set to the prepared statement. Allows integers and strings. They will be set in order.
     * @return The integer that the database returns from the operation.
     * @throws SQLException If executing the update statement fails.
     * @see PreparedStatement#executeUpdate()
     */
    protected int executeUpdate(String prepStatement, Object... args) throws SQLException {
        try (Connection dbConn = getConnection();
             PreparedStatement preparedStatement = setPreparedStatementArgs(dbConn.prepareStatement(prepStatement), args)) {
            return preparedStatement.executeUpdate();
        }
    }

    // Initializing database state

    /**
     * Creates the expected TPPets tables: tpp_allowed_players, tpp_protected_regions, tpp_lost_regions, tpp_unloaded_pets,
     * tpp_user_storage_locations, tpp_server_storage_locations.
     * @return true if all tables were created successfully, false if not.
     * @throws SQLException If creating any of the tables fails. This is logged as an error.
     */
    public boolean createTables() throws SQLException {
        String makeTableAllowedPlayers = "CREATE TABLE IF NOT EXISTS tpp_allowed_players(" +
                "pet_id CHAR(32),\n" +
                "user_id CHAR(32),\n" +
                "PRIMARY KEY(pet_id, user_id),\n" +
                "FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);";
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
        String makeTableServerStorageLocations = "CREATE TABLE IF NOT EXISTS tpp_server_storage_locations (\n" +
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
                && this.createStatement(makeTableAllowedPlayers)
                && this.createStatement(makeTableUserStorageLocations)
                && this.createStatement(makeTableServerStorageLocations)
                && this.thisPlugin.getDatabaseUpdater().updateSchemaVersion(this);
    }

    // Pet actions

    /**
     * Generates a unique pet name based on the pets the owner already owns. Generates in the form: {@code petType.toString() + integer}.
     * The integer is generated based on the number of pets the owner currently has, incrementing until the name is unique.
     * Ex: HORSE6.
     * @param ownerId The owner's id as a string, either trimmed or untrimmed.
     * @param petType The pet type used to generate the name.
     * @return The unique name for the user specified. Generates in the form: {@code petType.toString() + integer}. Ex: HORSE0.
     * @throws SQLException If generating a unique name fails. This is logged as an error.
     */
    public String generateUniquePetName(@NotNull String ownerId, @NotNull PetType.Pets petType) throws SQLException {
        int lastIndexChecked = this.getNumPets(ownerId);
        String ret;

        do {
            ret = petType.toString() + lastIndexChecked++;
        } while (this.getSpecificPet(ownerId, ret) != null);

        return ret;
    }

    /**
     * Inserts an entity as a entity in the database with the supplied owner and entity name. The entity data is used to get
     * the entity's current location.
     * @param entity The entity to insert.
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @param petName The new entity's formatted name.
     * @return true if the insert was successful, false if not.
     * @throws SQLException If inserting the entity fails. This is logged as an error.
     */
    public boolean insertPet(@NotNull Entity entity, @NotNull String ownerId, @NotNull String petName) throws SQLException {
        if (!PetType.isPetTypeTracked(entity)) {
            return false;
        }

        String insertPet = "INSERT INTO tpp_unloaded_pets(pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id, pet_name, effective_pet_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String trimmedPetId = UUIDUtils.trimUUID(entity.getUniqueId());
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        int petTypeIndex = PetType.getIndexFromPet(PetType.getEnumByEntity(entity));

        return this.insertPrepStatement(insertPet, trimmedPetId, petTypeIndex, entity.getLocation().getBlockX(), entity.getLocation().getBlockY(), entity.getLocation().getBlockZ(), entity.getWorld().getName(), trimmedOwnerId, petName, petName.toLowerCase());
    }

    /**
     * Removes a pet from the database from its id.
     * @param petId The pet's id, either trimmed or untrimmed.
     * @return true if the removal was successful, false if not.
     * @throws SQLException If removing the pet fails. This is logged as an error.
     */
    public boolean removePet(@NotNull String petId) throws SQLException {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        String deletePet = "DELETE FROM tpp_unloaded_pets WHERE pet_id = ?";
        return this.deletePrepStatement(deletePet, trimmedPetId);
    }

    /**
     * Updates a pet's location in the database. The pet is determined by both its pet id and its owner id.
     * @param entity The entity to insert.
     * @return true if the update was successful, false if not.
     * @throws SQLException If updating the pet fails. This is logged as an error.
     */
    public boolean updatePetLocation(@NotNull Entity entity) throws SQLException {
        if (PetType.isPetTracked(entity)) {
            String updatePetLocation = "UPDATE tpp_unloaded_pets SET pet_x = ?, pet_y = ?, pet_z = ?, pet_world = ? WHERE pet_id = ? AND owner_id = ?";

            Tameable pet = (Tameable) entity;
            String trimmedPetId = UUIDUtils.trimUUID(pet.getUniqueId());
            String trimmedOwnerId = UUIDUtils.trimUUID(Objects.requireNonNull(pet.getOwner()).getUniqueId());

            return this.updatePrepStatement(updatePetLocation, pet.getLocation().getBlockX(), pet.getLocation().getBlockY(), pet.getLocation().getBlockZ(), pet.getWorld().getName(), trimmedPetId, trimmedOwnerId);
        }
        return false;
    }

    /**
     * Inserts or updates a pet's location, based on whether or not the pet is already in the database. If inserting,
     * this method generates a new pet name with {@link #generateUniquePetName(String, PetType.Pets)}.
     * @param entity The entity to insert or update.
     * @return true if the insert or update was successful, false if not.
     * @throws SQLException If updating the pet fails. This is logged as an error.
     */
    public boolean insertOrUpdatePetLocation(@NotNull Entity entity) throws SQLException {
        if (PetType.isPetTracked(entity)) {
            Tameable pet = (Tameable) entity;
            if (getSpecificPet(entity.getUniqueId().toString()) != null) {
                return updatePetLocation(entity);
            } else {
                String ownerId = Objects.requireNonNull(pet.getOwner()).getUniqueId().toString();
                String petName = generateUniquePetName(ownerId, PetType.getEnumByEntity(entity));
                return insertPet(entity, ownerId, petName);
            }
        }
        return false;
    }

    /**
     * Renames a pet, setting its pet_name, and effective_pet_name in the database. Finds the pet based on its old
     * effective pet name and owner's id. Sets the effective_pet_name as the {@code .toLowerCase()} version of the
     * supplied newName.
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @param oldName The old pet's name. This can be its formatted or effective pet name.
     * @param newName The new pet name. This will determine both the new pet_name and effective_pet_name.
     * @return true if updating the pet's name was successful, false if not.
     * @throws SQLException If updating the pet's name fails. This is logged as an error.
     */
    public boolean renamePet(@NotNull String ownerId, @NotNull String oldName, @NotNull String newName) throws SQLException {
        String updatePetName = "UPDATE tpp_unloaded_pets SET pet_name = ?, effective_pet_name = ? WHERE owner_id = ? AND effective_pet_name = ?";
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        return this.updatePrepStatement(updatePetName, newName, newName.toLowerCase(), trimmedOwnerId, oldName.toLowerCase());
    }

    /**
     * Gets a specific pet, from its owner's id and its name (sent as effective pet name through {@code .toLowerCase()}).
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @param petName The pet's name, either formatted or effective.
     * @return A {@link PetStorage} object with the found pet, or null if no pet found with that name from that owner.
     * @throws SQLException If getting the specific pet fails. This is logged as an error.
     */
    public PetStorage getSpecificPet(@NotNull String ownerId, @NotNull String petName) throws SQLException {
        String selectSpecificPet = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?";
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectSpecificPet), trimmedOwnerId, petName.toLowerCase());
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                return new PetStorage(resultSet.getString("pet_id"), resultSet.getInt("pet_type"), resultSet.getInt("pet_x"), resultSet.getInt("pet_y"), resultSet.getInt("pet_z"), resultSet.getString("pet_world"), resultSet.getString("owner_id"), resultSet.getString("pet_name"), resultSet.getString("effective_pet_name"));
            }
            return null;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    /**
     * Gets a specific pet, from its id.
     * @param petId The pet's id, either trimmed or untrimmed.
     * @return A {@link PetStorage} object with the found pet, or null if no pet found with that id.
     * @throws SQLException If getting the specific pet fails. This is logged as an error.
     */
    public PetStorage getSpecificPet(@NotNull String petId) throws SQLException {
        String selectSpecificPet = "SELECT * FROM tpp_unloaded_pets WHERE pet_id = ?";
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectSpecificPet), trimmedPetId);
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                return new PetStorage(resultSet.getString("pet_id"), resultSet.getInt("pet_type"), resultSet.getInt("pet_x"), resultSet.getInt("pet_y"), resultSet.getInt("pet_z"), resultSet.getString("pet_world"), resultSet.getString("owner_id"), resultSet.getString("pet_name"), resultSet.getString("effective_pet_name"));
            }
            return null;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    /**
     * Gets all pets of a specified pet type from an owner.
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @param petType The pet type to find.
     * @return A list of {@link PetStorage} objects with every found pet. Returns an empty list if no pets are found. Never returns null.
     * @throws SQLException If getting all pets of the specific pet type fails. This is logged as an error.
     */
    public List<PetStorage> getPetTypeFromOwner(@NotNull String ownerId, @NotNull PetType.Pets petType) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String selectPetsFromOwner = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND pet_type = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectPetsFromOwner), trimmedOwnerId, PetType.getIndexFromPet(petType));
             ResultSet resultSet = selectStatement.executeQuery()) {
            List<PetStorage> ret = new ArrayList<>();
            while (resultSet.next()) {
                ret.add(new PetStorage(resultSet.getString("pet_id"), resultSet.getInt("pet_type"), resultSet.getInt("pet_x"), resultSet.getInt("pet_y"), resultSet.getInt("pet_z"), resultSet.getString("pet_world"), resultSet.getString("owner_id"), resultSet.getString("pet_name"), resultSet.getString("effective_pet_name")));
            }
            return ret;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    /**
     * Gets the number of known pets a player owns of a specific pet type. Queries the db using count(pet_id).
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @param petType The pet type to find.
     * @return An integer representing the number of known pets the owner has of the specified type.
     * @throws SQLException If getting the count of the pet type of the owner fails. This is logged as an error.
     */
    public int getNumPetsByPetType(String ownerId, PetType.Pets petType) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        int petTypeIndex = PetType.getIndexFromPet(petType);
        String getNumPets = "SELECT COUNT(pet_id) as count FROM tpp_unloaded_pets WHERE owner_id = ? AND pet_type = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getNumPets), trimmedOwnerId, petTypeIndex);
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt("count");
            }
            throw new SQLException("Could not select count");
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    /**
     * Gets the number of known pets a player owns. Queries the db using count(pet_id).
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @return An integer representing the number of known pets the owner has.
     * @throws SQLException If getting the count of the owner's pets fails. This is logged as an error.
     */
    public int getNumPets(String ownerId) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String getNumPets = "SELECT COUNT(pet_id) as count FROM tpp_unloaded_pets WHERE owner_id = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getNumPets), trimmedOwnerId);
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt("count");
            }
            throw new SQLException("Could not select count");
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    // Guests

    /**
     * Inserts a new guest.
     * @param petId The pet's id, either trimmed or untrimmed.
     * @param playerId The guest's id, either trimmed or untrimmed.
     * @return true if the insert was successful, false if not.
     * @throws SQLException If inserting the guest fails. This is logged as an error.
     */
    public boolean insertGuest(@NotNull String petId, @NotNull String playerId) throws SQLException {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        String trimmedPlayerId = UUIDUtils.trimUUID(playerId);
        String insertGuest = "INSERT INTO tpp_allowed_players (pet_id, user_id) VALUES (?, ?)";
        return this.insertPrepStatement(insertGuest, trimmedPetId, trimmedPlayerId);
    }

    /**
     * Removes an existing guest.
     * @param petId The pet's id, either trimmed or untrimmed.
     * @param playerId The guest's id, either trimmed or untrimmed.
     * @return true if the removal was successful, false if not.
     * @throws SQLException If removing the guest fails. This is logged as an error.
     */
    public boolean removeGuest(@NotNull String petId, @NotNull String playerId) throws SQLException {
        String trimmedPetId = UUIDUtils.trimUUID(petId);
        String trimmedPlayerId = UUIDUtils.trimUUID(playerId);
        String deleteGuest = "DELETE FROM tpp_allowed_players WHERE pet_id = ? AND user_id = ?";
        return this.deletePrepStatement(deleteGuest, trimmedPetId, trimmedPlayerId);
    }

    /**
     * Gets all guests to all pets, in a hashtable with the format: &lt; Trimmed Pet Ids &lt; Trimmed Guest Ids &gt; &gt;.
     * @return A hashtable with the format: &lt; Trimmed Pet Ids &lt; Trimmed Guest Ids &gt; &gt;, populated with all
     * pet/guest pairings. If no pet/guest pairings are found, an empty hashtable is returned. Null is never returned.
     * @throws SQLException If getting all guests fails. This is logged as an error.
     */
    public Hashtable<String, List<String>> getAllGuests() throws SQLException {
        String selectGuests = "SELECT * FROM tpp_allowed_players ORDER BY pet_id";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectGuests));
             ResultSet resultSet = selectStatement.executeQuery()) {
            Hashtable<String, List<String>> ret = new Hashtable<>();
            while (resultSet.next()) {
                String petId = resultSet.getString("pet_id");
                String playerId = resultSet.getString("user_id");
                if (!ret.containsKey(petId)) {
                    ret.put(petId, new ArrayList<>());
                }
                ret.get(petId).add(playerId);
            }
            return ret;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    // Lost regions

    /**
     * Inserts a new {@link LostAndFoundRegion}.
     * @param lostAndFoundRegion The {@link LostAndFoundRegion} to insert.
     * @return true if the insert was successful, false if not.
     * @throws SQLException If inserting the {@link LostAndFoundRegion} fails. This is logged as an error.
     */
    public boolean insertLostRegion(@NotNull LostAndFoundRegion lostAndFoundRegion) throws SQLException {
        String insertLost = "INSERT INTO tpp_lost_regions(zone_name, min_x, min_y, min_z, max_x, max_y, max_z, world_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        return this.insertPrepStatement(insertLost, lostAndFoundRegion.getRegionName(), lostAndFoundRegion.getMinLoc().getBlockX(), lostAndFoundRegion.getMinLoc().getBlockY(), lostAndFoundRegion.getMinLoc().getBlockZ(), lostAndFoundRegion.getMaxLoc().getBlockX(), lostAndFoundRegion.getMaxLoc().getBlockY(), lostAndFoundRegion.getMaxLoc().getBlockZ(), lostAndFoundRegion.getWorldName());
    }

    /**
     * Removes an existing {@link LostAndFoundRegion}
     * @param regionName The {@link LostAndFoundRegion}'s name to remove.
     * @return true if the removal was successful, false if not.
     * @throws SQLException If removing the {@link LostAndFoundRegion} fails. This is logged as an error.
     */
    public boolean removeLostRegion(@NotNull String regionName) throws SQLException {
        String deleteLost = "DELETE FROM tpp_lost_regions WHERE zone_name = ?";
        return this.deletePrepStatement(deleteLost, regionName);
    }

    /**
     * Gets a specific {@link LostAndFoundRegion} based on its name.
     * @param regionName The {@link LostAndFoundRegion}'s name to get.
     * @return A {@link LostAndFoundRegion} if successful, null if not.
     * @throws SQLException If getting the {@link LostAndFoundRegion} fails. This is logged as an error.
     */
    public LostAndFoundRegion getLostRegion(@NotNull String regionName) throws SQLException {
        String selectLostRegion = "SELECT * FROM tpp_lost_regions WHERE zone_name = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectLostRegion), regionName);
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                return new LostAndFoundRegion(resultSet.getString("zone_name"), resultSet.getString("world_name"), resultSet.getInt("min_x"), resultSet.getInt("min_y"), resultSet.getInt("min_z"), resultSet.getInt("max_x"), resultSet.getInt("max_y"), resultSet.getInt("max_z"));
            }
            return null;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    /**
     * Gets all active {@link LostAndFoundRegion}s on the server, in a hashtable with the format: &lt;Lost and Found
     * Region Names&lt; Lost and Found Region Objects &gt; &gt;.
     * @return a hashtable with the format: &lt;Lost and Found Region Names&lt; Lost and Found Region Objects &gt; &gt;,
     * populated with all active {@link LostAndFoundRegion}s. If no {@link LostAndFoundRegion}s are found, an empty
     * hashtable is returned. Null is never returned.
     * @throws SQLException If getting the {@link LostAndFoundRegion}s fails. This is logged as an error.
     */
    public Hashtable<String, LostAndFoundRegion> getLostRegions() throws SQLException {
        String selectLostRegions = "SELECT * FROM tpp_lost_regions";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectLostRegions));
             ResultSet resultSet = selectStatement.executeQuery()) {
            Hashtable<String, LostAndFoundRegion> ret = new Hashtable<>();
            while (resultSet.next()) {
                ret.put(resultSet.getString("zone_name"), new LostAndFoundRegion(resultSet.getString("zone_name"), resultSet.getString("world_name"), resultSet.getInt("min_x"), resultSet.getInt("min_y"), resultSet.getInt("min_z"), resultSet.getInt("max_x"), resultSet.getInt("max_y"), resultSet.getInt("max_z")));
            }
            return ret;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    // Protected regions

    /**
     * Inserts a new {@link ProtectedRegion}.
     * @param protectedRegion The {@link ProtectedRegion} to insert.
     * @return true if the insert was successful, false if not.
     * @throws SQLException If inserting the {@link ProtectedRegion} fails. This is logged as an error.
     */
    public boolean insertProtectedRegion(@NotNull ProtectedRegion protectedRegion) throws SQLException {
        String insertProtectedRegion = "INSERT INTO tpp_protected_regions(zone_name, enter_message, min_x, min_y, min_z, max_x, max_y, max_z, world_name, lf_zone_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return this.insertPrepStatement(insertProtectedRegion, protectedRegion.getRegionName(), protectedRegion.getEnterMessage(), protectedRegion.getMinLoc().getBlockX(), protectedRegion.getMinLoc().getBlockY(), protectedRegion.getMinLoc().getBlockZ(), protectedRegion.getMaxLoc().getBlockX(), protectedRegion.getMaxLoc().getBlockY(), protectedRegion.getMaxLoc().getBlockZ(), protectedRegion.getWorldName(), protectedRegion.getLfName());
    }

    /**
     * Removes an existing {@link ProtectedRegion}
     * @param regionName The {@link ProtectedRegion}'s name to remove.
     * @return true if the removal was successful, false if not.
     * @throws SQLException If removing the {@link ProtectedRegion} fails. This is logged as an error.
     */
    public boolean removeProtectedRegion(@NotNull String regionName) throws SQLException {
        String removeProtectedRegion = "DELETE FROM tpp_protected_regions WHERE zone_name = ?";
        return this.deletePrepStatement(removeProtectedRegion, regionName);
    }

    /**
     * Relinks a given {@link ProtectedRegion} with its corresponding {@link LostAndFoundRegion}.
     * @param regionName The {@link ProtectedRegion}'s name to relink.
     * @param lostRegionName The {@link LostAndFoundRegion}'s name to relink the {@link ProtectedRegion} to.
     * @return true if the update was successful, false if not.
     * @throws SQLException If updating the {@link ProtectedRegion} fails. This is logged as an error.
     */
    public boolean relinkProtectedRegion(@NotNull String regionName, @NotNull String lostRegionName) throws SQLException {
        String relinkProtectedRegion = "UPDATE tpp_protected_regions SET lf_zone_name = ? WHERE zone_name = ?";
        return this.updatePrepStatement(relinkProtectedRegion, lostRegionName, regionName);
    }

    /**
     * Gets a specific {@link ProtectedRegion} based on its name.
     * @param regionName The {@link ProtectedRegion}'s name to get.
     * @return A {@link ProtectedRegion} if successful, null if not.
     * @throws SQLException If getting the {@link ProtectedRegion} fails. This is logged as an error.
     */
    public ProtectedRegion getProtectedRegion(@NotNull String regionName) throws SQLException {
        String selectProtectedRegion = "SELECT * FROM tpp_protected_regions WHERE zone_name = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectProtectedRegion), regionName);
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                return new ProtectedRegion(resultSet.getString("zone_name"), resultSet.getString("enter_message"), resultSet.getString("world_name"), resultSet.getInt("min_x"), resultSet.getInt("min_y"), resultSet.getInt("min_z"), resultSet.getInt("max_x"), resultSet.getInt("max_y"), resultSet.getInt("max_z"), resultSet.getString("lf_zone_name"), this.thisPlugin);
            }
            return null;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    /**
     * Gets all active {@link ProtectedRegion}s on the server, in a hashtable with the format: &lt;Protected
     * Region Names&lt; Protected Region Objects &gt; &gt;.
     * @return a hashtable with the format: &lt;Protected Region Names&lt; Protected Region Objects &gt; &gt;,
     * populated with all active {@link ProtectedRegion}s. If no {@link ProtectedRegion}s are found, an empty
     * hashtable is returned. Null is never returned.
     * @throws SQLException If getting the {@link ProtectedRegion}s fails. This is logged as an error.
     */
    public Hashtable<String, ProtectedRegion> getProtectedRegions() throws SQLException {
        String selectProtectedRegions = "SELECT * FROM tpp_protected_regions";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectProtectedRegions));
             ResultSet resultSet = selectStatement.executeQuery()) {
            Hashtable<String, ProtectedRegion> ret = new Hashtable<>();
            while (resultSet.next()) {
                ret.put(resultSet.getString("zone_name"), new ProtectedRegion(resultSet.getString("zone_name"), resultSet.getString("enter_message"), resultSet.getString("world_name"), resultSet.getInt("min_x"), resultSet.getInt("min_y"), resultSet.getInt("min_z"), resultSet.getInt("max_x"), resultSet.getInt("max_y"), resultSet.getInt("max_z"), resultSet.getString("lf_zone_name"), this.thisPlugin));
            }
            return ret;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    // Storage locations

    /**
     * Inserts a new {@link PlayerStorageLocation}, by its owner's id, storage name, and {@link Location}.
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @param storageName The {@link PlayerStorageLocation}'s name.
     * @param location The {@link PlayerStorageLocation}'s location on the server. Its world and block coordinates are used.
     * @return true if insert successful, false if not.
     * @throws SQLException If inserting the {@link PlayerStorageLocation} fails. This is logged as an error.
     */
    public boolean insertStorageLocation(@NotNull String ownerId, @NotNull String storageName, Location location) throws SQLException {
        if (location.getWorld() != null) {
            String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
            String insertStorage = "INSERT INTO tpp_user_storage_locations (user_id, storage_name, effective_storage_name, loc_x, loc_y, loc_z, world_name) VALUES (?, ?, ?, ?, ?, ?, ?)";
            return this.insertPrepStatement(insertStorage, trimmedOwnerId, storageName, storageName.toLowerCase(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        }
        return false;
    }

    /**
     * Removes a {@link PlayerStorageLocation} from the database from its owner's id and name.
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @param storageName The {@link PlayerStorageLocation}'s name.
     * @return true if the removal was successful, false if not.
     * @throws SQLException If removing the storage location fails. This is logged as an error.
     */
    public boolean removeStorageLocation(@NotNull String ownerId, @NotNull String storageName) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String removeStorage = "DELETE FROM tpp_user_storage_locations WHERE user_id = ? AND effective_storage_name = ?";
        return this.deletePrepStatement(removeStorage, trimmedOwnerId, storageName.toLowerCase());
    }

    /**
     * Gets a {@link PlayerStorageLocation} by its owner's id and storage name.
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @param storageName The {@link PlayerStorageLocation}'s name.
     * @return A {@link PlayerStorageLocation} object with the found location data, or null if no location found.
     * @throws SQLException If getting the storage location fails. This is logged as an error.
     */
    public PlayerStorageLocation getStorageLocation(@NotNull String ownerId, @NotNull String storageName) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String getStorageLocation = "SELECT * FROM tpp_user_storage_locations WHERE user_id = ? AND effective_storage_name = ? LIMIT 1";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getStorageLocation), trimmedOwnerId, storageName.toLowerCase());
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                World world = Bukkit.getWorld(resultSet.getString("world_name"));
                if (world != null) {
                    Location loc = new Location(world, resultSet.getInt("loc_x"), resultSet.getInt("loc_y"), resultSet.getInt("loc_z"));
                    return new PlayerStorageLocation(resultSet.getString("user_id"), resultSet.getString("storage_name"), resultSet.getString("effective_storage_name"), loc);
                }
            }
            return null;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    /**
     * Gets all {@link PlayerStorageLocation}s by their owner's id.
     * @param ownerId The owner's id, either trimmed or untrimmed.
     * @return A list of {@link PlayerStorageLocation} objects with every found storage location.
     * @throws SQLException If getting {@link PlayerStorageLocation}s fails. This is logged as an error.
     */
    public List<PlayerStorageLocation> getStorageLocations(@NotNull String ownerId) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String getStorageLocations = "SELECT * FROM tpp_user_storage_locations WHERE user_id = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getStorageLocations), trimmedOwnerId);
             ResultSet resultSet = selectStatement.executeQuery()) {
            List<PlayerStorageLocation> ret = new ArrayList<>();
            while (resultSet.next()) {
                World world = Bukkit.getWorld(resultSet.getString("world_name"));
                if (world != null) {
                    Location retLoc = new Location(world, resultSet.getInt("loc_x"), resultSet.getInt("loc_y"), resultSet.getInt("loc_z"));
                    ret.add(new PlayerStorageLocation(resultSet.getString("user_id"), resultSet.getString("storage_name"), resultSet.getString("effective_storage_name"), retLoc));
                }
            }
            return ret;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    // Server storage locations

    /**
     * Inserts a new {@link ServerStorageLocation}, by its storage name and {@link Location}.
     * @param storageName The {@link ServerStorageLocation}'s name.
     * @param location The {@link ServerStorageLocation}'s location on the server. Its world and block coordinates are used.
     * @return true if insert successful, false if not.
     * @throws SQLException If inserting the {@link ServerStorageLocation} fails. This is logged as an error.
     */
    public boolean insertServerStorageLocation(@NotNull String storageName, @NotNull Location location) throws SQLException {
        if (location.getWorld() != null) {
            String insertServerStorage = "INSERT INTO tpp_server_storage_locations (storage_name, effective_storage_name, loc_x, loc_y, loc_z, world_name) VALUES (?, ?, ?, ?, ?, ?)";
            return this.insertPrepStatement(insertServerStorage, storageName, storageName.toLowerCase(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        }
        return false;
    }

    /**
     * Removes a {@link ServerStorageLocation} from the database from its name and world.
     * @param storageName The {@link ServerStorageLocation}'s name.
     * @param world The {@link ServerStorageLocation}'s world.
     * @return true if the removal was successful, false if not.
     * @throws SQLException If removing the storage location fails. This is logged as an error.
     */
    public boolean removeServerStorageLocation(@NotNull String storageName, @NotNull World world) throws SQLException {
        String removeServerStorage = "DELETE FROM tpp_server_storage_locations WHERE effective_storage_name = ? AND world_name = ?";
        return this.deletePrepStatement(removeServerStorage, storageName.toLowerCase(), world.getName());
    }

    /**
     * Gets a {@link ServerStorageLocation} by its storage name and world.
     * @param storageName The {@link ServerStorageLocation}'s name.
     * @param world The {@link ServerStorageLocation}'s world.
     * @return A {@link ServerStorageLocation} object with the found location data, or null if no location found.
     * @throws SQLException If getting the storage location fails. This is logged as an error.
     */
    public ServerStorageLocation getServerStorageLocation(@NotNull String storageName, @NotNull World world) throws SQLException {
        String getServerStorage = "SELECT * FROM tpp_server_storage_locations WHERE effective_storage_name = ? AND world_name = ? LIMIT 1";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getServerStorage), storageName.toLowerCase(), world.getName());
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                Location retLoc = new Location(world, resultSet.getInt("loc_x"), resultSet.getInt("loc_y"), resultSet.getInt("loc_z"));
                return new ServerStorageLocation(resultSet.getString("storage_name"), resultSet.getString("effective_storage_name"), retLoc);
            }
            return null;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }

    /**
     * Gets all {@link ServerStorageLocation}s by their world.
     * @param world The {@link ServerStorageLocation}s' world.
     * @return A list of {@link ServerStorageLocation} objects with every found storage location.
     * @throws SQLException If getting {@link ServerStorageLocation}s fails. This is logged as an error.
     */
    public List<ServerStorageLocation> getServerStorageLocations(@NotNull World world) throws SQLException {
        String getServerStorage = "SELECT * FROM tpp_server_storage_locations WHERE world_name = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getServerStorage), world.getName());
             ResultSet resultSet = selectStatement.executeQuery()) {
            List<ServerStorageLocation> ret = new ArrayList<>();
            while (resultSet.next()) {
                Location retLoc = new Location(world, resultSet.getInt("loc_x"), resultSet.getInt("loc_y"), resultSet.getInt("loc_z"));
                ret.add(new ServerStorageLocation(resultSet.getString("storage_name"), resultSet.getString("effective_storage_name"), retLoc));
            }
            return ret;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement - " + exception.getMessage());
            throw exception;
        }
    }
}
