package com.maxwellwheeler.plugins.tppets.storage;

import java.sql.Connection;
import java.sql.ResultSet;


public interface DBFrame {
    Connection getConnection();
    boolean insertPrepStatement(String prepStatement, Object... args);
    ResultSet selectPrepStatement(Connection dbConn, String prepStatement, Object... args);
    boolean deletePrepStatement(String prepStatement, Object... args);
    boolean updatePrepStatement(String prepStatement, Object... args);
    boolean createStatement(String statement);
}
