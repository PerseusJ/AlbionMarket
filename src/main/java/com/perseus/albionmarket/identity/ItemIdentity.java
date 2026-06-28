package com.perseus.albionmarket.identity;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Immutable value object representing the canonical identity of a market-tradeable item.
 *
 * <p>An {@code ItemIdentity} is defined by two fields (AD-04):</p>
 * <ul>
 *   <li>{@link #material} — the Bukkit {@link Material} enum value</li>
 *   <li>{@link #nbtHash} — the 64-character SHA-256 hex fingerprint from {@link ItemHasher}</li>
 * </ul>
 *
 * <p>Two {@code ItemIdentity} instances are equal if and only if both their material
 * and nbtHash are identical. This means they will be stored under the same order book
 * entry and can be matched against each other.</p>
 *
 * <p>The {@link #displayLabel} is a human-readable cache used for GUI display only;
 * it does not participate in equality or hashing.</p>
 */
public final class ItemIdentity {

    private final Material material;
    private final String nbtHash;
    private final String displayLabel;

    private ItemIdentity(Material material, String nbtHash, String displayLabel) {
        this.material = material;
        this.nbtHash = nbtHash;
        this.displayLabel = displayLabel;
    }

    /**
     * Creates an {@code ItemIdentity} from an {@link ItemStack}.
     *
     * <p>Returns {@code null} if the item is null, air, fails the durability gate,
     * or if the hash computation fails for any reason.</p>
     *
     * @param item   the item stack to fingerprint
     * @param hasher the configured {@link ItemHasher} instance
     * @return the item's canonical identity, or {@code null} if the item cannot be listed
     */
    public static ItemIdentity of(ItemStack item, ItemHasher hasher) {
        if (item == null || item.getType().isAir()) return null;

        String hash = hasher.computeHash(item);
        if (hash == null) return null; // durability gate or hash error

        String label = resolveDisplayLabel(item);
        return new ItemIdentity(item.getType(), hash, label);
    }

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    /**
     * Returns the item's Bukkit material type.
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Returns the SHA-256 hex fingerprint of this item's sanitized NBT/data.
     */
    public String getNbtHash() {
        return nbtHash;
    }

    /**
     * Returns the human-readable display label used in GUIs and messages.
     * This is derived from the item's display name or formatted material name.
     */
    public String getDisplayLabel() {
        return displayLabel;
    }

    // -----------------------------------------------------------------------
    // Value semantics
    // -----------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemIdentity other)) return false;
        return material == other.material && Objects.equals(nbtHash, other.nbtHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(material, nbtHash);
    }

    @Override
    public String toString() {
        return "ItemIdentity{material=" + material.name() +
                ", hash=" + nbtHash.substring(0, 8) + "..., label='" + displayLabel + "'}";
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the display label for an item.
     * Prefers the item's custom display name; falls back to a formatted material name.
     */
    private static String resolveDisplayLabel(ItemStack item) {
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
        }
        // Format material name: DIAMOND_SWORD → Diamond Sword
        String raw = item.getType().name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
