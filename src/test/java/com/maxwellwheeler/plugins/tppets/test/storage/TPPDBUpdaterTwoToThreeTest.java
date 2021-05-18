package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.DBUpdater;
import com.maxwellwheeler.plugins.tppets.storage.PetType;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPDBUpdaterTwoToThreeTest {
    private DBUpdater dbUpdater;
    private SQLWrapper sqlWrapper;
    private LogWrapper logWrapper;
    private Statement statement;
    private PreparedStatement twoToThreeAddEffectivePetNameColumn;
    private PreparedStatement twoToThreePopulateEffectivePetNameColumn;
    private PreparedStatement twoToThreeRenamePetWithId;
    private PreparedStatement twoToThreeGetPetsNamedAllList;
    private PreparedStatement twoToThreeRenameDuplicates;
    private PreparedStatement twoToThreeStopCascade;
    private PreparedStatement threeToTwoRemoveEffectivePetNameColumnRenameTable;
    private PreparedStatement threeToTwoRemoveEffectivePetNameColumnInsertData;
    private PreparedStatement threeToTwoRemoveEffectivePetNameColumnDropTable;

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
        when(getDbVersion.getInt("version")).thenReturn(2);


        // *** Runs the update from 2 to 3
        this.statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(this.statement);

        // twoToThreeAddEffectivePetNameColumn
        this.twoToThreeAddEffectivePetNameColumn = mock(PreparedStatement.class);
        when(connection.prepareStatement("ALTER TABLE tpp_unloaded_pets ADD COLUMN effective_pet_name VARCHAR(64)")).thenReturn(this.twoToThreeAddEffectivePetNameColumn);
        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenReturn(0);

        // twoToThreePopulateEffectivePetNameColumn
        this.twoToThreePopulateEffectivePetNameColumn = mock(PreparedStatement.class);
        when(connection.prepareStatement("UPDATE tpp_unloaded_pets SET effective_pet_name = lower(pet_name)")).thenReturn(this.twoToThreePopulateEffectivePetNameColumn);
        when(this.twoToThreePopulateEffectivePetNameColumn.executeUpdate()).thenReturn(0);

        // twoToThreeRenamePetWithId
        this.twoToThreeRenamePetWithId = mock(PreparedStatement.class);
        when(connection.prepareStatement("UPDATE tpp_unloaded_pets SET pet_name = ?, effective_pet_name = ? WHERE pet_id = ?")).thenReturn(this.twoToThreeRenamePetWithId);
        when(this.twoToThreeRenamePetWithId.executeUpdate()).thenReturn(0);

        // twoToThreeRemovePetsNamedAllList
        ResultSet twoToThreePetsNamedAllList = mock(ResultSet.class);
        this.twoToThreeGetPetsNamedAllList = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE effective_pet_name = \"all\" OR effective_pet_name = \"list\"")).thenReturn(this.twoToThreeGetPetsNamedAllList);
        when(this.twoToThreeGetPetsNamedAllList.executeQuery()).thenReturn(twoToThreePetsNamedAllList);
        when(twoToThreePetsNamedAllList.next()).thenReturn(true).thenReturn(false);
        when(twoToThreePetsNamedAllList.getString("pet_id")).thenReturn("MockPetId");
        when(twoToThreePetsNamedAllList.getInt("pet_type")).thenReturn(7);
        when(twoToThreePetsNamedAllList.getInt("pet_x")).thenReturn(1);
        when(twoToThreePetsNamedAllList.getInt("pet_y")).thenReturn(2);
        when(twoToThreePetsNamedAllList.getInt("pet_z")).thenReturn(3);
        when(twoToThreePetsNamedAllList.getString("pet_world")).thenReturn("MockPetWorld");
        when(twoToThreePetsNamedAllList.getString("owner_id")).thenReturn("MockOwnerId");
        when(twoToThreePetsNamedAllList.getString("pet_name")).thenReturn("all");
        doReturn("MockPetName").when(this.sqlWrapper).generateUniquePetName("MockOwnerId", PetType.Pets.HORSE);

        // twoToThreeRenameDuplicates
        ResultSet twoToThreeDuplicates = mock(ResultSet.class);
        this.twoToThreeRenameDuplicates = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT * \n"
                + "FROM tpp_unloaded_pets upi\n"
                + "WHERE EXISTS (\n"
                + "SELECT 1\n"
                + "FROM tpp_unloaded_pets upj\n"
                + "WHERE upj.effective_pet_name = upi.effective_pet_name\n"
                + "AND upj.owner_id = upi.owner_id\n"
                + "LIMIT 1, 1)")).thenReturn(this.twoToThreeRenameDuplicates);
        when(this.twoToThreeRenameDuplicates.executeQuery()).thenReturn(twoToThreeDuplicates);
        when(twoToThreeDuplicates.next()).thenReturn(true).thenReturn(false);
        when(twoToThreeDuplicates.getString("pet_id")).thenReturn("MockPetId2");
        when(twoToThreeDuplicates.getInt("pet_type")).thenReturn(7);
        when(twoToThreeDuplicates.getInt("pet_x")).thenReturn(1);
        when(twoToThreeDuplicates.getInt("pet_y")).thenReturn(2);
        when(twoToThreeDuplicates.getInt("pet_z")).thenReturn(3);
        when(twoToThreeDuplicates.getString("pet_world")).thenReturn("MockPetWorld");
        when(twoToThreeDuplicates.getString("owner_id")).thenReturn("MockOwnerId2");
        when(twoToThreeDuplicates.getString("pet_name")).thenReturn("anything");
        doReturn("MockPetName2").when(this.sqlWrapper).generateUniquePetName("MockOwnerId2", PetType.Pets.HORSE);


        // *** Stops the update from cascading from 2 to 3
        this.twoToThreeStopCascade = mock(PreparedStatement.class);
        when(connection.prepareStatement("UPDATE tpp_db_version SET version = ?")).thenReturn(this.twoToThreeStopCascade);
        when(this.twoToThreeStopCascade.executeUpdate()).thenReturn(-1);

        this.dbUpdater = new DBUpdater(tpPets);


        // *** Reverts from 3 to 2

        // threeToTwoRemoveEffectivePetNameColumn
        this.threeToTwoRemoveEffectivePetNameColumnRenameTable = mock(PreparedStatement.class);
        when(connection.prepareStatement("ALTER TABLE tpp_unloaded_pets RENAME TO tpp_unloaded_pets_temp")).thenReturn(this.threeToTwoRemoveEffectivePetNameColumnRenameTable);
        when(this.threeToTwoRemoveEffectivePetNameColumnRenameTable.executeUpdate()).thenReturn(0);

        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");")).thenReturn(0);

        this.threeToTwoRemoveEffectivePetNameColumnInsertData = mock(PreparedStatement.class);
        when(connection.prepareStatement("INSERT INTO tpp_unloaded_pets SELECT pet_id, pet_type, pet_x, pet_y, pet_z, pet_world, owner_id, pet_name FROM tpp_unloaded_pets_temp")).thenReturn(this.threeToTwoRemoveEffectivePetNameColumnInsertData);
        when(this.threeToTwoRemoveEffectivePetNameColumnInsertData.executeUpdate()).thenReturn(0);

        this.threeToTwoRemoveEffectivePetNameColumnDropTable = mock(PreparedStatement.class);
        when(connection.prepareStatement("DROP TABLE tpp_unloaded_pets_temp")).thenReturn(this.threeToTwoRemoveEffectivePetNameColumnDropTable);
        when(this.threeToTwoRemoveEffectivePetNameColumnDropTable.executeUpdate()).thenReturn(0);
    }

    void verifyRollback() throws SQLException {
        verify(this.threeToTwoRemoveEffectivePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");");
        verify(this.threeToTwoRemoveEffectivePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.threeToTwoRemoveEffectivePetNameColumnDropTable, times(1)).executeUpdate();
    }

    void verifyRenamedPet(String petId, String ownerId, String petName) throws SQLException {
        verify(this.sqlWrapper, times(1)).generateUniquePetName(ownerId, PetType.Pets.HORSE);
        verify(this.twoToThreeRenamePetWithId, times(1)).setString(1, petName);
        verify(this.twoToThreeRenamePetWithId, times(1)).setString(2, petName.toLowerCase());
        verify(this.twoToThreeRenamePetWithId, times(1)).setString(3, petId);
    }

    @Test
    @DisplayName("DBUpdater updates from two to three if nothing goes wrong")
    void dbUpdaterTwoToThree() throws SQLException {
        this.dbUpdater.update(this.sqlWrapper);

        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, times(1)).executeQuery();
        verify(this.twoToThreeRenameDuplicates, times(1)).executeQuery();
        verify(this.twoToThreeRenamePetWithId, times(2)).executeUpdate();
        verify(this.twoToThreeRenamePetWithId, times(6)).setString(anyInt(), anyString());
        verifyRenamedPet("MockPetId", "MockOwnerId", "MockPetName");
        verifyRenamedPet("MockPetId2", "MockOwnerId2", "MockPetName2");
        verify(this.twoToThreeStopCascade, times(1)).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater reverts update from two to three if adding effective pet name column fails")
    void dbUpdaterTwoToThreeRevertsAddingEPetNameColumnFails() throws SQLException {
        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 3
        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, never()).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, never()).executeQuery();
        verify(this.sqlWrapper, never()).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.twoToThreeRenameDuplicates, never()).executeQuery();
        verify(this.twoToThreeRenamePetWithId, never()).executeUpdate();
        verify(this.twoToThreeStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater reverts update from two to three and rethrows exception if adding pet name column fails")
    void dbUpdaterTwoToThreeRevertsAddingEPetNameColumnRethrows() throws SQLException {
        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 3
        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, never()).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, never()).executeQuery();
        verify(this.sqlWrapper, never()).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.twoToThreeRenameDuplicates, never()).executeQuery();
        verify(this.twoToThreeRenamePetWithId, never()).executeUpdate();
        verify(this.twoToThreeStopCascade, never()).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute update statement - Message");
    }

    @Test
    @DisplayName("DBUpdater reverts update from two to three if populating effective pet name column fails")
    void dbUpdaterTwoToThreeRevertsPopulatingEPetNameColumnFails() throws SQLException {
        when(this.twoToThreePopulateEffectivePetNameColumn.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 3
        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, never()).executeQuery();
        verify(this.sqlWrapper, never()).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.twoToThreeRenameDuplicates, never()).executeQuery();
        verify(this.twoToThreeRenamePetWithId, never()).executeUpdate();
        verify(this.twoToThreeStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater reverts update from two to three and rethrows exception if populating effective pet name column fails")
    void dbUpdaterTwoToThreeRevertsPopulatingEPetNameColumnRethrows() throws SQLException {
        when(this.twoToThreePopulateEffectivePetNameColumn.executeUpdate()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 3
        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, never()).executeQuery();
        verify(this.sqlWrapper, never()).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.twoToThreeRenameDuplicates, never()).executeQuery();
        verify(this.twoToThreeRenamePetWithId, never()).executeUpdate();
        verify(this.twoToThreeStopCascade, never()).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute update statement - Message");
    }

    @Test
    @DisplayName("DBUpdater reverts update from two to three if updating pet name fails")
    void dbUpdaterTwoToThreeRevertsUpdatingPetNameFails() throws SQLException {
        when(this.twoToThreeRenamePetWithId.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 3
        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, times(1)).executeQuery();
        verify(this.sqlWrapper, times(1)).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.twoToThreeRenameDuplicates, never()).executeQuery();
        verify(this.twoToThreeRenamePetWithId, times(1)).executeUpdate();
        verify(this.twoToThreeStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater reverts update from two to three and rethrows exception if updating pet name fails")
    void dbUpdaterTwoToThreeRevertsUpdatingPetNameFailsRethrows() throws SQLException {
        when(this.twoToThreeRenamePetWithId.executeUpdate()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 3
        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, times(1)).executeQuery();
        verify(this.sqlWrapper, times(1)).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.twoToThreeRenameDuplicates, never()).executeQuery();
        verify(this.twoToThreeRenamePetWithId, times(1)).executeUpdate();
        verify(this.twoToThreeStopCascade, never()).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute update statement - Message");
    }

    @Test
    @DisplayName("DBUpdater reverts update from two to three and rethrows exception if getting pets named all fails")
    void dbUpdaterTwoToThreeRevertsGettingPetsNamedAllFailsRethrows() throws SQLException {
        when(this.twoToThreeGetPetsNamedAllList.executeQuery()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 3
        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, times(1)).executeQuery();
        verify(this.sqlWrapper, never()).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.twoToThreeRenameDuplicates, never()).executeQuery();
        verify(this.twoToThreeRenamePetWithId, never()).executeUpdate();
        verify(this.twoToThreeStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater reverts update from two to three and rethrows exception if getting duplicate pet names fails")
    void dbUpdaterTwoToThreeRevertsGettingDuplicatePetNamesFailsRethrows() throws SQLException {
        when(this.twoToThreeRenameDuplicates.executeQuery()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 3
        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, times(1)).executeQuery();
        verify(this.sqlWrapper, times(1)).generateUniquePetName(anyString(), any(PetType.Pets.class));
        verify(this.twoToThreeRenameDuplicates, times(1)).executeQuery();
        verify(this.twoToThreeRenamePetWithId, times(1)).executeUpdate();
        verify(this.twoToThreeStopCascade, never()).executeUpdate();

        verifyRollback();
    }

    @Test
    @DisplayName("DBUpdater from two to three rethrows when setting current schema version")
    void dbUpdaterTwoToThreeRevertsSettingSchemaVersionRethrows() throws SQLException {
        when(this.twoToThreeStopCascade.executeUpdate()).thenThrow(new SQLException("Message"));

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 1 to 2
        verify(this.twoToThreeAddEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreePopulateEffectivePetNameColumn, times(1)).executeUpdate();
        verify(this.twoToThreeGetPetsNamedAllList, times(1)).executeQuery();
        verify(this.twoToThreeRenameDuplicates, times(1)).executeQuery();
        verify(this.twoToThreeRenamePetWithId, times(2)).executeUpdate();
        verify(this.twoToThreeRenamePetWithId, times(6)).setString(anyInt(), anyString());
        verifyRenamedPet("MockPetId", "MockOwnerId", "MockPetName");
        verifyRenamedPet("MockPetId2", "MockOwnerId2", "MockPetName2");
        verify(this.twoToThreeStopCascade, times(1)).executeUpdate();

        verifyRollback();

        verify(this.logWrapper, times(1)).logErrors("Can't execute update statement - Message");
    }

    @Test
    @DisplayName("DBUpdater from three to two stops when renaming old table when removing pet name column")
    void dbUpdaterTwoToThreeStopsWhenRenamingOldTableRemovingPetNames() throws SQLException {
        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.threeToTwoRemoveEffectivePetNameColumnRenameTable.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 1
        verify(this.threeToTwoRemoveEffectivePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, never()).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");");
        verify(this.threeToTwoRemoveEffectivePetNameColumnInsertData, never()).executeUpdate();
        verify(this.threeToTwoRemoveEffectivePetNameColumnDropTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from three to two stops when creating new table when removing pet name column")
    void dbUpdaterTwoToThreeStopsWhenCreatingNewTableRemovingPetNames() throws SQLException {
        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");")).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 1
        verify(this.threeToTwoRemoveEffectivePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");");
        verify(this.threeToTwoRemoveEffectivePetNameColumnInsertData, never()).executeUpdate();
        verify(this.threeToTwoRemoveEffectivePetNameColumnDropTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from three to two stops when inserting data when removing pet name column")
    void dbUpdaterThreeToTwoStopsWhenInsertingDataRemovingPetNames() throws SQLException {
        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.threeToTwoRemoveEffectivePetNameColumnInsertData.executeUpdate()).thenReturn(-1);

        this.dbUpdater.update(this.sqlWrapper);

        // 2 to 1
        verify(this.threeToTwoRemoveEffectivePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");");
        verify(this.threeToTwoRemoveEffectivePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.threeToTwoRemoveEffectivePetNameColumnDropTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from three to two stops and rethrows when renaming old table when removing pet name column")
    void dbUpdaterTwoToThreeStopsRethrowsWhenRenamingOldTableRemovingPetNames() throws SQLException {
        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.threeToTwoRemoveEffectivePetNameColumnRenameTable.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 1
        verify(this.threeToTwoRemoveEffectivePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, never()).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");");
        verify(this.threeToTwoRemoveEffectivePetNameColumnInsertData, never()).executeUpdate();
        verify(this.threeToTwoRemoveEffectivePetNameColumnDropTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from three to two stops and rethrows when creating new table when removing pet name column")
    void dbUpdaterTwoToThreeStopsRethrowsWhenCreatingNewTableRemovingPetNames() throws SQLException {
        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.statement.executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");")).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 1
        verify(this.threeToTwoRemoveEffectivePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");");
        verify(this.threeToTwoRemoveEffectivePetNameColumnInsertData, never()).executeUpdate();
        verify(this.threeToTwoRemoveEffectivePetNameColumnDropTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from three to two stops and rethrows when inserting data when removing pet name column")
    void dbUpdaterThreeToTwoStopsRethrowsWhenInsertingDataRemovingPetNames() throws SQLException {
        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenReturn(-1);
        when(this.threeToTwoRemoveEffectivePetNameColumnInsertData.executeUpdate()).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        // 2 to 1
        verify(this.threeToTwoRemoveEffectivePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");");
        verify(this.threeToTwoRemoveEffectivePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.threeToTwoRemoveEffectivePetNameColumnDropTable, never()).executeUpdate();
    }

    @Test
    @DisplayName("DBUpdater from three to two rethrows proper exception when exception thrown in update and revert")
    void dbUpdaterTwoToThreeRethrowsAttemptsRevert() throws SQLException {
        SQLException updateException = new SQLException("Update");
        SQLException revertException = new SQLException("Revert");

        when(this.twoToThreeAddEffectivePetNameColumn.executeUpdate()).thenThrow(updateException);
        when(this.threeToTwoRemoveEffectivePetNameColumnInsertData.executeUpdate()).thenThrow(revertException);

        SQLException exception = assertThrows(SQLException.class, () -> this.dbUpdater.update(this.sqlWrapper));

        assertEquals(revertException, exception);

        // 2 to 1
        verify(this.threeToTwoRemoveEffectivePetNameColumnRenameTable, times(1)).executeUpdate();
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)"
                + ");");
        verify(this.threeToTwoRemoveEffectivePetNameColumnInsertData, times(1)).executeUpdate();
        verify(this.threeToTwoRemoveEffectivePetNameColumnDropTable, never()).executeUpdate();
    }
}
