package com.maxwellwheeler.plugins.tppets.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

import com.maxwellwheeler.plugins.tppets.TPPets;

/**
 * A class that interfaces with the MySQL Server
 * @author GatheringExp
 *
 */
public class MySQLFrame extends DBGeneral {
    private String host;
    private int port;
    private String dbName;
    private String dbUsername;
    private String dbPassword;
    
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
    public Connection getConnection() {
        try {
            if (host != null && port >= 0 && port <= 65535 && dbName != null && dbUsername != null && dbPassword != null) {
                return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&useSSL=false", dbUsername, dbPassword);
            }
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't connect to MySQL database:" + e.getMessage());
        }
        return null;
    }
}
