package com.perseus.albionmarket.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    private Utils() {}

    public static String colorize(String msg) {
        if (msg == null) return "";
        Matcher matcher = HEX_PATTERN.matcher(msg);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "§x§" + String.join("§", matcher.group().substring(1).split("")));
        }
        matcher.appendTail(buffer);
        return buffer.toString().replace('&', '§');
    }

    public static Component deserialize(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) return Component.empty();
        try {
            return MINI_MESSAGE.deserialize(miniMessage);
        } catch (Exception e) {
            return LegacyComponentSerializer.legacySection().deserialize(colorize(miniMessage));
        }
    }

    public static String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.format("%,d", number);
    }

    public static String formatMoney(long amount) {
        return formatNumber(amount) + " silver";
    }
}
