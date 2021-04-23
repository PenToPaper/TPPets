package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;
import com.maxwellwheeler.plugins.tppets.regions.StorageLocation;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;

import java.sql.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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

    // TODO: Can replace with call to getSpecificPet
    public boolean isNameUnique(@NotNull String ownerId, @NotNull String petName) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String selectIsNameUnique = "SELECT * FROM tpp_unloaded_pets WHERE owner_id = ? AND effective_pet_name = ?";

        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectIsNameUnique), trimmedOwnerId, petName.toLowerCase());
             ResultSet resultSet = selectStatement.executeQuery()) {
            return !resultSet.next();
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("SQL Exception checking if pet name is unique: " + exception.getMessage());
            throw exception;
        }
    }

    public String generateUniquePetName(@NotNull String ownerId, @NotNull PetType.Pets petType) throws SQLException {
        List<PetStorage> currentPetsList = this.getAllPetsFromOwner(ownerId);
        int lastIndexChecked = currentPetsList.size();
        String ret;

        do {
            ret = petType.toString() + lastIndexChecked++;
        } while (!this.isNameUnique(ownerId, ret));

        return ret;
    }

    public boolean insertPet(@NotNull Entity pet, @NotNull String ownerId, @NotNull String petName) throws SQLException {
        if (!PetType.isPetTypeTracked(pet)) {
            return false;
        }

        String insertPet = "INSERT INTO tpp_unloaded_pets(pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id, pet_name, effective_pet_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String trimmedPetId = UUIDUtils.trimUUID(pet.getUniqueId());
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        int petTypeIndex = PetType.getIndexFromPet(PetType.getEnumByEntity(pet));

        return this.insertPrepStatement(insertPet, trimmedPetId, petTypeIndex, pet.getLocation().getBlockX(), pet.getLocation().getBlockY(), pet.getLocation().getBlockZ(), pet.getWorld().getName(), trimmedOwnerId, petName, petName.toLowerCase());
    }

    public boolean removePet(@NotNull Entity entity) throws SQLException {
        if (PetType.isPetTracked(entity)) {
            String deletePet = "DELETE FROM tpp_unloaded_pets WHERE pet_id = ?";
            String trimmedPetId = UUIDUtils.trimUUID(entity.getUniqueId());

            return this.deletePrepStatement(deletePet, trimmedPetId);
        }
        return false;
    }

    public boolean updatePetLocation(@NotNull Entity entity) throws SQLException {
        if (PetType.isPetTracked(entity)) {
            String updatePetLocation = "UPDATE tpp_unloaded_pets SET pet_x = ?, pet_y = ?, pet_z = ?, pet_world = ? WHERE pet_id = ? AND owner_id = ?";

            Tameable pet = (Tameable) entity;
            String trimmedPetId = UUIDUtils.trimUUID(entity.getUniqueId());
            String trimmedOwnerId = UUIDUtils.trimUUID(Objects.requireNonNull(pet.getOwner()).getUniqueId());

            return this.updatePrepStatement(updatePetLocation, pet.getLocation().getBlockX(), pet.getLocation().getBlockY(), pet.getLocation().getBlockZ(), pet.getWorld().getName(), trimmedPetId, trimmedOwnerId);
        }
        return false;
    }

    public boolean insertOrUpdatePetLocation(@NotNull Entity entity) throws SQLException {
        if (PetType.isPetTracked(entity)) {
            Tameable pet = (Tameable) entity;
            if (getSpecificPet(entity.getUniqueId().toString()) != null) {
                return updatePetLocation(entity);
            } else {
                String ownerId = pet.getOwner().getUniqueId().toString();
                String petName = generateUniquePetName(ownerId, PetType.getEnumByEntity(entity));
                return insertPet(entity, ownerId, petName);
            }
        }
        return false;
    }

    public boolean renamePet(@NotNull String ownerId, @NotNull String oldName, @NotNull String newName) throws SQLException {
        String updatePetName = "UPDATE tpp_unloaded_pets SET pet_name = ?, effective_pet_name = ? WHERE owner_id = ? AND effective_pet_name = ?";
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        return this.updatePrepStatement(updatePetName, newName, newName.toLowerCase(), trimmedOwnerId, oldName.toLowerCase());
    }

    // TODO: Make return single PetStorage. List not needed anymore
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
        }
    }

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
        }
    }

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

    public Hashtable<String, List<String>> getAllAllowedPlayers() throws SQLException {
        String selectAllAllowedPlayers = "SELECT * FROM tpp_allowed_players ORDER BY pet_id";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectAllAllowedPlayers));
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
        }
    }

    // Lost regions

    public boolean insertLostRegion(@NotNull LostAndFoundRegion lostAndFoundRegion) throws SQLException {
        String insertLost = "INSERT INTO tpp_lost_regions(zone_name, min_x, min_y, min_z, max_x, max_y, max_z, world_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        return this.insertPrepStatement(insertLost, lostAndFoundRegion.getRegionName(), lostAndFoundRegion.getMinLoc().getBlockX(), lostAndFoundRegion.getMinLoc().getBlockY(), lostAndFoundRegion.getMinLoc().getBlockZ(), lostAndFoundRegion.getMaxLoc().getBlockX(), lostAndFoundRegion.getMaxLoc().getBlockY(), lostAndFoundRegion.getMaxLoc().getBlockZ(), lostAndFoundRegion.getWorldName());
    }

    public boolean removeLostRegion(@NotNull String regionName) throws SQLException {
        String deleteLost = "DELETE FROM tpp_lost_regions WHERE zone_name = ?";
        return this.deletePrepStatement(deleteLost, regionName);
    }

    public LostAndFoundRegion getLostRegion(@NotNull String regionName) throws SQLException {
        String selectLostRegion = "SELECT * FROM tpp_lost_regions WHERE zone_name = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectLostRegion), regionName);
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                return new LostAndFoundRegion(resultSet.getString("zone_name"), resultSet.getString("world_name"), resultSet.getInt("min_x"), resultSet.getInt("min_y"), resultSet.getInt("min_z"), resultSet.getInt("max_x"), resultSet.getInt("max_y"), resultSet.getInt("max_z"));
            }
            return null;
        }
    }

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
        }
    }

    // Protected regions

    public boolean insertProtectedRegion(@NotNull ProtectedRegion protectedRegion) throws SQLException {
        String insertProtectedRegion = "INSERT INTO tpp_protected_regions(zone_name, enter_message, min_x, min_y, min_z, max_x, max_y, max_z, world_name, lf_zone_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return this.insertPrepStatement(insertProtectedRegion, protectedRegion.getRegionName(), protectedRegion.getEnterMessage(), protectedRegion.getMinLoc().getBlockX(), protectedRegion.getMinLoc().getBlockY(), protectedRegion.getMinLoc().getBlockZ(), protectedRegion.getMaxLoc().getBlockX(), protectedRegion.getMaxLoc().getBlockY(), protectedRegion.getMaxLoc().getBlockZ(), protectedRegion.getWorldName(), protectedRegion.getLfName());
    }

    public boolean removeProtectedRegion(@NotNull String regionName) throws SQLException {
        String removeProtectedRegion = "DELETE FROM tpp_protected_regions WHERE zone_name = ?";
        return this.deletePrepStatement(removeProtectedRegion, regionName);
    }

    public ProtectedRegion getProtectedRegion(@NotNull String regionName) throws SQLException {
        String selectProtectedRegion = "SELECT * FROM tpp_protected_regions WHERE zone_name = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(selectProtectedRegion), regionName);
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                return new ProtectedRegion(resultSet.getString("zone_name"), resultSet.getString("enter_message"), resultSet.getString("world_name"), resultSet.getInt("min_x"), resultSet.getInt("min_y"), resultSet.getInt("min_z"), resultSet.getInt("max_x"), resultSet.getInt("max_y"), resultSet.getInt("max_z"), resultSet.getString("lf_zone_name"), this.thisPlugin);
            }
            return null;
        }
    }

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
        }
    }

    // Storage locations

    public boolean addStorageLocation(@NotNull String ownerId, @NotNull String storageName, Location location) throws SQLException {
        if (location.getWorld() != null) {
            String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
            String insertStorage = "INSERT INTO tpp_user_storage_locations (user_id, storage_name, effective_storage_name, loc_x, loc_y, loc_z, world_name) VALUES (?, ?, ?, ?, ?, ?, ?)";
            return this.insertPrepStatement(insertStorage, trimmedOwnerId, storageName, storageName.toLowerCase(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        }
        return false;
    }

    public boolean removeStorageLocation(@NotNull String ownerId, @NotNull String storageName) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String removeStorage = "DELETE FROM tpp_user_storage_locations WHERE user_id = ? AND effective_storage_name = ?";
        return this.deletePrepStatement(removeStorage, trimmedOwnerId, storageName.toLowerCase());
    }

    public StorageLocation getStorageLocation(@NotNull String ownerId, @NotNull String storageName) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String getStorageLocation = "SELECT * FROM tpp_user_storage_locations WHERE user_id = ? AND effective_storage_name = ? LIMIT 1";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getStorageLocation), trimmedOwnerId, storageName.toLowerCase());
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                // TODO: CONSIDER REFACTORING PROTECTEDREGION CONSTRUCTOR TO BE SIMILAR WHERE YOU HAVE TO SUPPLY THE REFERENCE YOURSELF?
                Location retLoc = new Location(Bukkit.getWorld(resultSet.getString("world_name")), resultSet.getInt("loc_x"), resultSet.getInt("loc_y"), resultSet.getInt("loc_z"));
                return new StorageLocation(resultSet.getString("user_id"), resultSet.getString("storage_name"), retLoc);
            }
            return null;
        }
    }

    public List<StorageLocation> getPlayerStorageLocations(@NotNull String ownerId) throws SQLException {
        String trimmedOwnerId = UUIDUtils.trimUUID(ownerId);
        String getStorageLocations = "SELECT * FROM tpp_user_storage_locations WHERE user_id = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getStorageLocations), trimmedOwnerId);
             ResultSet resultSet = selectStatement.executeQuery()) {
            List<StorageLocation> ret = new ArrayList<>();
            if (resultSet.next()) {
                Location retLoc = new Location(Bukkit.getWorld(resultSet.getString("world_name")), resultSet.getInt("loc_x"), resultSet.getInt("loc_y"), resultSet.getInt("loc_z"));
                ret.add(new StorageLocation(resultSet.getString("user_id"), resultSet.getString("storage_name"), retLoc));
            }
            return ret;
        }
    }

    // Server storage locations

    public boolean addServerStorageLocation(@NotNull String storageName, @NotNull Location location) throws SQLException {
        if (location.getWorld() != null) {
            String insertServerStorage = "INSERT INTO tpp_server_storage_locations (storage_name, effective_storage_name, loc_x, loc_y, loc_z, world_name) VALUES (?, ?, ?, ?, ?, ?)";
            return this.insertPrepStatement(insertServerStorage, storageName, storageName.toLowerCase(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        }
        return false;
    }

    public boolean removeServerStorageLocation(@NotNull String storageName, @NotNull World world) throws SQLException {
        String removeServerStorage = "DELETE FROM tpp_server_storage_locations WHERE effective_storage_name = ? AND world_name = ?";
        return this.deletePrepStatement(removeServerStorage, storageName.toLowerCase(), world.getName());
    }

    public StorageLocation getServerStorageLocation(@NotNull String storageName, @NotNull World world) throws SQLException {
        String getServerStorage = "SELECT * FROM tpp_server_storage_locations WHERE effective_storage_name = ? AND world_name = ? LIMIT 1";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getServerStorage), storageName.toLowerCase(), world.getName());
             ResultSet resultSet = selectStatement.executeQuery()) {
            if (resultSet.next()) {
                // TODO: Consider refactoring StorageLocation to include ServerStorageLocation as a separate class?
                Location retLoc = new Location(Bukkit.getWorld(resultSet.getString("world_name")), resultSet.getInt("loc_x"), resultSet.getInt("loc_y"), resultSet.getInt("loc_z"));
                return new StorageLocation("server", resultSet.getString("storage_name"), retLoc);
            }
            return null;
        }
    }

    public List<StorageLocation> getServerStorageLocations(@NotNull World world) throws SQLException {
        String getServerStorage = "SELECT * FROM tpp_server_storage_locations WHERE world_name = ?";
        try (Connection dbConn = this.getConnection();
             PreparedStatement selectStatement = this.setPreparedStatementArgs(dbConn.prepareStatement(getServerStorage), world.getName());
             ResultSet resultSet = selectStatement.executeQuery()) {
            List<StorageLocation> ret = new ArrayList<>();
            while (resultSet.next()) {
                // TODO: Consider refactoring StorageLocation to include constructor that lets us do everything in one line
                Location retLoc = new Location(Bukkit.getWorld(resultSet.getString("world_name")), resultSet.getInt("loc_x"), resultSet.getInt("loc_y"), resultSet.getInt("loc_z"));
                ret.add(new StorageLocation(null, resultSet.getString("storage_name"), retLoc));
            }
            return ret;
        }
    }
}
