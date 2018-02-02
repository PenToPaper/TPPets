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

import org.bukkit.entity.Entity;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class SQLite {
    private TPPets plugin;
    private String dbPath;
    private String dbName;
    private String makeTable = "CREATE TABLE IF NOT EXISTS unloadedpets (\n"
            + "petId CHAR(32) PRIMARY KEY,\n"
            + "petType TINYINT NOT NULL,\n"
            + "petX INT NOT NULL,\n"
            + "petY INT NOT NULL,\n"
            + "petZ INT NOT NULL,\n"
            + "petWorld VARCHAR(25) NOT NULL,\n"
            + "ownerId CHAR(32) NOT NULL\n"
            + ");";
    private String insertPrepStatement = "INSERT INTO unloadedpets(petId, petType, petX, petY, petZ, petWorld, ownerId) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private String selectStatement = "SELECT * FROM unloadedpets WHERE ownerId = \"%s\"";
    private String selectStatementGeneric = "SELECT * FROM unloadedpets WHERE ownerId = \"%s\" AND petWorld = \"%s\" AND petType = %d";
    private String deletePrepStatement = "DELETE FROM unloadedpets WHERE petId = ? AND ownerId=?";
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
    
    public void createTable() {
        Connection dbc = getDBC();
        if (dbc != null) {
            try {
                Statement makeTableStmt = dbc.createStatement();
                makeTableStmt.execute(makeTable);
                dbc.close();
            } catch (SQLException e) {
                logSevere("SQL Exception", "creating table", e);
            }
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
                        System.out.println(String.format("INSERT INTO unloadedpets(petId, petType, petX, petY, petZ, petWorld, ownerId) VALUES (%s, %d, %d, %d, %d, %s, %s)", shortenUUID(entity.getUniqueId().toString()), entityTypeIndex, entity.getLocation().getBlockX(), entity.getLocation().getBlockY(), entity.getLocation().getBlockZ(), entity.getWorld().toString(), shortenUUID(tameableTemp.getOwner().getUniqueId().toString())));
                        PreparedStatement insertPStatement = dbc.prepareStatement(insertPrepStatement);
                        insertPStatement.setString(1, shortenUUID(entity.getUniqueId().toString()));
                        insertPStatement.setInt(2, entityTypeIndex);
                        insertPStatement.setInt(3, entity.getLocation().getBlockX());
                        insertPStatement.setInt(4, entity.getLocation().getBlockY());
                        insertPStatement.setInt(5, entity.getLocation().getBlockZ());
                        insertPStatement.setString(6, entity.getWorld().toString());
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
    
    public void deleteEntry(UUID petId, UUID playerId) {
        Connection dbc = getDBC();
        if (dbc != null) {
            String petIdString = shortenUUID(petId.toString());
            String playerIdString = shortenUUID(playerId.toString());
            try {
                System.out.println(String.format("DELETE FROM unloadedpets WHERE petId = %s AND ownerId=%s", petIdString, playerIdString));
                PreparedStatement pstmt = dbc.prepareStatement(deletePrepStatement);
                pstmt.setString(1, petIdString);
                pstmt.setString(2, playerIdString);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logSevere("SQL Exception", "deleting pet from database", e);
            }
        }
    }
    
    public ArrayList<PetStorage> selectFromUUID(UUID userId) {
        Connection dbc = getDBC();
        if (dbc != null) {
            String userIdString = shortenUUID(userId.toString());
            try {
                Statement stmt = dbc.createStatement();
                ArrayList<PetStorage> ret = new ArrayList<PetStorage>();
                ret = getAnimalsList(stmt.executeQuery(String.format(selectStatement, userIdString)));
                dbc.close();
                return ret;
            } catch (SQLException e) {
                logSevere("SQL Exception", "selecting pets from database", e);
            }
        }
        return null;
    }
    
    public ArrayList<PetStorage> selectGeneric(PetType.Pets pt, String worldName, String ownerId) {
        Connection dbc = getDBC();
        if (dbc != null) {
            String userIdString = shortenUUID(ownerId);
            try {
                Statement stmt = dbc.createStatement();
                ArrayList<PetStorage> ret = new ArrayList<PetStorage>();
                ret = getAnimalsList(stmt.executeQuery(String.format(selectStatementGeneric, userIdString, worldName, PetType.getIndexFromPet(pt))));
                dbc.close();
                return ret;
            } catch (SQLException e) {
                logSevere("SQL Exception", "selecting pets from database", e);
            }
        }
        return null;
    }
    
    private ArrayList<PetStorage> getAnimalsList(ResultSet rs) {
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
    
    public String shortenUUID(String longUUID) {
        return longUUID.replace("-", "");
    }
    
    private void logSevere(String exceptionType, String exceptionWhile, Exception e) {
        plugin.getLogger().log(Level.SEVERE, exceptionType + " while " + exceptionWhile + ": " + e.getMessage());
    }
    
    private String getJDBCPath() {
        return "jdbc:sqlite:" + dbPath + "\\" + dbName + ".db";
    }
}
