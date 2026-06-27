package com.perseus.albionmarket.config;

import com.perseus.albionmarket.AlbionMarket;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MarketNodeRegistry {

    private final AlbionMarket plugin;
    private final Map<String, MarketNode> nodes;
    private File file;
    private YamlConfiguration config;

    public MarketNodeRegistry(AlbionMarket plugin) {
        this.plugin = plugin;
        this.nodes = new HashMap<>();
    }

    public void load() {
        nodes.clear();
        file = new File(plugin.getDataFolder(), "markets.yml");
        if (!file.exists()) {
            plugin.saveResource("markets.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        ConfigManager configManager = plugin.getConfigManager();

        for (String nodeId : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(nodeId);
            if (section == null) continue;

            String displayName = section.getString("display-name", nodeId);
            boolean inheritDefaults = section.getBoolean("inherit-defaults", false);

            double setupFeePercent;
            double transactionTaxPercent;
            int maxOrderDurationDays;
            int maxActiveOrdersPerPlayer;

            if (inheritDefaults) {
                setupFeePercent = configManager.getDefaultSetupFeePercent();
                transactionTaxPercent = configManager.getDefaultTransactionTaxPercent();
                maxOrderDurationDays = configManager.getDefaultMaxOrderDurationDays();
                maxActiveOrdersPerPlayer = configManager.getDefaultMaxActiveOrdersPerPlayer();
            } else {
                setupFeePercent = section.getDouble("setup-fee-percent", configManager.getDefaultSetupFeePercent());
                transactionTaxPercent = section.getDouble("transaction-tax-percent", configManager.getDefaultTransactionTaxPercent());
                maxOrderDurationDays = section.getInt("max-order-duration-days", configManager.getDefaultMaxOrderDurationDays());
                maxActiveOrdersPerPlayer = section.getInt("max-active-orders-per-player", configManager.getDefaultMaxActiveOrdersPerPlayer());
            }

            MarketNode node = new MarketNode(
                nodeId, displayName, setupFeePercent, transactionTaxPercent,
                maxOrderDurationDays, maxActiveOrdersPerPlayer, inheritDefaults
            );
            nodes.put(nodeId, node);
        }
    }

    public MarketNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Collection<MarketNode> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public boolean containsNode(String nodeId) {
        return nodes.containsKey(nodeId);
    }

    public int getNodeCount() {
        return nodes.size();
    }
}
