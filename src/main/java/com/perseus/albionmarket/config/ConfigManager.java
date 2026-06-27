package com.perseus.albionmarket.config;

import com.perseus.albionmarket.AlbionMarket;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final AlbionMarket plugin;

    private String databaseType;
    private String sqliteFile;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private int maxPoolSize;
    private int connectionTimeoutMs;
    private int idleTimeoutMs;

    private double defaultSetupFeePercent;
    private double defaultTransactionTaxPercent;
    private int defaultMaxOrderDurationDays;
    private int defaultMaxActiveOrdersPerPlayer;
    private String currencyNameSingular;
    private String currencyNamePlural;
    private long minPricePerUnit;
    private long maxPricePerUnit;
    private double minEffectiveTaxPercent;
    private Map<String, Double> taxPermissionMultipliers;

    private List<String> pdcKeysToHash;
    private boolean requireFullDurability;

    private int expiredOrderCheckIntervalMinutes;
    private int batchSize;

    private boolean loginNotificationEnabled;
    private int loginNotificationDelayTicks;
    private String loginNotificationSound;
    private String onlineTradeAlertSound;

    private int placeholdersCacheTtlSeconds;

    public ConfigManager(AlbionMarket plugin) {
        this.plugin = plugin;
        this.taxPermissionMultipliers = new HashMap<>();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        ConfigurationSection db = config.getConfigurationSection("database");
        if (db != null) {
            databaseType = db.getString("type", "sqlite");
            ConfigurationSection sqlite = db.getConfigurationSection("sqlite");
            if (sqlite != null) {
                sqliteFile = sqlite.getString("file", "market.db");
            }
            ConfigurationSection mysql = db.getConfigurationSection("mysql");
            if (mysql != null) {
                mysqlHost = mysql.getString("host", "localhost");
                mysqlPort = mysql.getInt("port", 3306);
                mysqlDatabase = mysql.getString("database", "albionmarket");
                mysqlUsername = mysql.getString("username", "root");
                mysqlPassword = mysql.getString("password", "");
            }
            ConfigurationSection pool = db.getConfigurationSection("pool");
            if (pool != null) {
                maxPoolSize = pool.getInt("max-pool-size", 10);
                connectionTimeoutMs = pool.getInt("connection-timeout-ms", 5000);
                idleTimeoutMs = pool.getInt("idle-timeout-ms", 600000);
            }
        }

        ConfigurationSection economy = config.getConfigurationSection("economy");
        if (economy != null) {
            defaultSetupFeePercent = economy.getDouble("default-setup-fee-percent", 2.5);
            defaultTransactionTaxPercent = economy.getDouble("default-transaction-tax-percent", 6.0);
            defaultMaxOrderDurationDays = economy.getInt("default-max-order-duration-days", 30);
            defaultMaxActiveOrdersPerPlayer = economy.getInt("default-max-active-orders-per-player", 30);
            currencyNameSingular = economy.getString("currency-name-singular", "silver");
            currencyNamePlural = economy.getString("currency-name-plural", "silver");
            minPricePerUnit = economy.getLong("min-price-per-unit", 1);
            maxPricePerUnit = economy.getLong("max-price-per-unit", 999999999);
            minEffectiveTaxPercent = economy.getDouble("min-effective-tax-percent", 0.0);

            ConfigurationSection multipliers = economy.getConfigurationSection("tax-permission-multipliers");
            if (multipliers != null) {
                taxPermissionMultipliers.clear();
                for (String key : multipliers.getKeys(false)) {
                    taxPermissionMultipliers.put(key, multipliers.getDouble(key));
                }
            }
        }

        ConfigurationSection itemIdentity = config.getConfigurationSection("item-identity");
        if (itemIdentity != null) {
            pdcKeysToHash = itemIdentity.getStringList("pdc-keys-to-hash");
            requireFullDurability = itemIdentity.getBoolean("require-full-durability", true);
        }

        ConfigurationSection cleanup = config.getConfigurationSection("cleanup");
        if (cleanup != null) {
            expiredOrderCheckIntervalMinutes = cleanup.getInt("expired-order-check-interval-minutes", 60);
            batchSize = cleanup.getInt("batch-size", 100);
        }

        ConfigurationSection notifications = config.getConfigurationSection("notifications");
        if (notifications != null) {
            loginNotificationEnabled = notifications.getBoolean("login-notification-enabled", true);
            loginNotificationDelayTicks = notifications.getInt("login-notification-delay-ticks", 60);
            loginNotificationSound = notifications.getString("login-notification-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
            onlineTradeAlertSound = notifications.getString("online-trade-alert-sound", "BLOCK_NOTE_BLOCK_PLING");
        }

        ConfigurationSection placeholders = config.getConfigurationSection("placeholders");
        if (placeholders != null) {
            placeholdersCacheTtlSeconds = placeholders.getInt("cache-ttl-seconds", 30);
        }
    }

    public String getDatabaseType() { return databaseType; }
    public String getSqliteFile() { return sqliteFile; }
    public String getMysqlHost() { return mysqlHost; }
    public int getMysqlPort() { return mysqlPort; }
    public String getMysqlDatabase() { return mysqlDatabase; }
    public String getMysqlUsername() { return mysqlUsername; }
    public String getMysqlPassword() { return mysqlPassword; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public int getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public int getIdleTimeoutMs() { return idleTimeoutMs; }

    public double getDefaultSetupFeePercent() { return defaultSetupFeePercent; }
    public double getDefaultTransactionTaxPercent() { return defaultTransactionTaxPercent; }
    public int getDefaultMaxOrderDurationDays() { return defaultMaxOrderDurationDays; }
    public int getDefaultMaxActiveOrdersPerPlayer() { return defaultMaxActiveOrdersPerPlayer; }
    public String getCurrencyNameSingular() { return currencyNameSingular; }
    public String getCurrencyNamePlural() { return currencyNamePlural; }
    public long getMinPricePerUnit() { return minPricePerUnit; }
    public long getMaxPricePerUnit() { return maxPricePerUnit; }
    public double getMinEffectiveTaxPercent() { return minEffectiveTaxPercent; }
    public Map<String, Double> getTaxPermissionMultipliers() { return taxPermissionMultipliers; }

    public List<String> getPdcKeysToHash() { return pdcKeysToHash; }
    public boolean isRequireFullDurability() { return requireFullDurability; }

    public int getExpiredOrderCheckIntervalMinutes() { return expiredOrderCheckIntervalMinutes; }
    public int getBatchSize() { return batchSize; }

    public boolean isLoginNotificationEnabled() { return loginNotificationEnabled; }
    public int getLoginNotificationDelayTicks() { return loginNotificationDelayTicks; }
    public String getLoginNotificationSound() { return loginNotificationSound; }
    public String getOnlineTradeAlertSound() { return onlineTradeAlertSound; }

    public int getPlaceholdersCacheTtlSeconds() { return placeholdersCacheTtlSeconds; }
}
