package com.perseus.albionmarket.database;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaManager {

    private final DatabaseManager databaseManager;

    public SchemaManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void createTables() {
        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS schema_version (" +
                "version INTEGER NOT NULL," +
                "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "description VARCHAR(256)" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS market_orders (" +
                "order_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "node_id VARCHAR(64) NOT NULL," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "order_type VARCHAR(4) NOT NULL CHECK(order_type IN ('BUY','SELL'))," +
                "material VARCHAR(128) NOT NULL," +
                "nbt_hash VARCHAR(64) NOT NULL," +
                "display_label VARCHAR(256)," +
                "serialized_item TEXT," +
                "initial_quantity INTEGER NOT NULL," +
                "remaining_quantity INTEGER NOT NULL," +
                "price_per_unit BIGINT NOT NULL," +
                "total_escrow BIGINT DEFAULT 0," +
                "setup_fee_paid BIGINT DEFAULT 0," +
                "status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','FILLED','CANCELLED','EXPIRED'))," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "expires_at TIMESTAMP NOT NULL," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_orders_lookup ON market_orders(node_id, nbt_hash, order_type, status)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_orders_player ON market_orders(player_uuid, node_id, status)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_orders_expiry ON market_orders(status, expires_at)"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_mailboxes (" +
                "entry_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "node_id VARCHAR(64) NOT NULL," +
                "asset_type VARCHAR(4) NOT NULL CHECK(asset_type IN ('ITEM','CASH'))," +
                "serialized_item TEXT," +
                "cash_amount BIGINT," +
                "source_order_id BIGINT," +
                "source_type VARCHAR(64) NOT NULL," +
                "claimed BOOLEAN DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "claimed_at TIMESTAMP" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_mailbox_lookup ON player_mailboxes(player_uuid, node_id, claimed)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_mailbox_pending ON player_mailboxes(player_uuid, claimed)"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS market_trade_history (" +
                "trade_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "node_id VARCHAR(64) NOT NULL," +
                "buyer_uuid VARCHAR(36) NOT NULL," +
                "seller_uuid VARCHAR(36) NOT NULL," +
                "material VARCHAR(128) NOT NULL," +
                "nbt_hash VARCHAR(64) NOT NULL," +
                "quantity INTEGER NOT NULL," +
                "price_per_unit BIGINT NOT NULL," +
                "total_value BIGINT NOT NULL," +
                "tax_amount BIGINT DEFAULT 0," +
                "seller_payout BIGINT NOT NULL," +
                "trade_type VARCHAR(16) NOT NULL," +
                "buyer_order_id BIGINT," +
                "seller_order_id BIGINT," +
                "executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_trade_history_node ON market_trade_history(node_id, nbt_hash, executed_at)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_trade_history_node_stats ON market_trade_history(node_id, executed_at)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_trade_history_buyer ON market_trade_history(buyer_uuid, executed_at)"
            );
            stmt.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_trade_history_seller ON market_trade_history(seller_uuid, executed_at)"
            );

            stmt.executeUpdate(
                "INSERT OR IGNORE INTO schema_version (version, description) VALUES (1, 'Initial schema')"
            );

        } catch (Exception e) {
            databaseManager.getPlugin().getLogger().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
