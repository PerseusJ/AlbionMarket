package com.perseus.albionmarket.engine;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.MarketNode;
import com.perseus.albionmarket.database.DatabaseManager;
import com.perseus.albionmarket.orders.MarketOrder;
import com.perseus.albionmarket.orders.OrderType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MatchingEngine {

    private final AlbionMarket plugin;
    private final DatabaseManager databaseManager;
    private final Logger log;

    public MatchingEngine(AlbionMarket plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.log = plugin.getLogger();
    }

    public MatchResult matchOrder(MarketOrder incomingOrder) {
        OrderType counterType = incomingOrder.getOrderType() == OrderType.SELL
                ? OrderType.BUY : OrderType.SELL;

        List<TradeExecution> allTrades = new ArrayList<>();
        int remainingQuantity = incomingOrder.getRemainingQuantity();
        long totalMatchedValue = 0;
        long totalTax = 0;
        long totalSellerPayout = 0;
        MarketNode node = plugin.getMarketNodeRegistry().getNode(incomingOrder.getNodeId());

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<MarketOrder> counterOrders = databaseManager.getCounterOrdersForMatching(
                        conn, incomingOrder.getNodeId(), incomingOrder.getNbtHash(), counterType);

                for (MarketOrder counter : counterOrders) {
                    if (remainingQuantity <= 0) break;

                    if (counter.getOrderId() == incomingOrder.getOrderId()) continue;

                    boolean priceMatch;
                    if (incomingOrder.getOrderType() == OrderType.SELL) {
                        priceMatch = counter.getPricePerUnit() >= incomingOrder.getPricePerUnit();
                    } else {
                        priceMatch = counter.getPricePerUnit() <= incomingOrder.getPricePerUnit();
                    }
                    if (!priceMatch) {
                        if (incomingOrder.getOrderType() == OrderType.SELL) break;
                        else continue;
                    }

                    long executionPrice = counter.getPricePerUnit();
                    int fillQuantity = Math.min(remainingQuantity, counter.getRemainingQuantity());

                    boolean success = databaseManager.reduceOrderQuantityOptimistic(
                            conn, counter.getOrderId(), fillQuantity, counter.getRemainingQuantity());
                    if (!success) continue;

                    long tradeValue = executionPrice * fillQuantity;
                    long tradeTax = plugin.getFeeCalculator().calculateTransactionTax(tradeValue, node);
                    long sellerPayout = tradeValue - tradeTax;

                    String tradeType = (incomingOrder.getOrderType() == OrderType.SELL)
                            ? "LIMIT_MATCH" : "LIMIT_MATCH";

                    String buyerUuid, sellerUuid;
                    Long buyerOrderId, sellerOrderId;
                    if (incomingOrder.getOrderType() == OrderType.SELL) {
                        buyerUuid = counter.getPlayerUuid();
                        sellerUuid = incomingOrder.getPlayerUuid();
                        buyerOrderId = counter.getOrderId();
                        sellerOrderId = incomingOrder.getOrderId();
                        long escrowRelease = executionPrice * fillQuantity;
                        if (escrowRelease < counter.getTotalEscrow()) {
                            long leftoverInCounter = counter.getTotalEscrow() - counter.getPricePerUnit() * (long) counter.getInitialQuantity();
                            log.fine("[MatchingEngine] Buy order #" + counter.getOrderId()
                                    + " escrow release: " + escrowRelease + " of " + counter.getTotalEscrow());
                        }
                    } else {
                        buyerUuid = incomingOrder.getPlayerUuid();
                        sellerUuid = counter.getPlayerUuid();
                        buyerOrderId = incomingOrder.getOrderId();
                        sellerOrderId = counter.getOrderId();
                    }

                    databaseManager.insertTradeHistory(conn, incomingOrder.getNodeId(),
                            buyerUuid, sellerUuid, incomingOrder.getMaterial(), incomingOrder.getNbtHash(),
                            fillQuantity, executionPrice, tradeValue, tradeTax, sellerPayout,
                            tradeType, buyerOrderId, sellerOrderId);

                    allTrades.add(new TradeExecution(0, incomingOrder.getNodeId(),
                            buyerUuid, sellerUuid, incomingOrder.getMaterial(), incomingOrder.getNbtHash(),
                            fillQuantity, executionPrice, tradeValue, tradeTax, sellerPayout,
                            tradeType, buyerOrderId, sellerOrderId, null));

                    remainingQuantity -= fillQuantity;
                    totalMatchedValue += tradeValue;
                    totalTax += tradeTax;
                    totalSellerPayout += sellerPayout;
                }

                if (!allTrades.isEmpty()) {
                    databaseManager.reduceOrderQuantityOptimistic(conn,
                            incomingOrder.getOrderId(),
                            incomingOrder.getRemainingQuantity() - remainingQuantity,
                            incomingOrder.getRemainingQuantity());
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                log.severe("[MatchingEngine] Match transaction rolled back: " + e.getMessage());
                return MatchResult.empty();
            }
        } catch (SQLException e) {
            log.severe("[MatchingEngine] Failed to acquire connection for matching: " + e.getMessage());
            return MatchResult.empty();
        }

        boolean fullFilled = remainingQuantity <= 0;
        return new MatchResult(fullFilled, incomingOrder.getRemainingQuantity() - remainingQuantity,
                totalMatchedValue, totalTax, totalSellerPayout, 0, allTrades);
    }
}