package com.perseus.albionmarket.engine;

import java.sql.Timestamp;

public class TradeExecution {

    private final long tradeId;
    private final String nodeId;
    private final String buyerUuid;
    private final String sellerUuid;
    private final String material;
    private final String nbtHash;
    private final int quantity;
    private final long pricePerUnit;
    private final long totalValue;
    private final long taxAmount;
    private final long sellerPayout;
    private final String tradeType;
    private final Long buyerOrderId;
    private final Long sellerOrderId;
    private final Timestamp executedAt;

    public TradeExecution(long tradeId, String nodeId, String buyerUuid, String sellerUuid,
                          String material, String nbtHash, int quantity, long pricePerUnit,
                          long totalValue, long taxAmount, long sellerPayout, String tradeType,
                          Long buyerOrderId, Long sellerOrderId, Timestamp executedAt) {
        this.tradeId = tradeId;
        this.nodeId = nodeId;
        this.buyerUuid = buyerUuid;
        this.sellerUuid = sellerUuid;
        this.material = material;
        this.nbtHash = nbtHash;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.totalValue = totalValue;
        this.taxAmount = taxAmount;
        this.sellerPayout = sellerPayout;
        this.tradeType = tradeType;
        this.buyerOrderId = buyerOrderId;
        this.sellerOrderId = sellerOrderId;
        this.executedAt = executedAt;
    }

    public long getTradeId() { return tradeId; }
    public String getNodeId() { return nodeId; }
    public String getBuyerUuid() { return buyerUuid; }
    public String getSellerUuid() { return sellerUuid; }
    public String getMaterial() { return material; }
    public String getNbtHash() { return nbtHash; }
    public int getQuantity() { return quantity; }
    public long getPricePerUnit() { return pricePerUnit; }
    public long getTotalValue() { return totalValue; }
    public long getTaxAmount() { return taxAmount; }
    public long getSellerPayout() { return sellerPayout; }
    public String getTradeType() { return tradeType; }
    public Long getBuyerOrderId() { return buyerOrderId; }
    public Long getSellerOrderId() { return sellerOrderId; }
    public Timestamp getExecutedAt() { return executedAt; }
}