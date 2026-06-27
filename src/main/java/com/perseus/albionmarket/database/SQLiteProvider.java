package com.perseus.albionmarket.database;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.File;

public class SQLiteProvider implements DatabaseProvider {

    private final ConfigManager configManager;
    private final AlbionMarket plugin;
    private HikariDataSource dataSource;
    private boolean initialized;

    public SQLiteProvider(ConfigManager configManager, AlbionMarket plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        HikariConfig hikariConfig = new HikariConfig();

        File dbFile = new File(plugin.getDataFolder(), configManager.getSqliteFile());
        hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        hikariConfig.setDriverClassName("org.sqlite.JDBC");

        hikariConfig.setMaximumPoolSize(configManager.getMaxPoolSize());
        hikariConfig.setConnectionTimeout(configManager.getConnectionTimeoutMs());
        hikariConfig.setIdleTimeout(configManager.getIdleTimeoutMs());

        hikariConfig.addDataSourceProperty("journal_mode", "WAL");
        hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
        hikariConfig.addDataSourceProperty("foreign_keys", "ON");
        hikariConfig.addDataSourceProperty("busy_timeout", "5000");

        hikariConfig.setPoolName("albionmarket-sqlite");

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
