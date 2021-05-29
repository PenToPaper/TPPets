package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.DBUpdater;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TPPSQLWrapperInitializeTableTest {
    private TPPets tpPets;
    private DBUpdater dbUpdater;
    private Connection connection;
    private Statement statement;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        LogWrapper logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, logWrapper, false, false);
        this.dbUpdater = mock(DBUpdater.class);
        this.connection = mock(Connection.class);
        this.statement = mock(Statement.class);

        when(this.tpPets.getDatabaseUpdater()).thenReturn(this.dbUpdater);
        when(this.dbUpdater.updateSchemaVersion(any(SQLWrapper.class))).thenReturn(true);
        when(this.connection.createStatement()).thenReturn(this.statement);
        when(this.statement.executeUpdate(anyString())).thenReturn(0);
    }

    @Test
    @DisplayName("Initialize tables functions initializes all 6 functional tables")
    void initializeTables() throws SQLException {
        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);

        assertTrue(mockSQLWrapper.createTables());
        verify(this.connection, times(6)).createStatement();
    }

    @Test
    @DisplayName("Initialize tables functions returns false if cannot create unloaded pets table")
    void initializeTablesCannotCreateUnloadedPets() throws SQLException {
        String makeTableUnloadedPets = "CREATE TABLE IF NOT EXISTS tpp_unloaded_pets (\n"
                + "pet_id CHAR(32) PRIMARY KEY,\n"
                + "pet_type TINYINT NOT NULL,\n"
                + "pet_x INT NOT NULL,\n"
                + "pet_y INT NOT NULL,\n"
                + "pet_z INT NOT NULL,\n"
                + "pet_world VARCHAR(25) NOT NULL,\n"
                + "owner_id CHAR(32) NOT NULL,\n"
                + "pet_name VARCHAR(64)\n,"
                + "effective_pet_name VARCHAR(64)"
                + ");";
        when(this.statement.executeUpdate(makeTableUnloadedPets)).thenReturn(1);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        assertFalse(mockSQLWrapper.createTables());
    }

    @Test
    @DisplayName("Initialize tables functions returns false if cannot create lost regions table")
    void initializeTablesCannotCreateLostRegions() throws SQLException {
        String makeTableLostRegions = "CREATE TABLE IF NOT EXISTS tpp_lost_regions (\n"
                + "zone_name VARCHAR(64) PRIMARY KEY,\n"
                + "min_x INT NOT NULL,\n"
                + "min_y INT NOT NULL,\n"
                + "min_z INT NOT NULL,\n"
                + "max_x INT NOT NULL,\n"
                + "max_y INT NOT NULL,\n"
                + "max_z INT NOT NULL,\n"
                + "world_name VARCHAR(25) NOT NULL);";
        when(this.statement.executeUpdate(makeTableLostRegions)).thenReturn(1);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        assertFalse(mockSQLWrapper.createTables());
    }

    @Test
    @DisplayName("Initialize tables functions returns false if cannot create protected regions table")
    void initializeTablesCannotCreateProtectedRegions() throws SQLException {
        String makeTableProtectedRegions = "CREATE TABLE IF NOT EXISTS tpp_protected_regions (\n"
                + "zone_name VARCHAR(64) PRIMARY KEY,\n"
                + "enter_message VARCHAR(255),\n"
                + "min_x INT NOT NULL,\n"
                + "min_y INT NOT NULL,\n"
                + "min_z INT NOT NULL,\n"
                + "max_x INT NOT NULL,\n"
                + "max_y INT NOT NULL,\n"
                + "max_z INT NOT NULL,\n"
                + "world_name VARCHAR(25) NOT NULL,\n"
                + "lf_zone_name VARCHAR(64));";
        when(this.statement.executeUpdate(makeTableProtectedRegions)).thenReturn(1);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        assertFalse(mockSQLWrapper.createTables());
    }

    @Test
    @DisplayName("Initialize tables functions returns false if cannot create allowed players table")
    void initializeTablesCannotCreateAllowedPlayers() throws SQLException {
        String makeTableAllowedPlayers = "CREATE TABLE IF NOT EXISTS tpp_allowed_players(" +
                "pet_id CHAR(32),\n" +
                "user_id CHAR(32),\n" +
                "PRIMARY KEY(pet_id, user_id),\n" +
                "FOREIGN KEY(pet_id) REFERENCES tpp_unloaded_pets(pet_id) ON DELETE CASCADE);";
        when(this.statement.executeUpdate(makeTableAllowedPlayers)).thenReturn(1);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        assertFalse(mockSQLWrapper.createTables());
    }

    @Test
    @DisplayName("Initialize tables functions returns false if cannot create storage locations table")
    void initializeTablesCannotCreateStorageLocations() throws SQLException {
        String makeTableUserStorageLocations = "CREATE TABLE IF NOT EXISTS tpp_user_storage_locations (\n" +
                "user_id CHAR(32) NOT NULL, \n" +
                "storage_name VARCHAR(64) NOT NULL, \n" +
                "effective_storage_name VARCHAR(64) NOT NULL," +
                "loc_x INT NOT NULL, \n" +
                "loc_y INT NOT NULL, \n" +
                "loc_z INT NOT NULL, \n" +
                "world_name VARCHAR(25) NOT NULL, \n" +
                "PRIMARY KEY (user_id, effective_storage_name))";
        when(this.statement.executeUpdate(makeTableUserStorageLocations)).thenReturn(1);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        assertFalse(mockSQLWrapper.createTables());
    }

    @Test
    @DisplayName("Initialize tables functions returns false if cannot create server storage locations table")
    void initializeTablesCannotCreateServerStorageLocations() throws SQLException {
        String makeTableServerStorageLocations = "CREATE TABLE IF NOT EXISTS tpp_server_storage_locations (\n" +
                "storage_name VARCHAR(64) NOT NULL, \n" +
                "effective_storage_name VARCHAR(64) NOT NULL, \n" +
                "loc_x INT NOT NULL, \n" +
                "loc_y INT NOT NULL, \n" +
                "loc_z INT NOT NULL, \n" +
                "world_name VARCHAR(25) NOT NULL, \n" +
                "PRIMARY KEY (effective_storage_name, world_name))";
        when(this.statement.executeUpdate(makeTableServerStorageLocations)).thenReturn(1);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        assertFalse(mockSQLWrapper.createTables());
    }

    @Test
    @DisplayName("Initialize tables functions returns false if cannot update schema version")
    void initializeTablesCannotUpdateSchemaVersion() throws SQLException {
        when(this.dbUpdater.updateSchemaVersion(any(SQLWrapper.class))).thenReturn(false);

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        assertFalse(mockSQLWrapper.createTables());
    }

    @Test
    @DisplayName("Initialize tables functions throws SQLException if any action gives an SQLException")
    void initializeTablesThrowsSQLException() throws SQLException {
        when(this.dbUpdater.updateSchemaVersion(any(SQLWrapper.class))).thenThrow(new SQLException());

        MockSQLWrapper mockSQLWrapper = new MockSQLWrapper(this.tpPets, this.connection);
        assertThrows(SQLException.class, mockSQLWrapper::createTables);
    }
}
