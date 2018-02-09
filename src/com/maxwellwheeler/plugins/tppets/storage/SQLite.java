package com.maxwellwheeler.plugins.tppets.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.Hashtable;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.region.LostAndFoundRegion;
import com.maxwellwheeler.plugins.tppets.region.ProtectedRegion;

public class SQLite {
    private TPPets plugin;
    private String dbPath;
    private String dbName;
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
    private String makeTableRestrictedRegions = "CREATE TABLE IF NOT EXISTS restrictedregions (\n"
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
    private String insertPetPrep = "INSERT INTO unloadedpets(petId, petType, petX, petY, petZ, petWorld, ownerId) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private String selectPetFromOwnerStmt = "SELECT * FROM unloadedpets WHERE ownerId = \"%s\"";
    private String selectPetGenericStmt = "SELECT * FROM unloadedpets WHERE ownerId = \"%s\" AND petWorld = \"%s\" AND petType = %d";
    private String selectPetFromUUIDsStmt = "SELECT * FROM unloadedpets WHERE petId = \"%s\" AND ownerId = \"%s\"";
    private String deletePetPrep = "DELETE FROM unloadedpets WHERE petId = ? AND ownerId=?";
    private String updatePetPrep = "UPDATE unloadedpets SET petX = ?, petY = ?, petZ = ?, petWorld = ? WHERE petId = ? AND ownerId = ?";
    private String insertLostPrep = "INSERT INTO lostregions(zoneName, minX, minY, minZ, maxX, maxY, maxZ, worldName) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private String deleteLostPrep = "DELETE FROM lostregions WHERE zoneName = ?";
    private String selectLostPrep = "SELECT * FROM lostregions";
    private String insertRestrictedPrep = "INSERT INTO restrictedregions(zoneName, enterMessage, minX, minY, minZ, maxX, maxY, maxZ, worldName, lfZoneName) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private String deleteRestrictedPrep = "DELETE FROM restrictedregions WHERE zoneName = ?";
    private String selectRestrictedPrep = "SELECT * FROM restrictedregions";
    private Connection dbc;
    
    public SQLite (TPPets plugin, String dbPath, String dbName) {
        this.plugin = plugin;
        this.dbPath = dbPath;
        this.dbName = dbName;
    }
    
    public Connection getDBC() {
        File dbDir = new File(dbPath);
        
        if (!dbDir.exists()) {
            try {
                dbDir.mkdir();
            } catch (SecurityException e) {
                logSevere("Security Exception", "establishing database connection", e);
            }
        }
        
        try {
            dbc = DriverManager.getConnection(getJDBCPath());
        } catch (SQLException e) {
            logSevere("SQL Exception", "establishing database connection", e);
        }
        
        return dbc;
    }
    
