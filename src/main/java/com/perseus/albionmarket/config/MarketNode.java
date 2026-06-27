package com.perseus.albionmarket.config;

public class MarketNode {

    private final String nodeId;
    private final String displayName;
    private final double setupFeePercent;
    private final double transactionTaxPercent;
    private final int maxOrderDurationDays;
    private final int maxActiveOrdersPerPlayer;
    private final boolean inheritDefaults;

    public MarketNode(String nodeId, String displayName, double setupFeePercent,
                      double transactionTaxPercent, int maxOrderDurationDays,
                      int maxActiveOrdersPerPlayer, boolean inheritDefaults) {
        this.nodeId = nodeId;
        this.displayName = displayName;
        this.setupFeePercent = setupFeePercent;
        this.transactionTaxPercent = transactionTaxPercent;
        this.maxOrderDurationDays = maxOrderDurationDays;
        this.maxActiveOrdersPerPlayer = maxActiveOrdersPerPlayer;
        this.inheritDefaults = inheritDefaults;
    }

    public String getNodeId() { return nodeId; }
    public String getDisplayName() { return displayName; }
    public double getSetupFeePercent() { return setupFeePercent; }
    public double getTransactionTaxPercent() { return transactionTaxPercent; }
    public int getMaxOrderDurationDays() { return maxOrderDurationDays; }
    public int getMaxActiveOrdersPerPlayer() { return maxActiveOrdersPerPlayer; }
    public boolean isInheritDefaults() { return inheritDefaults; }
}
