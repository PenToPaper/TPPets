package com.maxwellwheeler.plugins.tppets.storage;

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
    private String makeTableUnloadedPets = "CREATE TABLE IF NOT EXISTS unloadedpets (\n"
            + "petId CHAR(32) PRIMARY KEY,\n"
            + "petType TINYINT NOT NULL,\n"
            + "petX INT NOT NULL,\n"
            + "petY INT NOT NULL,\n"
            + "petZ INT NOT NULL,\n"
            + "petWorld VARCHAR(25) NOT NULL,\n"
            + "ownerId CHAR(32) NOT NULL\n"
            + ");";
    private String makeTableLostRegions = "CREATE TABLE IF NOT EXISTS lostregions (\n"
            + "zoneName VARCHAR(64) PRIMARY KEY,\n"
            + "minX INT NOT NULL,\n"
            + "minY INT NOT NULL,\n"
            + "minZ INT NOT NULL,\n"
            + "maxX INT NOT NULL,\n"
            + "maxY INT NOT NULL,\n"
            + "maxZ INT NOT NULL,\n"
            + "worldName VARCHAR(25) NOT NULL);";
    private String makeTableProtectedRegions = "CREATE TABLE IF NOT EXISTS protectedregions (\n"
            + "zoneName VARCHAR(64) PRIMARY KEY,\n"
            + "enterMessage VARCHAR(255),\n"
            + "minX INT NOT NULL,\n"
            + "minY INT NOT NULL,\n"
            + "minZ INT NOT NULL,\n"
            + "maxX INT NOT NULL,\n"
            + "maxY INT NOT NULL,\n"
            + "maxZ INT NOT NULL,\n"
            + "worldName VARCHAR(25) NOT NULL,\n"
            + "lfZoneName VARCHAR(64),\n"
            + "FOREIGN KEY(lfZoneName) REFERENCES lostregions(zoneName));";
    
    /*
     *      UNLOADEDPETS STATEMENTS
     */
    private String insertPet = "INSERT INTO unloadedpets(petId, petType, petX, petY, petZ, petWorld, ownerId) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private String deletePet = "DELETE FROM unloadedpets WHERE petId = ? AND ownerId=?";
    private String updatePet = "UPDATE unloadedpets SET petX = ?, petY = ?, petZ = ?, petWorld = ? WHERE petId = ? AND ownerId = ?";
    private String selectPetFromUuid = "SELECT * FROM unloadedpets WHERE petId = ?";
    
    private String selectPetsFromOwner = "SELECT * FROM unloadedpets WHERE ownerId = ?";
    private String selectPetsFromUuids = "SELECT * FROM unloadedpets WHERE petId = ? AND ownerId = ?";
    private String selectPetsGeneric = "SELECT * FROM unloadedpets WHERE ownerId = ? AND petWorld = ? AND petType = ?";
    private String selectPetsFromWorld = "SELECT * FROM unloadedpets WHERE petWorld = ?";
    
    /*
     *      LOST AND FOUND REGION STATEMENTS
     */
    private String insertLost = "INSERT INTO lostregions(zoneName, minX, minY, minZ, maxX, maxY, maxZ, worldName) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private String deleteLost = "DELETE FROM lostregions WHERE zoneName = ?";
    private String selectLostPrep = "SELECT * FROM lostregions";
    
    /*
     *      PROTECTED REGION STATEMENTS
     */
    private String insertProtected = "INSERT INTO protectedregions(zoneName, enterMessage, minX, minY, minZ, maxX, maxY, maxZ, worldName, lfZoneName) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private String deleteProtected = "DELETE FROM protectedregions WHERE zoneName = ?";
    private String selectProtected = "SELECT * FROM protectedregions";
    private String updateProtected = "UPDATE protectedregions SET lfZoneName = ? WHERE zoneName = ?";
    
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
     *      UNLOADEDPETS METHODS
     */
    
    public boolean insertPet(Entity ent) {
        if (ent instanceof Tameable && ent instanceof Sittable) {
            Tameable tameableTemp = (Tameable) ent;
            String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
            String trimmedPlayerUUID = UUIDUtils.trimUUID(tameableTemp.getOwner().getUniqueId());
            int petTypeIndex = PetType.getIndexFromPet(PetType.getEnumByEntity(ent));
            return database.insertPrepStatement(insertPet, trimmedEntUUID, petTypeIndex, ent.getLocation().getBlockX(), ent.getLocation().getBlockY(), ent.getLocation().getBlockZ(), ent.getWorld().getName(), trimmedPlayerUUID);
        }
        return false;
    }
    
    public boolean deletePet(Entity ent) {
        if (ent instanceof Tameable && ent instanceof Sittable) {
            Tameable tameableTemp = (Tameable) ent;
            String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
            String trimmedPlayerUUID = UUIDUtils.trimUUID(tameableTemp.getOwner().getUniqueId());
            return database.deletePrepStatement(deletePet, trimmedEntUUID, trimmedPlayerUUID);
        }
        return false;
    }
    
    public boolean updatePet(Entity ent) {
        if (ent instanceof Tameable && ent instanceof Sittable) {
            Tameable tameableTemp = (Tameable) ent;
            String trimmedEntUUID = UUIDUtils.trimUUID(ent.getUniqueId());
            String trimmedPlayerUUID = UUIDUtils.trimUUID(tameableTemp.getOwner().getUniqueId());
            return database.updatePrepStatement(updatePet, ent.getLocation().getBlockX(), ent.getLocation().getBlockY(), ent.getLocation().getBlockZ(), ent.getLocation().getWorld().getName(), trimmedEntUUID, trimmedPlayerUUID);
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
        try {
            return database.selectPrepStatement(selectPetFromUuid, trimmedEntUUID).next();
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception selecting pet from table: " + e.getMessage());
            return false;
        }
    }
    
    public List<PetStorage> getPetsFromOwner(String uuid) { 
        String trimmedUuid = UUIDUtils.trimUUID(uuid);
        return getPetsList(database.selectPrepStatement(selectPetsFromOwner, trimmedUuid));
    }
    
    public List<PetStorage> getPetsFromUUIDs(String petUuid, String playerUuid) {
        String trimmedPetUuid = UUIDUtils.trimUUID(petUuid);
        String trimmedPlayerUuid = UUIDUtils.trimUUID(playerUuid);
        return getPetsList(database.selectPrepStatement(selectPetsFromUuids, trimmedPetUuid, trimmedPlayerUuid));
    }
    
    public List<PetStorage> getPetsGeneric(String playerUuid, String worldName, PetType.Pets petType) {
        String trimmedPlayerUuid = UUIDUtils.trimUUID(playerUuid);
        return getPetsList(database.selectPrepStatement(selectPetsGeneric, trimmedPlayerUuid, worldName, PetType.getIndexFromPet(petType)));
    }
    
    public List<PetStorage> getPetsFromWorld(String worldName) {
        return getPetsList(database.selectPrepStatement(selectPetsFromWorld, worldName));
    }
    
    private List<PetStorage> getPetsList(ResultSet rs) {
        List<PetStorage> ret = new ArrayList<PetStorage>();
        try {
            while (rs.next()) {
                ret.add(new PetStorage(rs.getString("petId"), rs.getInt("petType"), rs.getInt("petX"), rs.getInt("petY"), rs.getInt("petZ"), rs.getString("petWorld"), rs.getString("ownerId")));
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
        return database.insertPrepStatement(insertLost, lfr.getZoneName(), lfr.getMinLoc().getBlockX(), lfr.getMinLoc().getBlockY(), lfr.getMinLoc().getBlockZ(), lfr.getMaxLoc().getBlockX(), lfr.getMaxLoc().getBlockY(), lfr.getMaxLoc().getBlockZ(), lfr.getWorldName());
    }
    
    public boolean deleteLostRegion(LostAndFoundRegion lfr) {
        return database.deletePrepStatement(deleteLost, lfr.getZoneName());
    }
    
    public Hashtable<String, LostAndFoundRegion> getLostRegions() {
        Hashtable<String, LostAndFoundRegion> ret = new Hashtable<String, LostAndFoundRegion>();
        ResultSet rs = database.selectPrepStatement(selectLostPrep);
        try {
            while (rs.next()) {
                ret.put(rs.getString("zoneName"), new LostAndFoundRegion(rs.getString("zoneName"), rs.getString("worldName"), rs.getInt("minX"), rs.getInt("minY"), rs.getInt("minZ"), rs.getInt("maxX"), rs.getInt("maxY"), rs.getInt("maxZ")));
            }
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception getting lost and found regions");
        }
        return ret;
    }
    
    /*
     *      PROTECTED REGION STATEMENTS
     */
    
    public boolean insertProtectedRegion(ProtectedRegion pr) {
        return database.insertPrepStatement(insertProtected, pr.getZoneName(), pr.getEnterMessage(), pr.getMinLoc().getBlockX(), pr.getMinLoc().getBlockY(), pr.getMinLoc().getBlockZ(), pr.getMaxLoc().getBlockX(), pr.getMaxLoc().getBlockY(), pr.getMaxLoc().getBlockZ(), pr.getWorldName(), pr.getLfName());
    }
    
    public boolean deleteProtectedRegion(ProtectedRegion pr) {
        return database.deletePrepStatement(deleteProtected, pr.getZoneName());
    }
    
    public Hashtable<String, ProtectedRegion> getProtectedRegions() {
        Hashtable<String, ProtectedRegion> ret = new Hashtable<String, ProtectedRegion>();
        ResultSet rs = database.selectPrepStatement(selectProtected);
        try {
            while (rs.next()) {
                ret.put(rs.getString("zoneName"), new ProtectedRegion(rs.getString("zoneName"), rs.getString("enterMessage"), rs.getString("worldName"), rs.getInt("minX"), rs.getInt("minY"), rs.getInt("minZ"), rs.getInt("maxX"), rs.getInt("maxY"), rs.getInt("maxZ"), rs.getString("lfZoneName")));
            }
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception getting lost and found regions");
        }
        return ret;
    }
    
    public boolean updateProtectedRegion(ProtectedRegion pr) {
        return database.updatePrepStatement(updateProtected, pr.getLfName(), pr.getZoneName());
    }
}
