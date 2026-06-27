package com.perseus.albionmarket.database;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.ConfigManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final AlbionMarket plugin;
    private DatabaseProvider provider;
    private SchemaManager schemaManager;
    private boolean initialized;

    public DatabaseManager(AlbionMarket plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        ConfigManager configManager = plugin.getConfigManager();
        String dbType = configManager.getDatabaseType();

        if ("mysql".equalsIgnoreCase(dbType)) {
            provider = new MySQLProvider(configManager);
        } else {
            provider = new SQLiteProvider(configManager, plugin);
        }

        try {
            provider.initialize();
            initialized = true;
            plugin.getLogger().info("Database connected (" + dbType + ")");

            this.schemaManager = new SchemaManager(this);
            schemaManager.createTables();
            plugin.getLogger().info("Database schema initialized");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            initialized = false;
        }
    }

    public void shutdown() {
        if (provider != null) {
            provider.shutdown();
        }
        initialized = false;
    }

    public Connection getConnection() throws SQLException {
        if (!initialized || provider == null) {
            throw new SQLException("Database not initialized");
        }
        return provider.getDataSource().getConnection();
    }

    public DatabaseProvider getProvider() {
        return provider;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public AlbionMarket getPlugin() {
        return plugin;
    }
}
