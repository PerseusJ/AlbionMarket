package com.perseus.albionmarket.identity;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.ConfigManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generates deterministic SHA-256 fingerprints for {@link ItemStack}s.
 *
 * <p><b>V1.0 Hashing strategy (AD-04)</b>:</p>
 * <ul>
 *   <li>Material name — primary type discriminator</li>
 *   <li>Custom model data — distinguishes visual variants of the same material</li>
 *   <li>Item name (display name) — if custom-named, it's part of the identity</li>
 *   <li>Enchantments — a diamond sword with Sharpness V is a different market item</li>
 *   <li>PDC keys configured via {@code item-identity.pdc-keys-to-hash} — hooks for
 *       MMOItems, ItemsAdder, Oraxen etc. in V2.0+</li>
 * </ul>
 *
 * <p><b>Intentionally excluded from hash</b> (volatile / irrelevant):</p>
 * <ul>
 *   <li>Durability / damage — controlled by the durability gate, not the hash</li>
 *   <li>Repair cost — mutable anvil data, not part of item identity</li>
 *   <li>Lore — may contain server-injected player-specific text</li>
 * </ul>
 */
public class ItemHasher {

    private final ConfigManager configManager;
    private final Logger log;

    public ItemHasher(AlbionMarket plugin) {
        this.configManager = plugin.getConfigManager();
        this.log = plugin.getLogger();
    }

    // -----------------------------------------------------------------------
    // Durability gate
    // -----------------------------------------------------------------------

    /**
     * Returns {@code true} if the item can accumulate durability damage
     * (i.e., has a durability bar in-game).
     */
    public boolean isDamageable(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        return meta instanceof Damageable;
    }

    /**
     * Returns {@code true} if the item is at full (100%) durability.
     *
     * <p>Non-damageable items always pass this check — a stone block is
     * considered "full durability" because it has no durability concept.</p>
     *
     * @param item the item stack to check
     * @return true if the item is undamaged or not damageable
     */
    public boolean isFullDurability(ItemStack item) {
        if (item == null) return false;
        if (!isDamageable(item)) return true;
        Damageable damageable = (Damageable) item.getItemMeta();
        return damageable == null || damageable.getDamage() == 0;
    }

    // -----------------------------------------------------------------------
    // Hash computation
    // -----------------------------------------------------------------------

    /**
     * Computes a SHA-256 hex hash fingerprint for the given item stack.
     *
     * <p>Returns {@code null} if:</p>
     * <ul>
     *   <li>The item is null or air</li>
     *   <li>The durability gate is enabled and the item is damaged</li>
     * </ul>
     *
     * @param item the item to fingerprint
     * @return 64-character lowercase hex SHA-256 string, or {@code null} on failure
     */
    public String computeHash(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        // Durability gate (AD-04)
        if (configManager.isRequireFullDurability() && !isFullDurability(item)) {
            return null;
        }

        StringBuilder fingerprint = new StringBuilder();

        // 1. Material — primary discriminator
        fingerprint.append("material=").append(item.getType().name()).append(";");

        // 2. Custom model data — visual variant discriminator
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasCustomModelData()) {
                fingerprint.append("cmd=").append(meta.getCustomModelData()).append(";");
            }

            // 3. Display name — custom-named items are distinct market items
            if (meta.hasDisplayName()) {
                // Use plain text to avoid MiniMessage tag variance
                fingerprint.append("name=").append(meta.getDisplayName()).append(";");
            }

            // 4. Enchantments — sorted by key for determinism
            if (meta.hasEnchants()) {
                meta.getEnchants().entrySet().stream()
                        .sorted((a, b) -> a.getKey().getKey().compareTo(b.getKey().getKey()))
                        .forEach(e -> fingerprint
                                .append("ench=").append(e.getKey().getKey())
                                .append(":").append(e.getValue()).append(";"));
            }

            // 5. Configurable PDC keys (V2.0+ hooks: MMOItems, ItemsAdder, Oraxen)
            List<String> pdcKeys = configManager.getPdcKeysToHash();
            if (pdcKeys != null && !pdcKeys.isEmpty()) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                for (String rawKey : pdcKeys) {
                    try {
                        String[] parts = rawKey.split(":", 2);
                        if (parts.length != 2) continue;
                        org.bukkit.NamespacedKey nk = new org.bukkit.NamespacedKey(parts[0], parts[1]);
                        if (pdc.has(nk, PersistentDataType.STRING)) {
                            String val = pdc.get(nk, PersistentDataType.STRING);
                            fingerprint.append("pdc[").append(rawKey).append("]=").append(val).append(";");
                        }
                    } catch (Exception e) {
                        log.warning("[ItemHasher] Could not read PDC key '" + rawKey + "': " + e.getMessage());
                    }
                }
            }
        }

        return sha256Hex(fingerprint.toString());
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in any Java SE runtime
            log.severe("[ItemHasher] SHA-256 algorithm unavailable — this should never happen!");
            return null;
        }
    }
}
