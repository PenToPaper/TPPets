package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.DBUpdater;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TPPDBUpdaterThreeToFourTest {
    private DBUpdater dbUpdater;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private Statement statement;
    private final String createServerStorage = "CREATE TABLE IF NOT EXISTS tpp_server_storage_locations (\n" +
            "storage_name VARCHAR(64) NOT NULL, \n" +
            "effective_storage_name VARCHAR(64) NOT NULL, \n" +
            "loc_x INT NOT NULL, \n" +
            "loc_y INT NOT NULL, \n" +
            "loc_z INT NOT NULL, \n" +
            "world_name VARCHAR(25) NOT NULL, \n" +
            "PRIMARY KEY (effective_storage_name, world_name))";
    private final String createUserStorage = "CREATE TABLE IF NOT EXISTS tpp_user_storage_locations (\n" +
            "user_id CHAR(32) NOT NULL, \n" +
            "storage_name VARCHAR(64) NOT NULL, \n" +
            "effective_storage_name VARCHAR(64) NOT NULL," +
            "loc_x INT NOT NULL, \n" +
            "loc_y INT NOT NULL, \n" +
            "loc_z INT NOT NULL, \n" +
            "world_name VARCHAR(25) NOT NULL, \n" +
            "PRIMARY KEY (user_id, effective_storage_name))";
    private PreparedStatement threeToFourStopCascade;
    private PreparedStatement fourToThreeDropUserStorageLocations;
    private PreparedStatement fourToThreeDropServerStorageLocations;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.logWrapper = mock(LogWrapper.class);

        TPPets tpPets = MockFactory.getMockPlugin(null, this.logWrapper, false, false);
        this.sqlWrapper = mock(SQLWrapper.class, Mockito.withSettings()
                .useConstructor(tpPets)
                .defaultAnswer(CALLS_REAL_METHODS)
        );
        when(tpPets.getDatabase()).thenReturn(this.sqlWrapper);

        Connection connection = mock(Connection.class);

        when(this.sqlWrapper.getConnection()).thenReturn(connection);

        // *** Initializes this.dbUpdater
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetaData);

        // doesTppDbVersionExist
        ResultSet getDbVersionTable = mock(ResultSet.class);
        when(databaseMetaData.getTables(null, null, "tpp_db_version", null)).thenReturn(getDbVersionTable);
        when(getDbVersionTable.next()).thenReturn(true);
        when(getDbVersionTable.getString("TABLE_NAME")).thenReturn("tpp_db_version");

        // doesHaveInitializedTables
        ResultSet getDbVersion = mock(ResultSet.class);
        PreparedStatement getDbVersionStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT version FROM tpp_db_version")).thenReturn(getDbVersionStatement);
        when(getDbVersionStatement.executeQuery()).thenReturn(getDbVersion);
        when(getDbVersion.getInt("version")).thenReturn(3);


        // *** Runs the update from 3 to 4
        this.statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(this.statement);

        // threeToFourCreateTppUserStorageLocations
        when(this.statement.executeUpdate(this.createUserStorage)).thenReturn(0);

        // threeToFourCreateTppServerStorageLocations
        when(this.statement.executeUpdate(this.createServerStorage)).thenReturn(0);


        // *** Stops the update from cascading from 4 to 5 (future)
        this.threeToFourStopCascade = mock(PreparedStatement.class);
        when(connection.prepareStatement("UPDATE tpp_db_version SET version = ?")).thenReturn(this.threeToFourStopCascade);
        when(this.threeToFourStopCascade.executeUpdate()).thenReturn(-1);

        this.dbUpdater = new DBUpdater(tpPets);


        // *** Reverts from 3 to 2

        // threeToTwoRemoveEffectivePetNameColumn
        this.fourToThreeDropUserStorageLocations = mock(PreparedStatement.class);
        when(connection.prepareStatement("DROP TABLE IF EXISTS tpp_user_storage_locations")).thenReturn(this.fourToThreeDropUserStorageLocations);
        when(this.fourToThreeDropUserStorageLocations.executeUpdate()).thenReturn(0);

        this.fourToThreeDropServerStorageLocations = mock(PreparedStatement.class);
        when(connection.prepareStatement("DROP TABLE IF EXISTS tpp_server_storage_locations")).thenReturn(this.fourToThreeDropServerStorageLocations);
        when(this.fourToThreeDropServerStorageLocations.executeUpdate()).thenReturn(0);
    }

    void verifyRollback() throws SQLException {
        verify(this.fourToThreeDropUserStorageLocations, times(1)).executeUpdate();
        verify(this.fourToThreeDropServerStorageLocations, times(1)).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater updates from three to four if nothing goes wrong")
    void dbUpdaterThreeToFour() throws SQLException {
        this.dbUpdater.update(this.sqlWrapper);

        verify(this.statement, times(2)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate(this.createUserStorage);
        verify(this.statement, times(1)).executeUpdate(this.createServerStorage);
        verify(this.threeToFourStopCascade, times(1)).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater reverts update from three to four if creating user storage table fails")
    void dbUpdaterThreeToFourRevertsCreatingUserStorageTableFails() throws SQLException {
        when(this.statement.executeUpdate(this.createUserStorage)).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 3 to 4
        verify(this.statement, times(1)).executeUpdate(this.createUserStorage);
        verify(this.statement, never()).executeUpdate(this.createServerStorage);
        verify(this.threeToFourStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater reverts and rethrows update from three to four if creating user storage table fails")
    void dbUpdaterThreeToFourRethrowsCreatingUserStorageTableFails() throws SQLException {
        when(this.statement.executeUpdate(this.createUserStorage)).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 3 to 4
        verify(this.statement, times(1)).executeUpdate(this.createUserStorage);
        verify(this.statement, never()).executeUpdate(this.createServerStorage);
        verify(this.threeToFourStopCascade, never()).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute create statement: Message");
    }

    @Test
    @DisplayName("DBUpdater reverts update from three to four if creating server storage table fails")
    void dbUpdaterThreeToFourRevertsCreatingServerStorageTableFails() throws SQLException {
        when(this.statement.executeUpdate(this.createServerStorage)).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 3 to 4
        verify(this.statement, times(2)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate(this.createUserStorage);
        verify(this.statement, times(1)).executeUpdate(this.createServerStorage);
        verify(this.threeToFourStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater reverts and rethrows update from three to four if creating server storage table fails")
    void dbUpdaterThreeToFourRethrowsCreatingServerStorageTableFails() throws SQLException {
        when(this.statement.executeUpdate(this.createServerStorage)).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 3 to 4
        verify(this.statement, times(2)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate(this.createUserStorage);
        verify(this.statement, times(1)).executeUpdate(this.createServerStorage);
        verify(this.threeToFourStopCascade, never()).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute create statement: Message");
    }

    @Test
    @DisplayName("DBUpdater from three to four rethrows when setting current schema version")
    void dbUpdaterThreeToFourRevertsSettingSchemaVersionRethrows() throws SQLException {
        when(this.threeToFourStopCascade.executeUpdate()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 3 to 4
        verify(this.statement, times(2)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate(this.createUserStorage);
        verify(this.statement, times(1)).executeUpdate(this.createServerStorage);
        verify(this.threeToFourStopCascade, times(1)).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute update statement - Message");
    }

    @Test
    @DisplayName("DBUpdater from four to three stops when fails dropping user storage table")
    void dbUpdaterFourToThreeRevertsDroppingUserStorageFails() throws SQLException {
        when(this.statement.executeUpdate(this.createUserStorage)).thenReturn(-1);
        when(this.fourToThreeDropUserStorageLocations.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        verify(this.fourToThreeDropUserStorageLocations, times(1)).executeUpdate();
        verify(this.fourToThreeDropServerStorageLocations, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from four to three stops and rethrows when fails dropping user storage table")
    void dbUpdaterFourToThreeRethrowsDroppingUserStorageFails() throws SQLException {
        when(this.statement.executeUpdate(this.createUserStorage)).thenReturn(-1);
        when(this.fourToThreeDropUserStorageLocations.executeUpdate()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        verify(this.fourToThreeDropUserStorageLocations, times(1)).executeUpdate();
        verify(this.fourToThreeDropServerStorageLocations, never()).executeUpdate();

        verify(this.logWrapper, times(1)).logErrors("Can't execute update statement - Message");
    }

    @Test
    @DisplayName("DBUpdater from four to three rethrows proper exception when exception thrown in update and revert")
    void dbUpdaterFourToThreeRethrowsAttemptsRevert() throws SQLException {
        SQLException updateException = new SQLException("Update");
        SQLException revertException = new SQLException("Revert");

        when(this.statement.executeUpdate(this.createUserStorage)).thenThrow(updateException);
        when(this.fourToThreeDropUserStorageLocations.executeUpdate()).thenThrow(revertException);

        SQLException exception = assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        assertEquals(revertException, exception);

        verify(this.fourToThreeDropUserStorageLocations, times(1)).executeUpdate();
        verify(this.fourToThreeDropServerStorageLocations, never()).executeUpdate();
    }
}
