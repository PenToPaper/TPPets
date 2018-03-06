package com.maxwellwheeler.plugins.tppets.storage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.UUIDUtils;
import com.maxwellwheeler.plugins.tppets.regions.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.regions.ProtectedRegion;

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
            // + "FOREIGN KEY(lf_zone_name) REFERENCES tpp_lost_regions(zone_name));";
    
    /*
     *      UNLOADED_PETS STATEMENTS
     */
    private String insertPet = "INSERT INTO tpp_unloaded_pets(pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private String deletePet = "DELETE FROM tpp_unloaded_pets WHERE pet_id = ? AND owner_id = ?";
    private String updatePet = "UPDATE tpp_unloaded_pets SET pet_x = ?, pet_y = ?, pet_z = ?, pet_world = ? WHERE pet_id = ? AND owner_id = ?";
    private String selectPetFromUuid = "SELECT * FROM tpp_unloaded_pets WHERE pet_id = ?";
    
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
    
    public DBWrapper(String host, int port, String dbName, String dbUsername, String dbPassword, TPPets thisPlugin) {
        database = new MySQLFrame(host, port, dbName, dbUsername, dbPassword, thisPlugin);
        this.thisPlugin = thisPlugin;
    }
    
    public DBWrapper(String dbPath, String dbName, TPPets thisPlugin) {
        database = new SQLiteFrame(dbPath, dbName, thisPlugin);
        this.thisPlugin = thisPlugin;
    }
    
    public boolean initializeTables() {
        return database.createStatement(makeTableUnloadedPets)
                && database.createStatement(makeTableLostRegions)
                && database.createStatement(makeTableProtectedRegions);
    }
    
    /*
     *      UNLOADED_PETS METHODS
     */
    
    public boolean insertPet(Entity ent) {
        if (ent instanceof Tameable && ent instanceof Sittable) {
            Tameable tameableTemp = (Tameable) ent;
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
        return false;
    }
    
    public boolean deletePet(Entity ent) {
        if (ent instanceof Tameable && ent instanceof Sittable) {
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
    
    public boolean updatePet(Entity ent) {
        if (ent instanceof Tameable && ent instanceof Sittable) {
            Tameable tameableTemp = (Tameable) ent;
            String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
            String trimmedPlayerUUID = UUIDUtils.trimUUID(tameableTemp.getOwner().getUniqueId());
            if (database.updatePrepStatement(updatePet, ent.getLocation().getBlockX(), ent.getLocation().getBlockY(), ent.getLocation().getBlockZ(), ent.getLocation().getWorld().getName(), trimmedEntUUID, trimmedPlayerUUID)) {
                thisPlugin.getLogger().info("Pet with UUID " + trimmedEntUUID + " updated in database.");
                return true;
            } else {
                thisPlugin.getLogger().info("Pet with UUID " + trimmedEntUUID + " can't be updated in database.");
                return false;
            }
        }
        return false;
    }
    
    public boolean updateOrInsertPet(Entity ent) {
        if (ent instanceof Tameable && ent instanceof Sittable) {
            if (petInTable(ent)) {
                return updatePet(ent);
            }
            return insertPet(ent);
        }
        return false;
    }
    
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
    
    private List<PetStorage> getPetsList(ResultSet rs) {
        List<PetStorage> ret = new ArrayList<PetStorage>();
        try {
            while (rs.next()) {
                ret.add(new PetStorage(rs.getString("pet_id"), rs.getInt("pet_type"), rs.getInt("pet_x"), rs.getInt("pet_y"), rs.getInt("pet_z"), rs.getString("pet_world"), rs.getString("owner_id")));
            }
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception generating list from database results", e.getMessage());
        }
        return ret;
    }
    
    /*
     *      LOST AND FOUND REGION STATEMENTS
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
    
    public boolean deleteLostRegion(LostAndFoundRegion lfr) {
        if (database.deletePrepStatement(deleteLost, lfr.getZoneName())) {
            thisPlugin.getLogger().info("Lost and found region " + lfr.getZoneName() + " removed from database.");
            return true;
        } else {
            thisPlugin.getLogger().info("Lost and found region " + lfr.getZoneName() + " can't be removed from database.");
            return false;
        }
    }
    
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
    
    public boolean insertProtectedRegion(ProtectedRegion pr) {
        if (database.insertPrepStatement(insertProtected, pr.getZoneName(), pr.getEnterMessage(), pr.getMinLoc().getBlockX(), pr.getMinLoc().getBlockY(), pr.getMinLoc().getBlockZ(), pr.getMaxLoc().getBlockX(), pr.getMaxLoc().getBlockY(), pr.getMaxLoc().getBlockZ(), pr.getWorldName(), pr.getLfName())) {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " added to database.");
            return true;
        } else {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " can't be added to database.");
            return false;
        }
    }
    
    public boolean deleteProtectedRegion(ProtectedRegion pr) {
        if (database.deletePrepStatement(deleteProtected, pr.getZoneName())) {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " removed from database.");
            return true;
        } else {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " can't be removed from database.");
            return false;
        }
    }
    
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
    
    public boolean updateProtectedRegion(ProtectedRegion pr) {
        if (database.updatePrepStatement(updateProtected, pr.getLfName(), pr.getZoneName())) {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " updated in database.");
            return true;
        } else {
            thisPlugin.getLogger().info("Protected region " + pr.getZoneName() + " can't be updated in database.");
            return true;
        }
    }
    
    public DBFrame getRealDatabase() {
        return database;
    }
}
