package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.helpers.LogWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;
import com.maxwellwheeler.plugins.tppets.storage.SQLiteWrapper;
import com.maxwellwheeler.plugins.tppets.test.MockFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class TPPSQLiteWrapperTest {
    private SQLiteWrapper sqLiteWrapper;
    private TPPets tpPets;
    private final String connectionURL = "jdbc:sqlite:DbPath" + File.separator + "DbName.db";
    private LogWrapper logWrapper;

    @BeforeEach
    public void beforeEach() throws SQLException {
        SQLWrapper sqlWrapper = mock(SQLWrapper.class);
        this.logWrapper = mock(LogWrapper.class);
        this.tpPets = MockFactory.getMockPlugin(sqlWrapper, this.logWrapper, false, false);

        this.sqLiteWrapper = mock(SQLiteWrapper.class, withSettings().useConstructor("DbPath", "DbName", this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        doNothing().when(this.sqLiteWrapper).makeDatabaseDirectory();
    }

    @Test
    @DisplayName("getConnection calls driver manager with correct JDBC path")
    void getConnectionCallsDriverManagerWithCorrectPath() throws SQLException {
        try (MockedStatic<DriverManager> driverManager = mockStatic(DriverManager.class)) {
            this.sqLiteWrapper.getConnection();

            verify(this.sqLiteWrapper, times(1)).makeDatabaseDirectory();

            driverManager.verify(times(1), () -> DriverManager.getConnection(this.connectionURL));
        }
    }

    @Test
    @DisplayName("getConnection throws SQLException when dbPath is null")
    void getConnectionThrowsSQLExceptionDbPathNull() throws SQLException {
        this.sqLiteWrapper = mock(SQLiteWrapper.class, withSettings().useConstructor(null, "DbName", this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        SQLException exception = assertThrows(SQLException.class, () -> this.sqLiteWrapper.getConnection());

        assertEquals("Invalid database path", exception.getMessage());

        verify(this.sqLiteWrapper, never()).makeDatabaseDirectory();
        verify(this.logWrapper, times(1)).logErrors("Can't connect to SQLite database - Invalid database path");
    }

    @Test
    @DisplayName("getConnection throws SQLException when dbName is null")
    void getConnectionThrowsSQLExceptionDbNameNull() throws SQLException {
        this.sqLiteWrapper = mock(SQLiteWrapper.class, withSettings().useConstructor("DbPath", null, this.tpPets).defaultAnswer(CALLS_REAL_METHODS));

        SQLException exception = assertThrows(SQLException.class, () -> this.sqLiteWrapper.getConnection());

        assertEquals("Invalid database path", exception.getMessage());

        verify(this.sqLiteWrapper, never()).makeDatabaseDirectory();
        verify(this.logWrapper, times(1)).logErrors("Can't connect to SQLite database - Invalid database path");
    }

    @Test
    @DisplayName("getConnection rethrows exceptions thrown by makeDatabaseDirectory")
    void getConnectionRethrowsMakeDatabaseDirectorySQLException() throws SQLException {
        doThrow(new SQLException("Message")).when(this.sqLiteWrapper).makeDatabaseDirectory();

        SQLException exception = assertThrows(SQLException.class, () -> this.sqLiteWrapper.getConnection());

        assertEquals("Message", exception.getMessage());

        verify(this.sqLiteWrapper, times(1)).makeDatabaseDirectory();
        verify(this.logWrapper, times(1)).logErrors("Can't connect to SQLite database - Message");
    }
}
