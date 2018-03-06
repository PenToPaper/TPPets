package com.maxwellwheeler.plugins.tppets.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.logging.Level;

import com.maxwellwheeler.plugins.tppets.TPPets;

public abstract class DBGeneral implements DBFrame {
    protected TPPets thisPlugin;
    
    public DBGeneral(TPPets thisPlugin) {
        this.thisPlugin = thisPlugin;
    }
    
    public abstract Connection getConnection();
    
    public boolean insertPrepStatement(String prepStatement, Object... args) {
        try {
            return 1 == executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't execute insert statement: " + e.getMessage());
            return false;
        }
    }

    public ResultSet selectPrepStatement(Connection dbConn, String prepStatement, Object... args) {
        try {
            return executeQuery(dbConn, prepStatement, args);
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't execute select statement: " + e.getMessage());
            return null;
        }
    }

    public boolean deletePrepStatement(String prepStatement, Object... args) {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't execute delete statement: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePrepStatement(String prepStatement, Object... args) {
        try {
            return 0 <= executeUpdate(prepStatement, args);
        } catch (SQLException e) {
            thisPlugin.getLogger().log(Level.SEVERE, "Can't execute update statement: " + e.getMessage());
            return false;
        }
    }

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
