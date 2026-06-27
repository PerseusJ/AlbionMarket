package com.perseus.albionmarket;

import com.perseus.albionmarket.async.AsyncExecutor;
import com.perseus.albionmarket.commands.AdminSubcommand;
import com.perseus.albionmarket.commands.MarketCommand;
import com.perseus.albionmarket.commands.MarketTabCompleter;
import com.perseus.albionmarket.config.ConfigManager;
import com.perseus.albionmarket.config.MarketNodeRegistry;
import com.perseus.albionmarket.config.MessageManager;
import com.perseus.albionmarket.database.DatabaseManager;
import com.perseus.albionmarket.listeners.EntityInteractionListener;
import com.perseus.albionmarket.listeners.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;

public class AlbionMarket extends JavaPlugin {

    private static AlbionMarket instance;
    private ConfigManager configManager;
    private MarketNodeRegistry marketNodeRegistry;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private AsyncExecutor asyncExecutor;

    public static AlbionMarket getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        this.asyncExecutor = new AsyncExecutor(this);

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.marketNodeRegistry = new MarketNodeRegistry(this);
        this.marketNodeRegistry.load();

        this.messageManager = new MessageManager(this);
        this.messageManager.load();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();

        getServer().getPluginManager().registerEvents(new EntityInteractionListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

        MarketCommand marketCommand = new MarketCommand();
        marketCommand.registerSubcommand("admin", new AdminSubcommand());
        getCommand("market").setExecutor(marketCommand);
        getCommand("market").setTabCompleter(new MarketTabCompleter());

        getLogger().info("AlbionMarket v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("AlbionMarket disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MarketNodeRegistry getMarketNodeRegistry() {
        return marketNodeRegistry;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }
}
