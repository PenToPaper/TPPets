package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A class that interfaces with the MySQL Server
 * @author GatheringExp
 *
 */
public class MySQLFrame extends DBFrame {
    private final String host;
    private final int port;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;
    
    /**
     * The initializer storing all the data needed for the MySQL connection.
     * @param host The host address.
     * @param port The port number, between 0 and 65535
     * @param dbName The name of the database.
     * @param dbUsername The user to use in the connection
     * @param dbPassword The password to use in the connection
     * @param thisPlugin A reference to the TPPets plugin instance
     */
    public MySQLFrame(String host, int port, String dbName, String dbUsername, String dbPassword, TPPets thisPlugin) {
        super(thisPlugin);
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        try {
            if (host != null && port >= 0 && port <= 65535 && dbName != null && dbUsername != null && dbPassword != null) {
                return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&useSSL=false", dbUsername, dbPassword);
            }
            throw new SQLException("Invalid database credentials");
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't connect to MySQL database:" + exception.getMessage());
            throw exception;
        }
    }
}
