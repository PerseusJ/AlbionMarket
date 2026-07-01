package com.perseus.albionmarket.engine;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.ConfigManager;
import com.perseus.albionmarket.config.MarketNode;
import com.perseus.albionmarket.config.MarketNodeRegistry;
import com.perseus.albionmarket.database.DatabaseManager;
import com.perseus.albionmarket.orders.MarketOrder;
import com.perseus.albionmarket.orders.OrderStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class OrderValidator {

    private final AlbionMarket plugin;
    private final ConfigManager configManager;
    private final MarketNodeRegistry nodeRegistry;
    private final DatabaseManager databaseManager;

    public OrderValidator(AlbionMarket plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.nodeRegistry = plugin.getMarketNodeRegistry();
        this.databaseManager = plugin.getDatabaseManager();
    }

    public String validateCreateSellOrder(Player player, MarketNode node, ItemStack item,
                                          int quantity, long pricePerUnit) {
        if (player == null) return "player-null";
        if (node == null) return "node-null";
        if (item == null || item.getType().isAir()) return "item-invalid";
        if (quantity <= 0) return "quantity-invalid";
        if (pricePerUnit < configManager.getMinPricePerUnit()) return "price-too-low";
        if (pricePerUnit > configManager.getMaxPricePerUnit()) return "price-too-high";

        if (plugin.getItemHasher().computeHash(item) == null) return "damaged-item";

        int remaining = countRemainingInInventory(player, item);
        if (remaining < quantity) return "insufficient-items";

        int maxOrders = resolveMaxOrders(node);
        int activeCount = databaseManager.countActiveOrders(player.getUniqueId().toString(), node.getNodeId());
        if (activeCount >= maxOrders) return "max-orders";

        long upfrontCost = plugin.getFeeCalculator().calculateSellOrderUpfrontCost(pricePerUnit, quantity, node);
        if (!plugin.getEscrowManager().hasSufficientFunds(player, upfrontCost)) return "insufficient-funds";

        return null;
    }

    public String validateCreateBuyOrder(Player player, MarketNode node,
                                         int quantity, long pricePerUnit) {
        if (player == null) return "player-null";
        if (node == null) return "node-null";
        if (quantity <= 0) return "quantity-invalid";
        if (pricePerUnit < configManager.getMinPricePerUnit()) return "price-too-low";
        if (pricePerUnit > configManager.getMaxPricePerUnit()) return "price-too-high";

        int maxOrders = resolveMaxOrders(node);
        int activeCount = databaseManager.countActiveOrders(player.getUniqueId().toString(), node.getNodeId());
        if (activeCount >= maxOrders) return "max-orders";

        long requiredEscrow = plugin.getFeeCalculator().calculateBuyOrderEscrow(pricePerUnit, quantity, node);
        if (!plugin.getEscrowManager().hasSufficientFunds(player, requiredEscrow)) return "insufficient-funds";

        return null;
    }

    public String validateCancelOrder(Player player, MarketOrder order) {
        if (order == null) return "order-null";
        if (!order.getPlayerUuid().equals(player.getUniqueId().toString())) return "not-owner";
        if (!order.isActive()) return "not-active";
        return null;
    }

    private int resolveMaxOrders(MarketNode node) {
        if (node.isInheritDefaults()) {
            return configManager.getDefaultMaxActiveOrdersPerPlayer();
        }
        return node.getMaxActiveOrdersPerPlayer();
    }

    private int countRemainingInInventory(Player player, ItemStack template) {
        int total = 0;
        for (ItemStack slot : player.getInventory().getContents()) {
            if (slot != null && slot.isSimilar(template)) {
                total += slot.getAmount();
            }
        }
        return total;
    }
}