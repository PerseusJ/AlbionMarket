package com.perseus.albionmarket.economy;

import com.perseus.albionmarket.AlbionMarket;
import org.bukkit.OfflinePlayer;

import java.util.logging.Logger;

/**
 * Manages fund escrow for market orders.
 *
 * <p>This is a stateless service — all fund state is held in the Vault economy.
 * It delegates to {@link EconomyBridge} for all Vault API calls and provides
 * named, logged operations for auditing.</p>
 *
 * <p>For buy orders: funds are locked on placement and released on cancel/fill.
 * For sell orders: only the setup fee is withdrawn on placement (items are the escrow).</p>
 */
public class EscrowManager {

    private final AlbionMarket plugin;
    private final EconomyBridge economyBridge;
    private final Logger log;

    public EscrowManager(AlbionMarket plugin, EconomyBridge economyBridge) {
        this.plugin = plugin;
        this.economyBridge = economyBridge;
        this.log = plugin.getLogger();
    }

    /**
     * Locks funds from a player's account into escrow (used for buy orders + setup fees).
     *
     * @param player the player whose funds to lock
     * @param amount the amount in silver to lock (must be positive)
     * @param reason a short description for log tracing (e.g., "BUY_ORDER_ESCROW node=desert_bazaar")
     * @return true if the funds were successfully withdrawn, false if insufficient or unavailable
     */
    public boolean lockFunds(OfflinePlayer player, long amount, String reason) {
        if (!economyBridge.isAvailable()) {
            log.warning("[EscrowManager] Cannot lock funds — economy unavailable. Reason: " + reason);
            return false;
        }
        if (amount <= 0) {
            log.warning("[EscrowManager] Lock requested for non-positive amount (" + amount +
                    ") from " + player.getName() + ". Reason: " + reason);
            return false;
        }
        if (!economyBridge.has(player, amount)) {
            return false;
        }
        boolean success = economyBridge.withdraw(player, amount);
        if (success) {
            log.fine("[EscrowManager] Locked " + amount + " silver from " +
                    player.getName() + " | " + reason);
        } else {
            log.warning("[EscrowManager] Failed to lock " + amount + " silver from " +
                    player.getName() + " | " + reason);
        }
        return success;
    }

    /**
     * Releases escrowed funds back to a player (used on order cancellation or expiry refund).
     *
     * @param player the player to refund
     * @param amount the amount in silver to release (must be positive)
     * @param reason a short description for log tracing (e.g., "CANCEL_REFUND order_id=42")
     */
    public void releaseFunds(OfflinePlayer player, long amount, String reason) {
        if (!economyBridge.isAvailable()) {
            log.severe("[EscrowManager] Cannot release funds — economy unavailable! " +
                    "Player " + player.getName() + " is owed " + amount + " silver. Reason: " + reason);
            return;
        }
        if (amount <= 0) return;
        boolean success = economyBridge.deposit(player, amount);
        if (success) {
            log.fine("[EscrowManager] Released " + amount + " silver to " +
                    player.getName() + " | " + reason);
        } else {
            log.severe("[EscrowManager] Failed to release " + amount + " silver to " +
                    player.getName() + " | " + reason);
        }
    }

    /**
     * Transfers funds from one player to another in a single logical operation.
     *
     * <p>Withdraws from {@code from} first, then deposits to {@code to}.
     * If the deposit fails after a successful withdrawal, logs a SEVERE error
     * and attempts a refund to prevent permanent silver loss.</p>
     *
     * @param from   the player sending funds
     * @param to     the player receiving funds
     * @param amount the amount in silver to transfer (must be positive)
     * @param reason a short description for log tracing
     * @return true if both operations succeeded
     */
    public boolean transferFunds(OfflinePlayer from, OfflinePlayer to, long amount, String reason) {
        if (!economyBridge.isAvailable()) {
            log.severe("[EscrowManager] Cannot transfer funds — economy unavailable! Reason: " + reason);
            return false;
        }
        if (amount <= 0) return false;

        boolean withdrawn = economyBridge.withdraw(from, amount);
        if (!withdrawn) {
            log.warning("[EscrowManager] Transfer failed at withdraw stage. " +
                    "From=" + from.getName() + " Amount=" + amount + " | " + reason);
            return false;
        }

        boolean deposited = economyBridge.deposit(to, amount);
        if (!deposited) {
            log.severe("[EscrowManager] Transfer deposit FAILED after successful withdraw! " +
                    "Attempting refund to " + from.getName() + ". Amount=" + amount + " | " + reason);
            // Attempt refund to prevent silver loss — this is a best-effort recovery
            economyBridge.deposit(from, amount);
            return false;
        }

        log.fine("[EscrowManager] Transferred " + amount + " silver from " +
                from.getName() + " to " + to.getName() + " | " + reason);
        return true;
    }

    /**
     * Checks whether a player has sufficient funds for an operation without withdrawing.
     *
     * @param player the player to check
     * @param amount the required amount in silver
     * @return true if the player has at least {@code amount} silver
     */
    public boolean hasSufficientFunds(OfflinePlayer player, long amount) {
        return economyBridge.isAvailable() && economyBridge.has(player, amount);
    }

    /**
     * Returns the underlying {@link EconomyBridge} for direct balance queries.
     */
    public EconomyBridge getEconomyBridge() {
        return economyBridge;
    }
}
