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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TPPDBUpdaterOneToTwoTest {
    private DBUpdater dbUpdater;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private Statement statement;
    private PreparedStatement oneToTwoAddPetNameColumn;
    private PreparedStatement oneToTwoSetDefaultPetName;
    private PreparedStatement oneToTwoFillPetName;
    private PreparedStatement oneToTwoStopCascade;
    private PreparedStatement twoToOneDropDbVersionTable;
    private PreparedStatement twoToOneDropAllowedPlayersTable;
    private PreparedStatement twoToOneRemovePetNameColumnDropTable;
    private PreparedStatement twoToOneRemovePetNameColumnInsertData;
    private PreparedStatement twoToOneRemovePetNameColumnRenameTable;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.logWrapper = mock(LogWrapper.class);

        TPPets tpPets = MockFactory.getMockPlugin(null, this.logWrapper, false, false, false);
        this.sqlWrapper = mock(SQLWrapper.class, Mockito.withSettings()
                .useConstructor(tpPets)
                .defaultAnswer(Mockito.CALLS_REAL_METHODS)
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
        when(getDbVersionTable.next()).thenReturn(false);

        // doesHaveInitializedTables
        ResultSet getProtectedRegionTable = mock(ResultSet.class);
        when(databaseMetaData.getTables(null, null, "tpp_protected_regions", null)).thenReturn(getProtectedRegionTable);
        when(getProtectedRegionTable.next()).thenReturn(true);
        when(getProtectedRegionTable.getString("TABLE_NAME")).thenReturn("tpp_protected_regions");


        // *** Runs the update from 1 to 2
        this.statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(this.statement);

        // oneToTwoAddPetNameColumn
        this.oneToTwoAddPetNameColumn = mock(PreparedStatement.class);
        when(connection.prepareStatement("ALTER TABLE tpp_unloaded_pets ADD pet_name VARCHAR(64)")).thenReturn(this.oneToTwoAddPetNameColumn);
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(0);

        // oneToTwoCreateAllowedPlayersTable
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);")).thenReturn(0);

        // oneToTwoCreateDbVersionTable
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);")).thenReturn(0);

        // oneToTwoSetDefaultPetName
        this.oneToTwoSetDefaultPetName = mock(PreparedStatement.class);
        when(connection.prepareStatement("UPDATE tpp_unloaded_pets SET pet_name = ? WHERE pet_id = ?")).thenReturn(this.oneToTwoSetDefaultPetName);
        when(this.oneToTwoSetDefaultPetName.executeUpdate()).thenReturn(0);

        // oneToTwoFillPetName
        this.oneToTwoFillPetName = mock(PreparedStatement.class);
        ResultSet oneToTwoFillPetNameRS = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT * FROM tpp_unloaded_pets")).thenReturn(this.oneToTwoFillPetName);
        when(this.oneToTwoFillPetName.executeQuery()).thenReturn(oneToTwoFillPetNameRS);
        when(oneToTwoFillPetNameRS.next()).thenReturn(true).thenReturn(false);
        when(oneToTwoFillPetNameRS.getString("pet_id")).thenReturn("MockPetId");
        when(oneToTwoFillPetNameRS.getInt("pet_type")).thenReturn(7);
        when(oneToTwoFillPetNameRS.getInt("pet_x")).thenReturn(1);
        when(oneToTwoFillPetNameRS.getInt("pet_y")).thenReturn(2);
        when(oneToTwoFillPetNameRS.getInt("pet_z")).thenReturn(3);
        when(oneToTwoFillPetNameRS.getString("pet_world")).thenReturn("MockPetWorld");
        when(oneToTwoFillPetNameRS.getString("owner_id")).thenReturn("MockOwnerId");


        // *** Stops the update from cascading from 2 to 3
        this.oneToTwoStopCascade = mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO tpp_db_version (version) VALUES(?)")).thenReturn(this.oneToTwoStopCascade);
        when(this.oneToTwoStopCascade.executeUpdate()).thenReturn(0);

        this.dbUpdater = new DBUpdater(tpPets);


        // *** Reverts from 2 to 1

        // twoToOneRemovePetNameColumn
        this.twoToOneRemovePetNameColumnRenameTable = mock(PreparedStatement.class);
        when(connection.prepareStatement("ALTER TABLE tpp_unloaded_pets RENAME TO tpp_unloaded_pets_temp")).thenReturn(this.twoToOneRemovePetNameColumnRenameTable);
        when(this.twoToOneRemovePetNameColumnRenameTable.executeUpdate()).thenReturn(0);

        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");")).thenReturn(0);

        this.twoToOneRemovePetNameColumnInsertData = mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO tpp_unloaded_pets SELECT pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id FROM tpp_unloaded_pets_temp")).thenReturn(this.twoToOneRemovePetNameColumnInsertData);
        when(this.twoToOneRemovePetNameColumnInsertData.executeUpdate()).thenReturn(1);

        this.twoToOneRemovePetNameColumnDropTable = mock(PreparedStatement.class);
        when(connection.prepareStatement("DROP TABLE tpp_unloaded_pets_temp")).thenReturn(this.twoToOneRemovePetNameColumnDropTable);
        when(this.twoToOneRemovePetNameColumnDropTable.executeUpdate()).thenReturn(0);

        // twoToOneDropAllowedPlayersTable
        this.twoToOneDropAllowedPlayersTable = mock(PreparedStatement.class);
        when(connection.prepareStatement("DROP TABLE IF EXISTS tpp_allowed_players")).thenReturn(this.twoToOneDropAllowedPlayersTable);
        when(this.twoToOneDropAllowedPlayersTable.executeUpdate()).thenReturn(0);

        // twoToOneDropDbVersionTable
        this.twoToOneDropDbVersionTable = mock(PreparedStatement.class);
        when(connection.prepareStatement("DROP TABLE IF EXISTS tpp_db_version")).thenReturn(this.twoToOneDropDbVersionTable);
        when(this.twoToOneDropDbVersionTable.executeUpdate()).thenReturn(0);
    }

    void verifyRollback() throws SQLException {
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, times(1)).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, times(1)).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, times(1)).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater updates from one to two if nothing goes wrong")
    void dbUpdaterOneToTwo() throws SQLException {
        this.dbUpdater.update(this.sqlWrapper);

        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(3)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
        verify(this.statement, times(2)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);");
        verify(this.oneToTwoSetDefaultPetName, times(2)).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(1, "HORSE0");
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(2, "MockPetId");
        verify(this.oneToTwoSetDefaultPetName, times(1)).executeUpdate();
        verify(this.oneToTwoFillPetName, times(1)).executeQuery();
        verify(this.oneToTwoStopCascade, times(1)).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater reverts update from one to two if adding pet name column fails")
    void dbUpdaterOneToTwoRevertsAddingPetNameColumnFails() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate(anyString());
        verify(this.oneToTwoSetDefaultPetName, never()).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, never()).executeUpdate();
        verify(this.oneToTwoFillPetName, never()).executeQuery();
        verify(this.oneToTwoStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater from one to two reverts update and rethrows exception if adding pet name column fails")
    void dbUpdaterOneToTwoRevertsAddingPetNameColumnRethrows() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate(anyString());
        verify(this.oneToTwoSetDefaultPetName, never()).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, never()).executeUpdate();
        verify(this.oneToTwoFillPetName, never()).executeQuery();
        verify(this.oneToTwoStopCascade, never()).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute update statement: Message");
    }

    @Test
    @DisplayName("DBUpdater reverts update from one to two if creating allowed players table fails")
    void dbUpdaterOneToTwoRevertsCreatingAllowedPlayersFails() throws SQLException {
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);")).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(2)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
        verify(this.oneToTwoSetDefaultPetName, never()).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, never()).executeUpdate();
        verify(this.oneToTwoFillPetName, never()).executeQuery();
        verify(this.oneToTwoStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater from one to two reverts update and rethrows exception if creating allowed players table fails")
    void dbUpdaterOneToTwoRevertsCreatingAllowedPlayersRethrows() throws SQLException {
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);")).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(2)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
        verify(this.oneToTwoSetDefaultPetName, never()).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, never()).executeUpdate();
        verify(this.oneToTwoFillPetName, never()).executeQuery();
        verify(this.oneToTwoStopCascade, never()).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute create statement: Message");
    }

    @Test
    @DisplayName("DBUpdater reverts update from one to two if filling pet names fails")
    void dbUpdaterOneToTwoRevertsFillingPetNamesFails() throws SQLException {
        when(this.oneToTwoSetDefaultPetName.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(2)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
        verify(this.oneToTwoSetDefaultPetName, times(2)).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(1, "HORSE0");
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(2, "MockPetId");
        verify(this.oneToTwoSetDefaultPetName, times(1)).executeUpdate();
        verify(this.oneToTwoFillPetName, times(1)).executeQuery();
        verify(this.oneToTwoStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater from one to two reverts update and rethrows exception if filling pet names fails")
    void dbUpdaterOneToTwoRevertsFillingPetNamesRethrows() throws SQLException {
        when(this.oneToTwoSetDefaultPetName.executeUpdate()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(2)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
        verify(this.oneToTwoSetDefaultPetName, times(2)).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(1, "HORSE0");
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(2, "MockPetId");
        verify(this.oneToTwoSetDefaultPetName, times(1)).executeUpdate();
        verify(this.oneToTwoFillPetName, times(1)).executeQuery();
        verify(this.oneToTwoStopCascade, never()).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute update statement: Message");
    }

    @Test
    @DisplayName("DBUpdater from one to two reverts update and rethrows exception if getting pet names fails")
    void dbUpdaterOneToTwoRevertsGettingPetNamesRethrows() throws SQLException {
        when(this.oneToTwoFillPetName.executeQuery()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(2)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
        verify(this.oneToTwoSetDefaultPetName, never()).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, never()).executeUpdate();
        verify(this.oneToTwoFillPetName, times(1)).executeQuery();
        verify(this.oneToTwoStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater reverts update from one to two if creating db version table fails")
    void dbUpdaterOneToTwoRevertsCreatingDbVersionFails() throws SQLException {
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);")).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(3)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);");
        verify(this.oneToTwoSetDefaultPetName, times(2)).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(1, "HORSE0");
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(2, "MockPetId");
        verify(this.oneToTwoSetDefaultPetName, times(1)).executeUpdate();
        verify(this.oneToTwoFillPetName, times(1)).executeQuery();
        verify(this.oneToTwoStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater from one to two reverts update and rethrows exception if creating db version table fails")
    void dbUpdaterOneToTwoRevertsCreatingDbVersionRethrows() throws SQLException {
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);")).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(3)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);");
        verify(this.oneToTwoSetDefaultPetName, times(2)).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(1, "HORSE0");
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(2, "MockPetId");
        verify(this.oneToTwoSetDefaultPetName, times(1)).executeUpdate();
        verify(this.oneToTwoFillPetName, times(1)).executeQuery();
        verify(this.oneToTwoStopCascade, never()).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute create statement: Message");
    }

    @Test
    @DisplayName("DBUpdater from one to two rethrows when setting current schema version")
    void dbUpdaterOneToTwoRevertsSettingSchemaVersionRethrows() throws SQLException {
        when(this.oneToTwoStopCascade.executeUpdate()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 1 to 2
        verify(this.oneToTwoAddPetNameColumn, times(1)).executeUpdate();
        verify(this.statement, times(4)).executeUpdate(anyString());
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_allowed_players(pet_id CHAR(32), user_id CHAR(32), PRIMARY KEY(pet_id, user_id), FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);");
        verify(this.statement, times(2)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_db_version (version INT PRIMARY KEY);");
        verify(this.oneToTwoSetDefaultPetName, times(2)).setString(anyInt(), anyString());
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(1, "HORSE0");
        verify(this.oneToTwoSetDefaultPetName, times(1)).setString(2, "MockPetId");
        verify(this.oneToTwoSetDefaultPetName, times(1)).executeUpdate();
        verify(this.oneToTwoFillPetName, times(1)).executeQuery();
        verify(this.oneToTwoStopCascade, times(1)).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute insert statement: Message");
    }

    @Test
    @DisplayName("DBUpdater from one to two stops when renaming old table when removing pet name column")
    void dbUpdaterOneToTwoStopsWhenRenamingOldTableRemovingPetNames() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.twoToOneRemovePetNameColumnRenameTable.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, never()).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, never()).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, never()).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, never()).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two stops when creating new table when removing pet name column")
    void dbUpdaterOneToTwoStopsWhenCreatingNewTableRemovingPetNames() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");")).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, never()).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, never()).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, never()).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two stops when inserting data when removing pet name column")
    void dbUpdaterOneToTwoStopsWhenInsertingDataRemovingPetNames() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.twoToOneRemovePetNameColumnInsertData.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, never()).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, never()).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two stops when dropping temp table when removing pet name column")
    void dbUpdaterOneToTwoStopsWhenDroppingTempTableRemovingPetNames() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.twoToOneRemovePetNameColumnDropTable.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, times(1)).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, never()).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two stops when dropping allowed players table")
    void dbUpdaterTwoToOneStopsWhenDroppingAllowedPlayersTable() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.twoToOneDropAllowedPlayersTable.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, times(1)).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, times(1)).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two rethrows and stops when renaming old table when removing pet name column")
    void dbUpdaterTwoToOneRethrowsWhenRenamingOldTableRemovingPetNames() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.twoToOneRemovePetNameColumnRenameTable.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, never()).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, never()).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, never()).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, never()).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two rethrows and stops when creating new table when removing pet name column")
    void dbUpdaterTwoToOneRethrowsWhenCreatingNewTableRemovingPetNames() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");")).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, never()).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, never()).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, never()).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two rethrows and stops when inserting data when removing pet name column")
    void dbUpdaterTwoToOneRethrowsWhenInsertingDataRemovingPetNames() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.twoToOneRemovePetNameColumnInsertData.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, never()).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, never()).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two rethrows and stops when dropping temp table when removing pet name column")
    void dbUpdaterTwoToOneRethrowsWhenDroppingTempTableRemovingPetNames() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.twoToOneRemovePetNameColumnDropTable.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, times(1)).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, never()).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two rethrows and stops when dropping allowed players table")
    void dbUpdaterTwoToOneRethrowsWhenDroppingAllowedPlayersTable() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.twoToOneDropAllowedPlayersTable.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, times(1)).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, times(1)).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from one to two rethrows and stops when dropping allowed players table")
    void dbUpdaterTwoToOneRethrowsWhenDroppingDbVersionTable() throws SQLException {
        when(this.oneToTwoAddPetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.twoToOneDropDbVersionTable.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 1
        verify(this.twoToOneRemovePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL"
                + ");");
        verify(this.twoToOneRemovePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.twoToOneRemovePetNameColumnDropTable, times(1)).executeUpdate();
        verify(this.twoToOneDropAllowedPlayersTable, times(1)).executeUpdate();
        verify(this.twoToOneDropDbVersionTable, times(1)).executeUpdate();
    }
}
