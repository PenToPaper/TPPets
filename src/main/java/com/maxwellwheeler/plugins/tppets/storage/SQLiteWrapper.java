package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Used to interface with a SQLite Server
 * @author GatheringExp
 */
public class SQLiteWrapper extends SQLWrapper {
    /** The path to the SQLite database on disk. */
    private final String dbPath;
    /** The SQLite database name. */
    private final String dbName;

    /**
     * Initializes instance variables.
     * @param dbPath The path to the SQLite database on disk.
     * @param dbName The SQLite database name.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public SQLiteWrapper(String dbPath, String dbName, TPPets thisPlugin) {
        super(thisPlugin);
        this.dbPath = dbPath;
        this.dbName = dbName;
    }

    /**
     * Creates a new directory at {@link SQLiteWrapper#dbPath}.
     * @throws SQLException If unable to create the database directory.
     */
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

    /**
     * Gets a database Connection object from this object's SQLite credentials.
     * @return A new {@link Connection} object. Does not return null.
     * @throws SQLException If generating a new connection to the database fails.
     */
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

    /**
     * Determines the JDBC SQLite url based on {@link SQLiteWrapper#dbPath} and {@link SQLiteWrapper#dbName}.
     * @return A JDBC SQLite url string.
     */
    private String getJDBCPath() {
        return "jdbc:sqlite:" + this.dbPath + File.separator + this.dbName + ".db";
    }

}
