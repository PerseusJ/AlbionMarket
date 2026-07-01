package com.perseus.albionmarket.database;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.ConfigManager;
import com.perseus.albionmarket.orders.MarketOrder;
import com.perseus.albionmarket.orders.OrderStatus;
import com.perseus.albionmarket.orders.OrderType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private final AlbionMarket plugin;
    private DatabaseProvider provider;
    private SchemaManager schemaManager;
    private boolean initialized;

    public DatabaseManager(AlbionMarket plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        ConfigManager configManager = plugin.getConfigManager();
        String dbType = configManager.getDatabaseType();

        if ("mysql".equalsIgnoreCase(dbType)) {
            provider = new MySQLProvider(configManager);
        } else {
            provider = new SQLiteProvider(configManager, plugin);
        }

        try {
            provider.initialize();
            initialized = true;
            plugin.getLogger().info("Database connected (" + dbType + ")");

            this.schemaManager = new SchemaManager(this);
            schemaManager.createTables();
            plugin.getLogger().info("Database schema initialized");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            initialized = false;
        }
    }

    public void shutdown() {
        if (provider != null) {
            provider.shutdown();
        }
        initialized = false;
    }

    public Connection getConnection() throws SQLException {
        if (!initialized || provider == null) {
            throw new SQLException("Database not initialized");
        }
        return provider.getDataSource().getConnection();
    }

    public DatabaseProvider getProvider() {
        return provider;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public AlbionMarket getPlugin() {
        return plugin;
    }

    public MarketOrder createOrder(Connection conn, String nodeId, String playerUuid,
                                   OrderType orderType, String material, String nbtHash,
                                   String displayLabel, String serializedItem, int quantity,
                                   long pricePerUnit, long totalEscrow, long setupFeePaid,
                                   Timestamp expiresAt) throws SQLException {
        String sql = "INSERT INTO market_orders (node_id, player_uuid, order_type, material, nbt_hash, " +
                "display_label, serialized_item, initial_quantity, remaining_quantity, price_per_unit, " +
                "total_escrow, setup_fee_paid, status, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nodeId);
            ps.setString(2, playerUuid);
            ps.setString(3, orderType.name());
            ps.setString(4, material);
            ps.setString(5, nbtHash);
            ps.setString(6, displayLabel);
            ps.setString(7, serializedItem);
            ps.setInt(8, quantity);
            ps.setInt(9, quantity);
            ps.setLong(10, pricePerUnit);
            ps.setLong(11, totalEscrow);
            ps.setLong(12, setupFeePaid);
            ps.setTimestamp(13, expiresAt);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long orderId = rs.getLong(1);
                    Timestamp now = new Timestamp(System.currentTimeMillis());
                    return new MarketOrder(orderId, nodeId, playerUuid, orderType,
                            material, nbtHash, displayLabel, serializedItem, quantity, quantity,
                            pricePerUnit, totalEscrow, setupFeePaid, OrderStatus.ACTIVE,
                            now, expiresAt, now);
                }
            }
        }
        throw new SQLException("Failed to retrieve generated order ID");
    }

    public MarketOrder getOrder(Connection conn, long orderId) throws SQLException {
        String sql = "SELECT * FROM market_orders WHERE order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapOrder(rs);
                }
            }
        }
        return null;
    }

    public boolean cancelOrderOptimistic(Connection conn, long orderId, String playerUuid) throws SQLException {
        String sql = "UPDATE market_orders SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP " +
                "WHERE order_id = ? AND status = 'ACTIVE' AND player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setString(2, playerUuid);
            return ps.executeUpdate() == 1;
        }
    }

    public int countActiveOrders(Connection conn, String playerUuid, String nodeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM market_orders WHERE player_uuid = ? AND node_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, nodeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countActiveOrders(String playerUuid, String nodeId) {
        try (Connection conn = getConnection()) {
            return countActiveOrders(conn, playerUuid, nodeId);
        } catch (SQLException e) {
            plugin.getLogger().warning("countActiveOrders failed: " + e.getMessage());
            return 0;
        }
    }

    public List<MarketOrder> getCounterOrdersForMatching(Connection conn, String nodeId,
                                                         String nbtHash, OrderType counterType) throws SQLException {
        String sql = "SELECT * FROM market_orders WHERE node_id = ? AND nbt_hash = ? " +
                "AND order_type = ? AND status = 'ACTIVE' ORDER BY " +
                (counterType == OrderType.SELL ? "price_per_unit ASC" : "price_per_unit DESC") +
                ", created_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nodeId);
            ps.setString(2, nbtHash);
            ps.setString(3, counterType.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<MarketOrder> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
                return orders;
            }
        }
    }

    public boolean reduceOrderQuantityOptimistic(Connection conn, long orderId,
                                                  int reduceBy, int expectedRemaining) throws SQLException {
        int newRemaining = expectedRemaining - reduceBy;
        String status = newRemaining <= 0 ? "FILLED" : "ACTIVE";
        String sql = "UPDATE market_orders SET remaining_quantity = ?, status = ?, updated_at = CURRENT_TIMESTAMP " +
                "WHERE order_id = ? AND status = 'ACTIVE' AND remaining_quantity = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(0, newRemaining));
            ps.setString(2, status);
            ps.setLong(3, orderId);
            ps.setInt(4, expectedRemaining);
            return ps.executeUpdate() == 1;
        }
    }

    public long insertTradeHistory(Connection conn, String nodeId, String buyerUuid,
                                   String sellerUuid, String material, String nbtHash, int quantity,
                                   long pricePerUnit, long totalValue, long taxAmount, long sellerPayout,
                                   String tradeType, Long buyerOrderId, Long sellerOrderId) throws SQLException {
        String sql = "INSERT INTO market_trade_history (node_id, buyer_uuid, seller_uuid, material, nbt_hash, " +
                "quantity, price_per_unit, total_value, tax_amount, seller_payout, trade_type, " +
                "buyer_order_id, seller_order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nodeId);
            ps.setString(2, buyerUuid);
            ps.setString(3, sellerUuid);
            ps.setString(4, material);
            ps.setString(5, nbtHash);
            ps.setInt(6, quantity);
            ps.setLong(7, pricePerUnit);
            ps.setLong(8, totalValue);
            ps.setLong(9, taxAmount);
            ps.setLong(10, sellerPayout);
            ps.setString(11, tradeType);
            if (buyerOrderId != null) ps.setLong(12, buyerOrderId); else ps.setNull(12, Types.BIGINT);
            if (sellerOrderId != null) ps.setLong(13, sellerOrderId); else ps.setNull(13, Types.BIGINT);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Failed to retrieve generated trade ID");
    }

    public List<MarketOrder> getActiveOrdersByPlayer(Connection conn, String playerUuid,
                                                      String nodeId) throws SQLException {
        String sql = "SELECT * FROM market_orders WHERE player_uuid = ? AND node_id = ? AND status = 'ACTIVE' " +
                "ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid);
            ps.setString(2, nodeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MarketOrder> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
                return orders;
            }
        }
    }

    private MarketOrder mapOrder(ResultSet rs) throws SQLException {
        return new MarketOrder(
                rs.getLong("order_id"),
                rs.getString("node_id"),
                rs.getString("player_uuid"),
                OrderType.valueOf(rs.getString("order_type")),
                rs.getString("material"),
                rs.getString("nbt_hash"),
                rs.getString("display_label"),
                rs.getString("serialized_item"),
                rs.getInt("initial_quantity"),
                rs.getInt("remaining_quantity"),
                rs.getLong("price_per_unit"),
                rs.getLong("total_escrow"),
                rs.getLong("setup_fee_paid"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("expires_at"),
                rs.getTimestamp("updated_at")
        );
    }

    public SchemaManager getSchemaManager() {
        return schemaManager;
    }
}
