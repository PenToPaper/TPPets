package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * A class that interfaces with the SQLite File
 * @author GatheringExp
 *
 */
public class SQLiteFrame extends DBGeneral {
    private String dbPath;
    private String dbName;
    
    /**
     * The initializer storing all the data needed for the SQLite connection.
     * @param dbPath The path to the SQLite database.
     * @param dbName The name of the database file itself, without the file extension.
     * @param thisPlugin A reference to the TPPets plugin instance.
     */
    public SQLiteFrame(String dbPath, String dbName, TPPets thisPlugin) {
        super(thisPlugin);
        this.dbPath = dbPath;
        this.dbName = dbName;
        this.thisPlugin = thisPlugin;
    }
    
    @Override
    public Connection getConnection() {
        File dbDir = new File(dbPath);
        if (!dbDir.exists()) {
            try {
                dbDir.mkdir();
            } catch (SecurityException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "Security Exception creating database" + e.getMessage());
            }
        }
        
        try {
            Connection dbConn = DriverManager.getConnection(getJDBCPath());
            return dbConn;
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "SQL Exception creating database" + e.getMessage());
            return null;
        }
    }

    private String getJDBCPath() {
        return "jdbc:sqlite:" + dbPath + "\\" + dbName + ".db";
    }

}
