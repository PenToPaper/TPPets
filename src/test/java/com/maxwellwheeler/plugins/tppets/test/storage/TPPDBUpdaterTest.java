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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TPPDBUpdaterTest {
    private DBUpdater dbUpdater;
    private SQLWrapper sqlWrapper;
    private Statement statement;
    private PreparedStatement oneToTwoFillPetName;
    private PreparedStatement twoToThreeGetPetsNamedAllList;
    private ResultSet getProtectedRegionTable;
    private TPPets tpPets;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        this.logWrapper = mock(LogWrapper.class);

        tpPets = MockFactory.getMockPlugin(null, this.logWrapper, false, false);
        this.sqlWrapper = mock(SQLWrapper.class, Mockito.withSettings()
                .useConstructor(tpPets)
                .defaultAnswer(Mockito.CALLS_REAL_METHODS)
        );
        when(tpPets.getDatabase()).thenReturn(this.sqlWrapper);

        Connection connection = mock(Connection.class);

        when(this.sqlWrapper.getConnection()).thenReturn(connection);

        // *** Initializes this.dbUpdater at version = 1
        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(databaseMetaData);

        // doesTppDbVersionExist
        ResultSet getDbVersionTable = mock(ResultSet.class);
        when(databaseMetaData.getTables(null, null, "tpp_db_version", null)).thenReturn(getDbVersionTable);
        when(getDbVersionTable.next()).thenReturn(false);

        // doesHaveInitializedTables
        this.getProtectedRegionTable = mock(ResultSet.class);
        when(databaseMetaData.getTables(null, null, "tpp_protected_regions", null)).thenReturn(this.getProtectedRegionTable);
        when(this.getProtectedRegionTable.next()).thenReturn(true);
        when(this.getProtectedRegionTable.getString("TABLE_NAME")).thenReturn("tpp_protected_regions");

        // Making all create statements succeed
        this.statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(this.statement);
        when(this.statement.executeUpdate(anyString())).thenReturn(0);

        // Making all non-select prepared statements succeed
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);


        // *** Runs the update from 1 to 2

        // oneToTwoFillPetName
        this.oneToTwoFillPetName = mock(PreparedStatement.class);
        ResultSet oneToTwoFillPetNameRS = mock(ResultSet.class);
        when(connection.prepareStatement("SELECT * FROM tpp_unloaded_pets")).thenReturn(this.oneToTwoFillPetName);
        when(this.oneToTwoFillPetName.executeQuery()).thenReturn(oneToTwoFillPetNameRS);
        when(oneToTwoFillPetNameRS.next()).thenReturn(true).thenReturn(false);
        setPetStorageResultSet(oneToTwoFillPetNameRS);


        // *** Runs the update from 2 to 3
        doReturn("MockPetName").when(this.sqlWrapper).generateUniquePetName(anyString(), any(PetType.Pets.class));

        // twoToThreeRemovePetsNamedAllList
        ResultSet twoToThreePetsNamedAllList = mock(ResultSet.class);
        this.twoToThreeGetPetsNamedAllList = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT * FROM tpp_unloaded_pets WHERE effective_pet_name = \"all\" OR effective_pet_name = \"list\"")).thenReturn(this.twoToThreeGetPetsNamedAllList);
        when(this.twoToThreeGetPetsNamedAllList.executeQuery()).thenReturn(twoToThreePetsNamedAllList);
        when(twoToThreePetsNamedAllList.next()).thenReturn(true).thenReturn(false);
        setPetStorageResultSet(twoToThreePetsNamedAllList);

        // twoToThreeRenameDuplicates
        ResultSet twoToThreeDuplicates = mock(ResultSet.class);
        PreparedStatement twoToThreeRenameDuplicates = mock(PreparedStatement.class);
        when(connection.prepareStatement("SELECT * \n"
                + "FROM tpp_unloaded_pets upi\n"
                + "WHERE EXISTS (\n"
                + "SELECT 1\n"
                + "FROM tpp_unloaded_pets upj\n"
                + "WHERE upj.effective_pet_name = upi.effective_pet_name\n"
                + "AND upj.owner_id = upi.owner_id\n"
                + "LIMIT 1, 1)")).thenReturn(twoToThreeRenameDuplicates);
        when(twoToThreeRenameDuplicates.executeQuery()).thenReturn(twoToThreeDuplicates);
        when(twoToThreeDuplicates.next()).thenReturn(true).thenReturn(false);
        setPetStorageResultSet(twoToThreeDuplicates);


        this.dbUpdater = new DBUpdater(tpPets);
    }

    void setPetStorageResultSet(ResultSet resultSet) throws SQLException {
        when(resultSet.getString("pet_id")).thenReturn("MockPetId");
        when(resultSet.getInt("pet_type")).thenReturn(7);
        when(resultSet.getInt("pet_x")).thenReturn(1);
        when(resultSet.getInt("pet_y")).thenReturn(2);
        when(resultSet.getInt("pet_z")).thenReturn(3);
        when(resultSet.getString("pet_world")).thenReturn("MockPetWorld");
        when(resultSet.getString("owner_id")).thenReturn("MockOwnerId");
    }

    @Test
    @DisplayName("DBUpdater cascades updates from one to four if nothing wrong")
    void dbUpdaterOneToFour() throws SQLException {
        this.dbUpdater.update(this.sqlWrapper);

        // One to two is ran
        verify(this.oneToTwoFillPetName, times(1)).executeQuery();

        // Two to three is ran
        verify(this.twoToThreeGetPetsNamedAllList, times(1)).executeQuery();

        // Three to four is ran
        verify(this.statement, times(1)).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_user_storage_locations (\n" +
                "user_id CHAR(32) NOT NULL, \n" +
                "storage_name VARCHAR(64) NOT NULL, \n" +
                "effective_storage_name VARCHAR(64) NOT NULL," +
                "loc_x INT NOT NULL, \n" +
                "loc_y INT NOT NULL, \n" +
                "loc_z INT NOT NULL, \n" +
                "world_name VARCHAR(25) NOT NULL, \n" +
                "PRIMARY KEY (user_id, effective_storage_name))");

        // Update is logged
        verify(this.logWrapper, times(1)).logPluginInfo("Updated database version from version 1 to 4");
    }

    @Test
    @DisplayName("DBUpdater cascades does nothing if db schema is 0, meaning this is a new install")
    void dbUpdaterZero() throws SQLException {
        when(this.getProtectedRegionTable.next()).thenReturn(false);

        this.dbUpdater = new DBUpdater(this.tpPets);

        this.dbUpdater.update(this.sqlWrapper);

        // One to two is not ran
        verify(this.oneToTwoFillPetName, never()).executeQuery();

        // Two to three is not ran
        verify(this.twoToThreeGetPetsNamedAllList, never()).executeQuery();

        // Three to four is not ran
        verify(this.statement, never()).executeUpdate("CREATE TABLE IF NOT EXISTS tpp_user_storage_locations (\n" +
                "user_id CHAR(32) NOT NULL, \n" +
                "storage_name VARCHAR(64) NOT NULL, \n" +
                "effective_storage_name VARCHAR(64) NOT NULL," +
                "loc_x INT NOT NULL, \n" +
                "loc_y INT NOT NULL, \n" +
                "loc_z INT NOT NULL, \n" +
                "world_name VARCHAR(25) NOT NULL, \n" +
                "PRIMARY KEY (user_id, effective_storage_name))");

        // Update is not logged
        verify(this.logWrapper, never()).logPluginInfo(anyString());
    }
}
