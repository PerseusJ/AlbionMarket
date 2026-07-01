package com.perseus.albionmarket;

import com.perseus.albionmarket.async.AsyncExecutor;
import com.perseus.albionmarket.commands.AdminSubcommand;
import com.perseus.albionmarket.commands.MarketCommand;
import com.perseus.albionmarket.commands.MarketTabCompleter;
import com.perseus.albionmarket.config.ConfigManager;
import com.perseus.albionmarket.config.MarketNodeRegistry;
import com.perseus.albionmarket.config.MessageManager;
import com.perseus.albionmarket.database.DatabaseManager;
import com.perseus.albionmarket.economy.EconomyBridge;
import com.perseus.albionmarket.economy.EscrowManager;
import com.perseus.albionmarket.economy.FeeCalculator;
import com.perseus.albionmarket.engine.MatchingEngine;
import com.perseus.albionmarket.engine.OrderValidator;
import com.perseus.albionmarket.identity.ItemHasher;
import com.perseus.albionmarket.identity.ItemSerializer;
import com.perseus.albionmarket.listeners.EntityInteractionListener;
import com.perseus.albionmarket.listeners.PlayerListener;
import com.perseus.albionmarket.orders.OrderManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AlbionMarket extends JavaPlugin {

    private static AlbionMarket instance;
    private ConfigManager configManager;
    private MarketNodeRegistry marketNodeRegistry;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private AsyncExecutor asyncExecutor;

    // V1.0.1 — Economy Bridge & Item Identity
    private EconomyBridge economyBridge;
    private FeeCalculator feeCalculator;
    private EscrowManager escrowManager;
    private ItemHasher itemHasher;
    private ItemSerializer itemSerializer;

    // V1.0.2 — Core Matching Engine
    private OrderValidator orderValidator;
    private MatchingEngine matchingEngine;
    private OrderManager orderManager;

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

        // V1.0.1 — Economy Bridge (must be initialized after all plugins are loaded)
        this.economyBridge = new EconomyBridge(this);
        this.economyBridge.initialize();

        this.feeCalculator = new FeeCalculator(configManager);
        this.escrowManager = new EscrowManager(this, economyBridge);

        // V1.0.1 — Item Identity
        this.itemHasher = new ItemHasher(this);
        this.itemSerializer = new ItemSerializer(this);

        // V1.0.2 — Core Matching Engine
        this.orderValidator = new OrderValidator(this);
        this.matchingEngine = new MatchingEngine(this);
        this.orderManager = new OrderManager(this, matchingEngine, orderValidator);

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

    // -----------------------------------------------------------------------
    // Getters — V1.0.0
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // Getters — V1.0.1
    // -----------------------------------------------------------------------

    public EconomyBridge getEconomyBridge() {
        return economyBridge;
    }

    public FeeCalculator getFeeCalculator() {
        return feeCalculator;
    }

    public EscrowManager getEscrowManager() {
        return escrowManager;
    }

    public ItemHasher getItemHasher() {
        return itemHasher;
    }

    public ItemSerializer getItemSerializer() {
        return itemSerializer;
    }

    // -----------------------------------------------------------------------
    // Getters — V1.0.2
    // -----------------------------------------------------------------------

    public OrderValidator getOrderValidator() {
        return orderValidator;
    }

    public MatchingEngine getMatchingEngine() {
        return matchingEngine;
    }

    public OrderManager getOrderManager() {
        return orderManager;
    }
}
