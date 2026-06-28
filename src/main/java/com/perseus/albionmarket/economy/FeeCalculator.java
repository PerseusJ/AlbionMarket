package com.perseus.albionmarket.economy;

import com.perseus.albionmarket.config.ConfigManager;
import com.perseus.albionmarket.config.MarketNode;

/**
 * Computes setup fees and transaction taxes from the hierarchical configuration.
 *
 * <p>Resolves effective rates by applying per-node overrides first, falling back
 * to global defaults from {@link ConfigManager} when the node inherits defaults
 * (AD-11). All arithmetic is integer-only with {@link Math#round} to prevent
 * floating-point drift (AD-05).</p>
 */
public class FeeCalculator {

    private final ConfigManager configManager;

    public FeeCalculator(ConfigManager configManager) {
        this.configManager = configManager;
    }

    // -----------------------------------------------------------------------
    // Effective rate resolution
    // -----------------------------------------------------------------------

    /**
     * Returns the effective setup fee percentage for a given market node.
     * Uses the node's own rate unless the node is set to inherit global defaults.
     *
     * @param node the market node
     * @return setup fee as a percent (e.g. 2.5 → 2.5%)
     */
    public double getEffectiveSetupFeePercent(MarketNode node) {
        if (node.isInheritDefaults()) {
            return configManager.getDefaultSetupFeePercent();
        }
        return node.getSetupFeePercent();
    }

    /**
     * Returns the effective transaction tax percentage for a given market node.
     * Uses the node's own rate unless the node is set to inherit global defaults.
     *
     * @param node the market node
     * @return transaction tax as a percent (e.g. 6.0 → 6.0%)
     */
    public double getEffectiveTransactionTaxPercent(MarketNode node) {
        if (node.isInheritDefaults()) {
            return configManager.getDefaultTransactionTaxPercent();
        }
        return node.getTransactionTaxPercent();
    }

    // -----------------------------------------------------------------------
    // Fee computations
    // -----------------------------------------------------------------------

    /**
     * Calculates the non-refundable setup fee for a new order.
     *
     * <p>Formula: {@code round(pricePerUnit * quantity * (feePercent / 100))}</p>
     * <p>Minimum fee is 1 silver when the order has a non-zero value (prevents
     * free listing of cheap items).</p>
     *
     * @param pricePerUnit price per single item in silver
     * @param quantity     number of items in the order
     * @param feePercent   setup fee rate (e.g. 2.5)
     * @return setup fee in silver (>= 1 if order value > 0)
     */
    public long calculateSetupFee(long pricePerUnit, int quantity, double feePercent) {
        long orderValue = pricePerUnit * quantity;
        if (orderValue <= 0) return 0L;
        long fee = Math.round(orderValue * (feePercent / 100.0));
        return Math.max(1L, fee);
    }

    /**
     * Convenience overload — resolves the fee percent from the node automatically.
     *
     * @param pricePerUnit price per single item in silver
     * @param quantity     number of items in the order
     * @param node         the market node the order is placed at
     * @return setup fee in silver
     */
    public long calculateSetupFee(long pricePerUnit, int quantity, MarketNode node) {
        return calculateSetupFee(pricePerUnit, quantity, getEffectiveSetupFeePercent(node));
    }

    /**
     * Calculates the transaction tax deducted from the seller's gross revenue on a trade.
     *
     * <p>Formula: {@code round(grossRevenue * (taxPercent / 100))}</p>
     *
     * @param grossRevenue total silver the buyer paid (pricePerUnit * filledQuantity)
     * @param taxPercent   transaction tax rate (e.g. 6.0)
     * @return tax amount in silver
     */
    public long calculateTransactionTax(long grossRevenue, double taxPercent) {
        if (grossRevenue <= 0) return 0L;
        return Math.round(grossRevenue * (taxPercent / 100.0));
    }

    /**
     * Convenience overload — resolves the tax percent from the node automatically.
     *
     * @param grossRevenue total silver from the trade
     * @param node         the market node the trade occurred at
     * @return tax amount in silver
     */
    public long calculateTransactionTax(long grossRevenue, MarketNode node) {
        return calculateTransactionTax(grossRevenue, getEffectiveTransactionTaxPercent(node));
    }

    /**
     * Calculates the seller's net payout after tax.
     *
     * @param grossRevenue total silver the buyer paid
     * @param node         the market node
     * @return seller's net payout in silver
     */
    public long calculateNetPayout(long grossRevenue, MarketNode node) {
        return grossRevenue - calculateTransactionTax(grossRevenue, node);
    }

    /**
     * Calculates the total escrow required for a buy order.
     *
     * <p>The buyer must lock: {@code (pricePerUnit * quantity) + setupFee}.</p>
     * The setup fee is never returned, but the trade value portion is released
     * to the seller on fill (or back to the buyer on cancel).
     *
     * @param pricePerUnit price per single item in silver
     * @param quantity     order quantity
     * @param node         the market node
     * @return total silver to withdraw from buyer on order creation
     */
    public long calculateBuyOrderEscrow(long pricePerUnit, int quantity, MarketNode node) {
        long tradeValue = pricePerUnit * (long) quantity;
        long setupFee = calculateSetupFee(pricePerUnit, quantity, node);
        return tradeValue + setupFee;
    }

    /**
     * Calculates the setup fee alone for a sell order.
     *
     * <p>Sell orders only charge the setup fee up-front; the items are the escrow.</p>
     *
     * @param pricePerUnit price per single item in silver
     * @param quantity     order quantity
     * @param node         the market node
     * @return setup fee in silver
     */
    public long calculateSellOrderUpfrontCost(long pricePerUnit, int quantity, MarketNode node) {
        return calculateSetupFee(pricePerUnit, quantity, node);
    }
}
