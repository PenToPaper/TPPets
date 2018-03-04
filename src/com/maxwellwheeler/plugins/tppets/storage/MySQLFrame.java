package com.maxwellwheeler.plugins.tppets.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;

import com.maxwellwheeler.plugins.tppets.TPPets;

public class MySQLFrame extends DBGeneral {
    private String host;
    private int port;
    private String dbName;
    private String dbUsername;
    private String dbPassword;
    private TPPets thisPlugin;
    
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
            return DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + dbName, dbUsername, dbPassword);
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't connect to MySQL database: " + dbName + " error: " + e.getMessage());
        }
        return null;
    }
}
