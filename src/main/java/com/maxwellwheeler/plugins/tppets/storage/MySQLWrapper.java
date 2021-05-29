package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Used to interface with a MySQL Server
 * @author GatheringExp
 */
public class MySQLWrapper extends SQLWrapper {
    /** The MySQL host address. */
    private final String host;
    /** The MySQL port number. */
    private final int port;
    /** The MySQL database name. */
    private final String dbName;
    /** The MySQL username. */
    private final String dbUsername;
    /** The MySQL password. */
    private final String dbPassword;
    
    /**
     * Initializes instance variables.
     * @param host The MySQL server host address.
     * @param port The MySQL server port number, between 0 and 65535
     * @param dbName The MySQL database name.
     * @param dbUsername The MySQL user's username.
     * @param dbPassword The MySQL user's password.
     * @param thisPlugin A reference to the active {@link TPPets} instance.
     */
    public MySQLWrapper(String host, int port, String dbName, String dbUsername, String dbPassword, TPPets thisPlugin) {
        super(thisPlugin);
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    /**
     * Gets a database Connection object from this object's MySQL credentials.
     * @return A new {@link Connection} object. Does not return null.
     * @throws SQLException If generating a new connection to the database fails.
     */
    @Override
    public Connection getConnection() throws SQLException {
        try {
            if (host != null && port >= 0 && port <= 65535 && dbName != null && dbUsername != null && dbPassword != null) {
                return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&useSSL=false", dbUsername, dbPassword);
            }
            throw new SQLException("Invalid database credentials");
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't connect to MySQL database - " + exception.getMessage());
            throw exception;
        }
    }
}
