package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A class that interfaces with the SQLite File
 * @author GatheringExp
 *
 */
public class SQLiteWrapper extends SQLWrapper {
    private final String dbPath;
    private final String dbName;

    /**
     * The initializer storing all the data needed for the SQLite connection.
     * @param dbPath The path to the SQLite database.
     * @param dbName The name of the database file itself, without the file extension.
     * @param thisPlugin A reference to the TPPets plugin instance.
     */
    public SQLiteWrapper(String dbPath, String dbName, TPPets thisPlugin) {
        super(thisPlugin);
        this.dbPath = dbPath;
        this.dbName = dbName;
    }

    public void makeDatabaseDirectory() throws SQLException {
        File dbDir = new File(this.dbPath);
        SQLException exception = new SQLException("Could not access database directory");
        try {
            if (!dbDir.exists() && !dbDir.mkdir()) {
                throw exception;
            }
        } catch (SecurityException ignored) {
            throw exception;
        }

    }
    
    @Override
    public Connection getConnection() throws SQLException {
        try {
            if (this.dbPath == null || this.dbName == null) {
                throw new SQLException("Invalid database path");
            }

            makeDatabaseDirectory();

            return DriverManager.getConnection(getJDBCPath());

        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't connect to SQLite database - " + exception.getMessage());
            throw exception;
        }
    }

    private String getJDBCPath() {
        return "jdbc:sqlite:" + this.dbPath + File.separator + this.dbName + ".db";
    }

}
