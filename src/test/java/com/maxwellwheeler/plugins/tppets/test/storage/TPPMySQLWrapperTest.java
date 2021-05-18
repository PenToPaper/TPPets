package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.MySQLWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TPPMySQLWrapperTest {
    private MySQLWrapper mySQLWrapper;
    private final String connectionURL = "jdbc:mysql://111.111.111.111:10/TPPets?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&useSSL=false";
    private TPPets tpPets;
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, this.logWrapper, false, false);

        this.mySQLWrapper = new MySQLWrapper("111.111.111.111", 10, "TPPets", "Username", "Password", this.tpPets);
    }

    @Test
    @DisplayName("getConnection calls driver manager with correct URL")
    void getConnectionCallsCorrectURL() throws SQLException {
        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            this.mySQLWrapper.getConnection();

            driverManager.verify(times(1), () -> DriverManager.getConnection(this.connectionURL, "Username", "Password"));
        }
    }

    @Test
    @DisplayName("getConnection throws SQLException when host = null")
    void getConnectionThrowsWhenHostNull() {
        this.mySQLWrapper = new MySQLWrapper(null, 10, "TPPets", "Username", "Password", this.tpPets);

        assertThrows(SQLException.class, () -> this.mySQLWrapper.getConnection());

        verify(this.logWrapper, times(1)).logErrors("Can't connect to MySQL database - Invalid database credentials");
    }

    @Test
    @DisplayName("getConnection throws SQLException when port less than 0")
    void getConnectionThrowsWhenPortNegative() {
        this.mySQLWrapper = new MySQLWrapper("111.111.111.111", -1, "TPPets", "Username", "Password", this.tpPets);

        assertThrows(SQLException.class, () -> this.mySQLWrapper.getConnection());

        verify(this.logWrapper, times(1)).logErrors("Can't connect to MySQL database - Invalid database credentials");
    }

    @Test
    @DisplayName("getConnection throws SQLException when port greater than 65535")
    void getConnectionThrowsWhenPortHigh() {
        this.mySQLWrapper = new MySQLWrapper("111.111.111.111", 65536, "TPPets", "Username", "Password", this.tpPets);

        assertThrows(SQLException.class, () -> this.mySQLWrapper.getConnection());

        verify(this.logWrapper, times(1)).logErrors("Can't connect to MySQL database - Invalid database credentials");
    }

    @Test
    @DisplayName("getConnection throws SQLException when dbName = null")
    void getConnectionThrowsWhenDBNameNull() {
        this.mySQLWrapper = new MySQLWrapper("111.111.111.111", 10, null, "Username", "Password", this.tpPets);

        assertThrows(SQLException.class, () -> this.mySQLWrapper.getConnection());

        verify(this.logWrapper, times(1)).logErrors("Can't connect to MySQL database - Invalid database credentials");
    }

    @Test
    @DisplayName("getConnection throws SQLException when dbUsername = null")
    void getConnectionThrowsWhenUsernameNull() {
        this.mySQLWrapper = new MySQLWrapper("111.111.111.111", 10, "TPPets", null, "Password", this.tpPets);

        assertThrows(SQLException.class, () -> this.mySQLWrapper.getConnection());

        verify(this.logWrapper, times(1)).logErrors("Can't connect to MySQL database - Invalid database credentials");
    }

    @Test
    @DisplayName("getConnection throws SQLException when dbPassword = null")
    void getConnectionThrowsWhenPasswordNull() {
        this.mySQLWrapper = new MySQLWrapper("111.111.111.111", 10, "TPPets", "Username", null, this.tpPets);

        assertThrows(SQLException.class, () -> this.mySQLWrapper.getConnection());

        verify(this.logWrapper, times(1)).logErrors("Can't connect to MySQL database - Invalid database credentials");
    }

    @Test
    @DisplayName("getConnection rethrows SQLException from DriverManager")
    void getConnectionRethrowsExceptionFromDriverManager() throws SQLException {
        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class, RETURNS_MOCKS)) {
            SQLException exception = new SQLException("Message");
            driverManager.when(() -> DriverManager.getConnection(this.connectionURL, "Username", "Password")).thenThrow(exception);

            assertThrows(SQLException.class, () -> this.mySQLWrapper.getConnection());

            driverManager.verify(times(1), () -> DriverManager.getConnection(this.connectionURL, "Username", "Password"));

            verify(this.logWrapper, times(1)).logErrors("Can't connect to MySQL database - Message");
        }
    }
}
