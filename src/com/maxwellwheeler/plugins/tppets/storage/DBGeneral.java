package com.maxwellwheeler.plugins.tppets.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;

import com.maxwellwheeler.plugins.tppets.TPPets;

/**
 * A general class that governs the classes {@link MySQLFrame} and {@link SQLiteFrame}.
 * @author GatheringExp
 *
 */
public abstract class DBGeneral implements DBFrame {
    protected TPPets thisPlugin;
    
    /**
     * General constructor. Stores a reference to the TPPets plugin instance.
     * @param thisPlugin TPPets plugin instance.
     */
    public DBGeneral(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }
    
    /**
     * Gets a connection to the database
     */
    public abstract Connection getConnection();
    
    /**
     * Executes an insert prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     */
    public boolean insertPrepStatement(String prepStatement, Object... args) {
        try {
            return 1 == executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't execute insert statement: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a select prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     */
    public ResultSet selectPrepStatement(Connection dbConn, String prepStatement, Object... args) {
        try {
            return executeQuery(dbConn, prepStatement, args);
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't execute select statement: " + e.getMessage());
            return null;
        }
    }

    /**
     * Executes a delete prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     */
    public boolean deletePrepStatement(String prepStatement, Object... args) {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't execute delete statement: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes an update prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     */
    public boolean updatePrepStatement(String prepStatement, Object... args) {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't execute update statement: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a create statement in the database.
     * @param prepStatement A string representing the statement.
     * @return True if successful, false if not.
     */
    public boolean createStatement(String statement) {
        Connection dbConn = getConnection();
        if (dbConn != null) {
            try {
                Statement stmt = dbConn.createStatement();
                int tempInt = stmt.executeUpdate(statement);
                dbConn.close();
                return 0 == tempInt;
            } catch (SQLException e) {
                thisPlugin.getLogger().log(Level.SEVERE, "Can't execute create statement: " + e.getMessage());
            }
        }
        return false;
    }
    
    /**
     * Executes an update prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     */
    protected int executeUpdate(String prepStatement, Object... args) throws SQLException {
        Connection dbConn = getConnection();
        if (dbConn != null) {
            PreparedStatement pstmt = dbConn.prepareStatement(prepStatement);
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) args[i]);
                } else if (args[i] instanceof String) {
                    pstmt.setString(i + 1, (String) args[i]);
                } else {
                    pstmt.setNull(i + 1, Types.NULL);
                    pstmt.close();
                    dbConn.close();
                    return -1;
                }
            }
            int result = pstmt.executeUpdate();
            dbConn.close();
            return result;
        }
        return -1;
    }
    
    /**
     * Passes a general query statement through the provided dbConn. Acts as a mediator between the database object and the calling method.
     * @param dbConn An active connection to a database, presumably provided by a getConnection() implementation.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return A ResultSet representing the values returned from the database. This requires the connection to remain open, so it takes the dbConn as an argument.
     */
    protected ResultSet executeQuery(Connection dbConn, String prepStatement, Object... args) throws SQLException {
        if (dbConn != null) {
            PreparedStatement pstmt = dbConn.prepareStatement(prepStatement);
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Integer) {
                    pstmt.setInt(i + 1, (Integer) args[i]);
                } else if (args[i] instanceof String) {
                    pstmt.setString(i + 1, (String) args[i]);
                } else if (args[i] == null) {
                    pstmt.setNull(i + 1, Types.NULL);
                } else {
                    pstmt.close();
                    dbConn.close();
                    return null;
                }
            }
            return pstmt.executeQuery();
        }
        return null;
    }
}
