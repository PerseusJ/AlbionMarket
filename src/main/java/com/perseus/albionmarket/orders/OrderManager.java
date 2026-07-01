package com.perseus.albionmarket.orders;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.MarketNode;
import com.perseus.albionmarket.database.DatabaseManager;
import com.perseus.albionmarket.engine.MatchResult;
import com.perseus.albionmarket.engine.MatchingEngine;
import com.perseus.albionmarket.engine.OrderValidator;
import com.perseus.albionmarket.engine.TradeExecution;
import com.perseus.albionmarket.identity.ItemIdentity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Logger;

public class OrderManager {

    private final AlbionMarket plugin;
    private final DatabaseManager databaseManager;
    private final MatchingEngine matchingEngine;
    private final OrderValidator validator;
    private final Logger log;

    public OrderManager(AlbionMarket plugin, MatchingEngine matchingEngine, OrderValidator validator) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.matchingEngine = matchingEngine;
        this.validator = validator;
        this.log = plugin.getLogger();
    }

    public MarketOrder createSellOrder(Player player, MarketNode node, ItemStack item,
                                       int quantity, long pricePerUnit) {
        String validationError = validator.validateCreateSellOrder(player, node, item, quantity, pricePerUnit);
        if (validationError != null) {
            log.fine("[OrderManager] Sell order validation failed: " + validationError + " for " + player.getName());
            return null;
        }

        ItemIdentity identity = ItemIdentity.of(item, plugin.getItemHasher());
        if (identity == null) return null;

        MarketNode resolvedNode = node.isInheritDefaults()
                ? new MarketNode(node.getNodeId(), node.getDisplayName(),
                plugin.getConfigManager().getDefaultSetupFeePercent(),
                plugin.getConfigManager().getDefaultTransactionTaxPercent(),
                plugin.getConfigManager().getDefaultMaxOrderDurationDays(),
                plugin.getConfigManager().getDefaultMaxActiveOrdersPerPlayer(), true)
                : node;

        long setupFee = plugin.getFeeCalculator().calculateSetupFee(pricePerUnit, quantity, resolvedNode);
        long upfrontCost = plugin.getFeeCalculator().calculateSellOrderUpfrontCost(pricePerUnit, quantity, resolvedNode);

        if (!plugin.getEscrowManager().lockFunds(player, upfrontCost,
                "SELL_SETUP_FEE node=" + node.getNodeId())) {
            return null;
        }

        ItemStack toSerialize = item.clone();
        toSerialize.setAmount(quantity);
        String serialized = plugin.getItemSerializer().serialize(toSerialize);

        Timestamp expiresAt = new Timestamp(System.currentTimeMillis()
                + resolveOrderDuration(resolvedNode) * 24L * 60L * 60L * 1000L);

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                MarketOrder order = databaseManager.createOrder(conn, node.getNodeId(),
                        player.getUniqueId().toString(), OrderType.SELL,
                        identity.getMaterial().name(), identity.getNbtHash(),
                        identity.getDisplayLabel(), serialized, quantity, pricePerUnit,
                        0, setupFee, expiresAt);
                conn.commit();
                log.info("[OrderManager] Sell order #" + order.getOrderId() + " created by " + player.getName()
                        + " at " + node.getNodeId() + " — " + quantity + "x " + identity.getDisplayLabel()
                        + " @ " + pricePerUnit + "/ea");

                MatchResult result = matchingEngine.matchOrder(order);
                if (result.hasTrades()) {
                    log.info("[OrderManager] Sell order #" + order.getOrderId() + " matched " + result.getTrades().size()
                            + " trade(s), " + result.getTotalFilledQuantity() + " filled");
                }
                return order;
            } catch (Exception e) {
                conn.rollback();
                log.severe("[OrderManager] Failed to create sell order: " + e.getMessage());
                plugin.getEscrowManager().releaseFunds(player, upfrontCost,
                        "SELL_FAILURE_REFUND node=" + node.getNodeId());
                return null;
            }
        } catch (SQLException e) {
            log.severe("[OrderManager] DB error creating sell order: " + e.getMessage());
            plugin.getEscrowManager().releaseFunds(player, upfrontCost,
                    "SELL_FAILURE_REFUND node=" + node.getNodeId());
            return null;
        }
    }

    public MarketOrder createBuyOrder(Player player, MarketNode node,
                                      String material, String nbtHash, String displayLabel,
                                      int quantity, long pricePerUnit) {
        String validationError = validator.validateCreateBuyOrder(player, node, quantity, pricePerUnit);
        if (validationError != null) {
            log.fine("[OrderManager] Buy order validation failed: " + validationError + " for " + player.getName());
            return null;
        }

        MarketNode resolvedNode = node.isInheritDefaults()
                ? new MarketNode(node.getNodeId(), node.getDisplayName(),
                plugin.getConfigManager().getDefaultSetupFeePercent(),
                plugin.getConfigManager().getDefaultTransactionTaxPercent(),
                plugin.getConfigManager().getDefaultMaxOrderDurationDays(),
                plugin.getConfigManager().getDefaultMaxActiveOrdersPerPlayer(), true)
                : node;

        long totalEscrow = plugin.getFeeCalculator().calculateBuyOrderEscrow(pricePerUnit, quantity, resolvedNode);
        long tradeValue = pricePerUnit * (long) quantity;
        long setupFee = plugin.getFeeCalculator().calculateSetupFee(pricePerUnit, quantity, resolvedNode);

        if (!plugin.getEscrowManager().lockFunds(player, totalEscrow,
                "BUY_ESCROW node=" + node.getNodeId())) {
            return null;
        }

        Timestamp expiresAt = new Timestamp(System.currentTimeMillis()
                + resolveOrderDuration(resolvedNode) * 24L * 60L * 60L * 1000L);

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                MarketOrder order = databaseManager.createOrder(conn, node.getNodeId(),
                        player.getUniqueId().toString(), OrderType.BUY,
                        material, nbtHash, displayLabel, null, quantity, pricePerUnit,
                        tradeValue, setupFee, expiresAt);
                conn.commit();
                log.info("[OrderManager] Buy order #" + order.getOrderId() + " created by " + player.getName()
                        + " at " + node.getNodeId() + " — " + quantity + "x " + displayLabel
                        + " @ " + pricePerUnit + "/ea (escrow: " + tradeValue + " + " + setupFee + " fee)");

                MatchResult result = matchingEngine.matchOrder(order);
                if (result.hasTrades()) {
                    log.info("[OrderManager] Buy order #" + order.getOrderId() + " matched " + result.getTrades().size()
                            + " trade(s), " + result.getTotalFilledQuantity() + " filled");
                }
                return order;
            } catch (Exception e) {
                conn.rollback();
                log.severe("[OrderManager] Failed to create buy order: " + e.getMessage());
                plugin.getEscrowManager().releaseFunds(player, totalEscrow,
                        "BUY_FAILURE_REFUND node=" + node.getNodeId());
                return null;
            }
        } catch (SQLException e) {
            log.severe("[OrderManager] DB error creating buy order: " + e.getMessage());
            plugin.getEscrowManager().releaseFunds(player, totalEscrow,
                    "BUY_FAILURE_REFUND node=" + node.getNodeId());
            return null;
        }
    }

    public boolean cancelOrder(Player player, long orderId) {
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                MarketOrder order = databaseManager.getOrder(conn, orderId);
                if (order == null) {
                    conn.rollback();
                    return false;
                }

                String validationError = validator.validateCancelOrder(player, order);
                if (validationError != null) {
                    conn.rollback();
                    log.fine("[OrderManager] Cancel validation failed: " + validationError);
                    return false;
                }

                boolean cancelled = databaseManager.cancelOrderOptimistic(conn, orderId,
                        player.getUniqueId().toString());
                if (!cancelled) {
                    conn.rollback();
                    log.warning("[OrderManager] Cancel optimistic lock failed for order #" + orderId);
                    return false;
                }

                conn.commit();

                if (order.getOrderType() == OrderType.BUY) {
                    long refundAmount = order.getTotalEscrow();
                    if (refundAmount > 0) {
                        plugin.getEscrowManager().releaseFunds(player, refundAmount,
                                "CANCEL_REFUND order_id=" + orderId);
                    }
                }

                log.info("[OrderManager] Order #" + orderId + " cancelled by " + player.getName());
                return true;
            } catch (Exception e) {
                conn.rollback();
                log.severe("[OrderManager] Failed to cancel order #" + orderId + ": " + e.getMessage());
                return false;
            }
        } catch (SQLException e) {
            log.severe("[OrderManager] DB error cancelling order #" + orderId + ": " + e.getMessage());
            return false;
        }
    }

    public List<MarketOrder> getPlayerActiveOrders(Player player, MarketNode node) {
        try (Connection conn = databaseManager.getConnection()) {
            return databaseManager.getActiveOrdersByPlayer(conn,
                    player.getUniqueId().toString(), node.getNodeId());
        } catch (SQLException e) {
            log.warning("[OrderManager] Failed to get active orders: " + e.getMessage());
            return List.of();
        }
    }

    private int resolveOrderDuration(MarketNode node) {
        if (node.isInheritDefaults()) {
            return plugin.getConfigManager().getDefaultMaxOrderDurationDays();
        }
        return node.getMaxOrderDurationDays();
    }
}