    public void createDatabase() {
        Connection dbc = getDBC();
        if (dbc != null) {
            try {
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "establishing database connection", e);
            }
        }
    }
    
    public void createTables() {
        Connection dbc = getDBC();
        if (dbc != null) {
            try {
                Statement makeTableStmt = dbc.createStatement();
                makeTableStmt.execute(makeTableUnloadedPets);
                makeTableStmt.execute(makeTableLostRegions);
                makeTableStmt.execute(makeTableRestrictedRegions);
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "creating table", e);
            }
        }
    }
    
    public void insertRestrictedRegion(ProtectedRegion pr) {
        Connection dbc = getDBC();
        if (dbc != null) {
            try {
                PreparedStatement insertPStatement = dbc.prepareStatement(insertRestrictedPrep);
                insertPStatement.setString(1, pr.getZoneName());
                insertPStatement.setString(2, pr.getEnterMessage());
                insertPStatement.setInt(3, pr.getMinLoc().getBlockX());
                insertPStatement.setInt(4, pr.getMinLoc().getBlockY());
                insertPStatement.setInt(5, pr.getMinLoc().getBlockZ());
                insertPStatement.setInt(6, pr.getMaxLoc().getBlockX());
                insertPStatement.setInt(7, pr.getMaxLoc().getBlockY());
                insertPStatement.setInt(8, pr.getMaxLoc().getBlockZ());
                insertPStatement.setString(9, pr.getWorldName());
                insertPStatement.setString(10, pr.getLfReference().getZoneName());
                insertPStatement.executeUpdate();
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "inserting restricted region into database", e);
            }
        }
    }
    
    public void insertLostRegion(LostAndFoundRegion lfr) {
        Connection dbc = getDBC();
        if (dbc != null) {
            try {
                System.out.println(String.format("INSERT INTO lostregions(zoneName, minX, minY, minZ, maxX, maxY, maxZ, worldName) VALUES (%s, %d, %d, %d, %d, %d, %d, %s)", lfr.getZoneName(), lfr.getMinLoc().getBlockX(), lfr.getMinLoc().getBlockY(), lfr.getMinLoc().getBlockZ(), lfr.getMaxLoc().getBlockX(), lfr.getMaxLoc().getBlockY(), lfr.getMaxLoc().getBlockZ(), lfr.getWorldName()));
                PreparedStatement insertPStatement = dbc.prepareStatement(insertLostPrep);
                insertPStatement.setString(1, lfr.getZoneName());
                insertPStatement.setInt(2, lfr.getMinLoc().getBlockX());
                insertPStatement.setInt(3, lfr.getMinLoc().getBlockY());
                insertPStatement.setInt(4, lfr.getMinLoc().getBlockZ());
                insertPStatement.setInt(5, lfr.getMaxLoc().getBlockX());
                insertPStatement.setInt(6, lfr.getMaxLoc().getBlockY());
                insertPStatement.setInt(7, lfr.getMaxLoc().getBlockZ());
                insertPStatement.setString(8, lfr.getWorldName());
                insertPStatement.executeUpdate();
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "inserting lost and found region into database", e);
            }
        }
    }
    
    public ArrayList<ProtectedRegion> getProtectedRegions() {
        Connection dbc = getDBC();
        ArrayList<ProtectedRegion> ret = new ArrayList<ProtectedRegion>();
        if (dbc != null) {
            try {
                PreparedStatement selectPStatement = dbc.prepareStatement(selectRestrictedPrep);
                ResultSet rs = selectPStatement.executeQuery();
                while (rs.next()) {
                    // TODO replace null with reference
                    ret.add(new ProtectedRegion(rs.getString("zoneName"), rs.getString("enterMessage"), rs.getString("worldName"), rs.getInt("minX"), rs.getInt("minY"), rs.getInt("minZ"), rs.getInt("maxX"), rs.getInt("maxY"), rs.getInt("maxZ"), rs.getString("lfZoneName")));
                }
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "selecting restricted regions from database", e);
            }
        }
        return ret;
    }
    
    public Hashtable<String, LostAndFoundRegion> getLostRegions() {
        Connection dbc = getDBC();
        Hashtable<String, LostAndFoundRegion> ret = new Hashtable<String, LostAndFoundRegion>();
        if (dbc != null) {
            try {
                PreparedStatement selectPStatement = dbc.prepareStatement(selectLostPrep);
                ResultSet rs = selectPStatement.executeQuery();
                while (rs.next()) {
                    ret.put(rs.getString("zoneName"), new LostAndFoundRegion(rs.getString("zoneName"), rs.getString("worldName"), rs.getInt("minX"), rs.getInt("minY"), rs.getInt("minZ"), rs.getInt("maxX"), rs.getInt("maxY"), rs.getInt("maxZ")));
                }
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "selecting restricted regions from database", e);
            }
        }
        return ret;
    }
    
    public void deleteRestrictedRegion(ProtectedRegion pr) {
        try {
            deleteRegion(true, pr.getZoneName());
        } catch (SQLException e) {
            logSevere("SQL Exception", "deleting protected region from database", e);
        }
    }
    
    public void deleteLostRegion(LostAndFoundRegion lfr) {
        try {
            deleteRegion(false, lfr.getZoneName());
        } catch (SQLException e) {
            logSevere("SQL Exception", "deleting lost region from database", e);
        }
    }
    
    private void deleteRegion(boolean isRestrictedRegion, String regionName) throws SQLException {
        Connection dbc = getDBC();
        if (dbc != null) {
            PreparedStatement deleteRegionPStatement = dbc.prepareStatement(isRestrictedRegion ? deleteRestrictedPrep : deleteLostPrep);
            deleteRegionPStatement.setString(1, regionName);
            deleteRegionPStatement.executeUpdate();
            dbc.close();
        }
    }
    
    public void insertPet(Entity entity) {
        Connection dbc = getDBC();
        if (dbc != null) {
            if (entity instanceof Tameable && entity instanceof Sittable) {
                Tameable tameableTemp = (Tameable) entity;
                if (tameableTemp.isTamed()) {
                    int entityTypeIndex = PetType.getIndexFromPet(PetType.Pets.UNKNOWN);
                    if (entity instanceof Ocelot) {
                        entityTypeIndex = PetType.getIndexFromPet(PetType.Pets.CAT);
                    } else if (entity instanceof Wolf) {
                        entityTypeIndex = PetType.getIndexFromPet(PetType.Pets.DOG);
                    } else if (entity instanceof Parrot) {
                        entityTypeIndex = PetType.getIndexFromPet(PetType.Pets.PARROT);
                    }
                    
                    try {
                        System.out.println(String.format("INSERT INTO unloadedpets(petId, petType, petX, petY, petZ, petWorld, ownerId) VALUES (%s, %d, %d, %d, %d, %s, %s)", shortenUUID(entity.getUniqueId().toString()), entityTypeIndex, entity.getLocation().getBlockX(), entity.getLocation().getBlockY(), entity.getLocation().getBlockZ(), entity.getWorld().getName(), shortenUUID(tameableTemp.getOwner().getUniqueId().toString())));
                        PreparedStatement insertPStatement = dbc.prepareStatement(insertPetPrep);
                        insertPStatement.setString(1, shortenUUID(entity.getUniqueId().toString()));
                        insertPStatement.setInt(2, entityTypeIndex);
                        insertPStatement.setInt(3, entity.getLocation().getBlockX());
                        insertPStatement.setInt(4, entity.getLocation().getBlockY());
                        insertPStatement.setInt(5, entity.getLocation().getBlockZ());
                        insertPStatement.setString(6, entity.getWorld().getName());
                        insertPStatement.setString(7, shortenUUID(tameableTemp.getOwner().getUniqueId().toString()));
                        insertPStatement.executeUpdate();
                    } catch (SQLException e) {
                        logSevere("SQL Exception", "inserting pet into database", e);
                    }
                }
            }
            try {
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "inserting pet into database", e);
            }
        }
    }
    
    public void deletePet(UUID petId, UUID playerId) {
        Connection dbc = getDBC();
        if (dbc != null) {
            String petIdString = shortenUUID(petId.toString());
            String playerIdString = shortenUUID(playerId.toString());
            try {
                System.out.println(String.format("DELETE FROM unloadedpets WHERE petId = %s AND ownerId=%s", petIdString, playerIdString));
                PreparedStatement pstmt = dbc.prepareStatement(deletePetPrep);
                pstmt.setString(1, petIdString);
                pstmt.setString(2, playerIdString);
                pstmt.executeUpdate();
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "deleting pet from database", e);
            }
        }
    }
    
    public void updateOrInsertPet (Entity entity) {
        if (isPetAlreadyInTable(entity)) {
            updatePet(entity);
        } else {
            insertPet(entity);
        }
    }
    
    public void updatePet(Entity ent) {
        Connection dbc = getDBC();
        if (dbc != null) {
            if (ent instanceof Tameable) {
                Tameable tameableTemp = (Tameable) ent;
                String petIdString = shortenUUID(ent.getUniqueId().toString());
                String playerIdString = shortenUUID(tameableTemp.getOwner().getUniqueId().toString());
                try {
                    PreparedStatement pstmt = dbc.prepareStatement(updatePetPrep);
                    pstmt.setInt(1, ent.getLocation().getBlockX());
                    pstmt.setInt(2, ent.getLocation().getBlockY());
                    pstmt.setInt(3, ent.getLocation().getBlockZ());
                    pstmt.setString(4, ent.getWorld().getName());
                    pstmt.setString(5, petIdString);
                    pstmt.setString(6, playerIdString);
                    pstmt.executeUpdate();
                    dbc.close();
                } catch (SQLException e) {
                    logSevere("SQL Exception", "updating pet entry in database", e);
                }
            }
            try {
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "closing database connection", e);
            }
        }
    }
    
    public boolean isPetAlreadyInTable(Entity ent) {
        Connection dbc = getDBC();
        if (dbc != null) {
            if (ent instanceof Tameable) {
                Tameable tameableTemp = (Tameable) ent;
                String petIdString = shortenUUID(ent.getUniqueId().toString());
                String playerIdString = shortenUUID(tameableTemp.getOwner().getUniqueId().toString());
                try {
                    Statement stmt = dbc.createStatement();
                    ResultSet rs = stmt.executeQuery(String.format(selectPetFromUUIDsStmt, petIdString, playerIdString));
                    boolean ret = rs.next();
                    dbc.close();
                    return ret;
                } catch (SQLException e) {
                    logSevere("SQL Exception", "selecting pets from database", e);
                }
            }
            try {
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "closing database connection", e);
            }
        }
        return false;
    }
    
    public ArrayList<PetStorage> getPetsFromUUID(UUID userId) {
        Connection dbc = getDBC();
        if (dbc != null) {
            String userIdString = shortenUUID(userId.toString());
            try {
                Statement stmt = dbc.createStatement();
                ArrayList<PetStorage> ret = new ArrayList<PetStorage>();
                ret = getPetsList(stmt.executeQuery(String.format(selectPetFromOwnerStmt, userIdString)));
                dbc.close();
                return ret;
            } catch (SQLException e) {
                logSevere("SQL Exception", "selecting pets from database", e);
            }
        }
        return null;
    }
    
    public ArrayList<PetStorage> getPetsGeneric(PetType.Pets pt, String worldName, String ownerId) {
        Connection dbc = getDBC();
        if (dbc != null) {
            String userIdString = shortenUUID(ownerId);
            try {
                Statement stmt = dbc.createStatement();
                ArrayList<PetStorage> ret = new ArrayList<PetStorage>();
                ret = getPetsList(stmt.executeQuery(String.format(selectPetGenericStmt, userIdString, worldName, PetType.getIndexFromPet(pt))));
                dbc.close();
                return ret;
            } catch (SQLException e) {
                logSevere("SQL Exception", "selecting pets from database", e);
            }
        }
        return null;
    }
    
    private ArrayList<PetStorage> getPetsList(ResultSet rs) {
        ArrayList<PetStorage> ret = new ArrayList<PetStorage>();
        try {
            while (rs.next()) {
                ret.add(new PetStorage(rs.getString("petId"), rs.getInt("petType"), rs.getInt("petX"), rs.getInt("petY"), rs.getInt("petZ"), rs.getString("petWorld"), rs.getString("ownerId")));
            }
        } catch (SQLException e) {
            logSevere("SQL Exception", "generating list from database results", e);
        }
        return ret;
    }
    
    private String shortenUUID(String longUUID) {
        return longUUID.replace("-", "");
    }
    
    private void logSevere(String exceptionType, String exceptionWhile, Exception e) {
        plugin.getLogger().log(Level.SEVERE, exceptionType + " while " + exceptionWhile + ": " + e.getMessage());
    }
    
    private String getJDBCPath() {
        return "jdbc:sqlite:" + dbPath + "\\" + dbName + ".db";
    }
}
