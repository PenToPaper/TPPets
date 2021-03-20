package com.maxwellwheeler.plugins.tppets.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;

import java.sql.*;

/**
 * A general class that governs the classes {@link MySQLFrame} and {@link SQLiteFrame}.
 * @author GatheringExp
 *
 */
public abstract class DBFrame {
    protected TPPets thisPlugin;
    
    /**
     * General constructor. Stores a reference to the TPPets plugin instance.
     * @param thisPlugin TPPets plugin instance.
     */
    DBFrame(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }

    abstract public Connection getConnection() throws SQLException;

    /**
     * Executes an insert prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     */
    public boolean insertPrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return 1 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute insert statement: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Executes a select prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     */
    // Removed dbConn as argument, but can re-add if deemed necessary
    public ResultSet selectPrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return executeQuery(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute select statement: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Executes a delete prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     */
    public boolean deletePrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute delete statement: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Executes an update prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     */
    public boolean updatePrepStatement(String prepStatement, Object... args) throws SQLException {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute update statement: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Executes a create statement in the database.
     * @param statement A string representing the statement.
     * @return True if successful, false if not.
     */
    public boolean createStatement(String statement) throws SQLException {
        try (Connection dbConn = getConnection();
             Statement stmt = dbConn.createStatement()) {
            return stmt.executeUpdate(statement) == 0;
        } catch (SQLException exception) {
            this.thisPlugin.getLogWrapper().logErrors("Can't execute create statement: " + exception.getMessage());
            throw exception;
        }
    }

    protected PreparedStatement setPreparedStatementArgs(PreparedStatement preparedStatement, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Integer) {
                preparedStatement.setInt(i + 1, (Integer) args[i]);
            } else if (args[i] instanceof String) {
                preparedStatement.setString(i + 1, (String) args[i]);
            } else {
                throw new SQLException("Invalid argument to prepared statement");
            }
        }
        return preparedStatement;
    }
    
    /**
     * Executes an update prepared statement in the database.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return True if successful, false if not.
     * @throws SQLException Forwards SQLExceptions to be dealt with by the other functions
     */
    protected int executeUpdate(String prepStatement, Object... args) throws SQLException {
        try (Connection dbConn = getConnection();
             PreparedStatement preparedStatement = setPreparedStatementArgs(dbConn.prepareStatement(prepStatement), args)) {
            return preparedStatement.executeUpdate();
        }
    }
    
    /**
     * Passes a general query statement through the provided dbConn. Acts as a mediator between the database object and the calling method.
     * @param prepStatement A string representing the prepared statement.
     * @param args Arguments representing the fillers for the ?s in the prepared statement.
     * @return A ResultSet representing the values returned from the database. This requires the connection to remain open, so it takes the dbConn as an argument.
     * @throws SQLException Forwards SQLExceptions to be dealt with by the other functions
     */
    protected ResultSet executeQuery(String prepStatement, Object... args) throws SQLException {
        try (Connection dbConn = getConnection();
             PreparedStatement preparedStatement = setPreparedStatementArgs(dbConn.prepareStatement(prepStatement), args)) {
            return preparedStatement.executeQuery();
        }
    }
}
