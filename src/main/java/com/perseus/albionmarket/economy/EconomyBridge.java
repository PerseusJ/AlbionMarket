package com.perseus.albionmarket.economy;

import com.perseus.albionmarket.AlbionMarket;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Wraps the Vault Economy API, providing a clean long-based interface.
 *
 * <p>All internal balances are stored as whole integer silver units (long).
 * Conversion to/from Vault's double-based API uses {@link BigDecimal} with
 * {@link RoundingMode#HALF_UP} to avoid floating-point drift (AD-05).</p>
 */
public class EconomyBridge {

    private final AlbionMarket plugin;
    private Economy economy;
    private boolean available;

    public EconomyBridge(AlbionMarket plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempts to hook into Vault's Economy provider.
     * Must be called after all plugins have been enabled (i.e., inside onEnable).
     */
    public void initialize() {
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().severe("[EconomyBridge] No Vault Economy provider found! " +
                    "Please install an economy plugin (e.g. EssentialsX). " +
                    "Economy operations will be disabled.");
            available = false;
            return;
        }

        economy = rsp.getProvider();
        available = true;
        plugin.getLogger().info("[EconomyBridge] Hooked into economy provider: " +
                economy.getName());
    }

    /**
     * Returns true if a Vault economy provider is available and initialized.
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Gets the player's current balance as a long (whole silver units).
     *
     * @param player the offline player to query
     * @return balance in silver, or -1 if economy is unavailable
     */
    public long getBalance(OfflinePlayer player) {
        if (!available) return -1L;
        double raw = economy.getBalance(player);
        return toInternalLong(raw);
    }

    /**
     * Withdraws {@code amount} silver from the player's account.
     *
     * @param player the offline player
     * @param amount amount to withdraw in silver (must be positive)
     * @return true if the withdrawal succeeded, false if funds were insufficient or economy unavailable
     */
    public boolean withdraw(OfflinePlayer player, long amount) {
        if (!available || amount <= 0) return false;
        double vaultAmount = toVaultDouble(amount);
        EconomyResponse response = economy.withdrawPlayer(player, vaultAmount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("[EconomyBridge] Withdraw failed for " +
                    player.getName() + " — " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    /**
     * Deposits {@code amount} silver into the player's account.
     *
     * @param player the offline player
     * @param amount amount to deposit in silver (must be positive)
     * @return true if the deposit succeeded, false if economy unavailable
     */
    public boolean deposit(OfflinePlayer player, long amount) {
        if (!available || amount <= 0) return false;
        double vaultAmount = toVaultDouble(amount);
        EconomyResponse response = economy.depositPlayer(player, vaultAmount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning("[EconomyBridge] Deposit failed for " +
                    player.getName() + " — " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    /**
     * Checks whether the player has at least {@code amount} silver.
     *
     * @param player the offline player
     * @param amount amount to check in silver
     * @return true if balance >= amount and economy is available
     */
    public boolean has(OfflinePlayer player, long amount) {
        if (!available) return false;
        return economy.has(player, toVaultDouble(amount));
    }

    /**
     * Converts a Vault double balance to an internal long (whole units, HALF_UP rounding).
     */
    public static long toInternalLong(double vaultDouble) {
        return BigDecimal.valueOf(vaultDouble)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    /**
     * Converts an internal long amount to the Vault double representation.
     */
    public static double toVaultDouble(long internal) {
        return (double) internal;
    }

    /**
     * Returns the underlying Vault Economy instance, or null if unavailable.
     */
    public Economy getEconomy() {
        return economy;
    }
}
