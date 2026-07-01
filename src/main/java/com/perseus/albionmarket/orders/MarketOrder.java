package com.perseus.albionmarket.orders;

import java.sql.Timestamp;

public class MarketOrder {

    private final long orderId;
    private final String nodeId;
    private final String playerUuid;
    private final OrderType orderType;
    private final String material;
    private final String nbtHash;
    private final String displayLabel;
    private final String serializedItem;
    private final int initialQuantity;
    private final int remainingQuantity;
    private final long pricePerUnit;
    private final long totalEscrow;
    private final long setupFeePaid;
    private final OrderStatus status;
    private final Timestamp createdAt;
    private final Timestamp expiresAt;
    private final Timestamp updatedAt;

    public MarketOrder(long orderId, String nodeId, String playerUuid, OrderType orderType,
                       String material, String nbtHash, String displayLabel, String serializedItem,
                       int initialQuantity, int remainingQuantity, long pricePerUnit, long totalEscrow,
                       long setupFeePaid, OrderStatus status, Timestamp createdAt, Timestamp expiresAt,
                       Timestamp updatedAt) {
        this.orderId = orderId;
        this.nodeId = nodeId;
        this.playerUuid = playerUuid;
        this.orderType = orderType;
        this.material = material;
        this.nbtHash = nbtHash;
        this.displayLabel = displayLabel;
        this.serializedItem = serializedItem;
        this.initialQuantity = initialQuantity;
        this.remainingQuantity = remainingQuantity;
        this.pricePerUnit = pricePerUnit;
        this.totalEscrow = totalEscrow;
        this.setupFeePaid = setupFeePaid;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
    }

    public long getOrderId() { return orderId; }
    public String getNodeId() { return nodeId; }
    public String getPlayerUuid() { return playerUuid; }
    public OrderType getOrderType() { return orderType; }
    public String getMaterial() { return material; }
    public String getNbtHash() { return nbtHash; }
    public String getDisplayLabel() { return displayLabel; }
    public String getSerializedItem() { return serializedItem; }
    public int getInitialQuantity() { return initialQuantity; }
    public int getRemainingQuantity() { return remainingQuantity; }
    public long getPricePerUnit() { return pricePerUnit; }
    public long getTotalEscrow() { return totalEscrow; }
    public long getSetupFeePaid() { return setupFeePaid; }
    public OrderStatus getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    public boolean isExpired() {
        return expiresAt.before(new Timestamp(System.currentTimeMillis()));
    }

    public boolean isActive() {
        return status == OrderStatus.ACTIVE && !isExpired();
    }
}