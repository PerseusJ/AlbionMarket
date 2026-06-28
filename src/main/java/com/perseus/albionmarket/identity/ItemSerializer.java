package com.perseus.albionmarket.identity;

import com.perseus.albionmarket.AlbionMarket;
import org.bukkit.inventory.ItemStack;

import java.util.Base64;
import java.util.logging.Logger;

/**
 * Handles Base64 serialization and deserialization of {@link ItemStack}s.
 *
 * <p>Serialized strings are stored in the {@code market_orders.serialized_item} and
 * {@code player_mailboxes.serialized_item} database columns. The format is:</p>
 * <pre>Base64( Bukkit.ItemStack.serializeAsBytes() )</pre>
 *
 * <p>This uses Bukkit's own internal binary format, which is versioned and
 * migrated automatically by the server on startup. No custom serialization
 * logic is required.</p>
 */
public class ItemSerializer {

    private final Logger log;

    public ItemSerializer(AlbionMarket plugin) {
        this.log = plugin.getLogger();
    }

    /**
     * Serializes an {@link ItemStack} to a Base64-encoded string suitable for database storage.
     *
     * @param item the item stack to serialize (may be null — returns null)
     * @return a Base64 string, or {@code null} if the item is null or serialization fails
     */
    public String serialize(ItemStack item) {
        if (item == null) return null;
        try {
            byte[] bytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.warning("[ItemSerializer] Failed to serialize item " +
                    item.getType().name() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Deserializes a Base64-encoded string back into an {@link ItemStack}.
     *
     * @param encoded the Base64 string previously produced by {@link #serialize(ItemStack)}
     *                (may be null or empty — returns null)
     * @return the reconstructed {@link ItemStack}, or {@code null} if deserialization fails
     */
    public ItemStack deserialize(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            log.warning("[ItemSerializer] Failed to deserialize item from Base64: " + e.getMessage());
            return null;
        }
    }
}
