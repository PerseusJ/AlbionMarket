package com.perseus.albionmarket.database;

import com.perseus.albionmarket.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class MySQLProvider implements DatabaseProvider {

    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private boolean initialized;

    public MySQLProvider(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void initialize() {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl("jdbc:mysql://" + configManager.getMysqlHost() + ":" +
                configManager.getMysqlPort() + "/" + configManager.getMysqlDatabase() +
                "?useSSL=false&allowPublicKeyRetrieval=true");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setUsername(configManager.getMysqlUsername());
        hikariConfig.setPassword(configManager.getMysqlPassword());

        hikariConfig.setMaximumPoolSize(configManager.getMaxPoolSize());
        hikariConfig.setConnectionTimeout(configManager.getConnectionTimeoutMs());
        hikariConfig.setIdleTimeout(configManager.getIdleTimeoutMs());

        hikariConfig.setPoolName("albionmarket-mysql");

        this.dataSource = new HikariDataSource(hikariConfig);
        this.initialized = true;
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        initialized = false;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }
}
