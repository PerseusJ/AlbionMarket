package com.perseus.albionmarket.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchResult {

    private final boolean fullFilled;
    private final int totalFilledQuantity;
    private final long totalMatchedValue;
    private final long totalTax;
    private final long totalSellerPayout;
    private final long leftoverEscrow;
    private final List<TradeExecution> trades;

    public MatchResult(boolean fullFilled, int totalFilledQuantity, long totalMatchedValue,
                       long totalTax, long totalSellerPayout, long leftoverEscrow,
                       List<TradeExecution> trades) {
        this.fullFilled = fullFilled;
        this.totalFilledQuantity = totalFilledQuantity;
        this.totalMatchedValue = totalMatchedValue;
        this.totalTax = totalTax;
        this.totalSellerPayout = totalSellerPayout;
        this.leftoverEscrow = leftoverEscrow;
        this.trades = trades != null ? new ArrayList<>(trades) : Collections.emptyList();
    }

    public static MatchResult empty() {
        return new MatchResult(false, 0, 0, 0, 0, 0, Collections.emptyList());
    }

    public boolean isFullFilled() { return fullFilled; }
    public int getTotalFilledQuantity() { return totalFilledQuantity; }
    public long getTotalMatchedValue() { return totalMatchedValue; }
    public long getTotalTax() { return totalTax; }
    public long getTotalSellerPayout() { return totalSellerPayout; }
    public long getLeftoverEscrow() { return leftoverEscrow; }
    public List<TradeExecution> getTrades() { return Collections.unmodifiableList(trades); }
    public boolean hasTrades() { return !trades.isEmpty(); }
}