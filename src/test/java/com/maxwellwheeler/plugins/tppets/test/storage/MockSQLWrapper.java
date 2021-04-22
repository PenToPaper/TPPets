package com.maxwellwheeler.plugins.tppets.test.storage;

import com.maxwellwheeler.plugins.tppets.TPPets;
import com.maxwellwheeler.plugins.tppets.storage.SQLWrapper;

import java.sql.Connection;

public class MockSQLWrapper extends SQLWrapper {
    public Connection connection;

    protected MockSQLWrapper(TPPets thisPlugin, Connection connection) {
        super(thisPlugin);
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
