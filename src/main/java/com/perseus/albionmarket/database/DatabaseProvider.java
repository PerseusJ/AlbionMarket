package com.perseus.albionmarket.database;

import javax.sql.DataSource;

public interface DatabaseProvider {

    void initialize() throws Exception;

    void shutdown();

    DataSource getDataSource();

    boolean isInitialized();
}
