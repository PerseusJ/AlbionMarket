package com.perseus.albionmarket.config;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final AlbionMarket plugin;
    private final Map<String, String> messages;
    private String prefix;
    private File file;
    private YamlConfiguration config;

    public MessageManager(AlbionMarket plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
    }

    public void load() {
        messages.clear();
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        prefix = config.getString("prefix", "<dark_gray>[</dark_gray><gradient:#FFD700:#FF8C00>Market</gradient><dark_gray>]</dark_gray> ");

        flattenConfig(config, "");
    }

    private void flattenConfig(YamlConfiguration config, String path) {
        for (String key : config.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (config.isConfigurationSection(key)) {
                flattenConfig(YamlConfiguration.loadConfiguration(file), fullPath);
            } else {
                String value = config.getString(key);
                if (value != null) {
                    messages.put(fullPath, value);
                }
            }
        }
    }

    private void flattenConfig(org.bukkit.configuration.ConfigurationSection section, String path) {
        for (String key : section.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (section.isConfigurationSection(key)) {
                flattenConfig(section.getConfigurationSection(key), fullPath);
            } else {
                String value = section.getString(key);
                if (value != null) {
                    messages.put(fullPath, value);
                }
            }
        }
    }

    public Component getMessage(String key) {
        String message = messages.get(key);
        if (message == null) {
            return Component.text("<missing key: " + key + ">");
        }
        return Utils.deserialize(message.replace("{prefix}", prefix));
    }

    public Component getMessage(String key, Map<String, String> placeholders) {
        String message = messages.get(key);
        if (message == null) {
            return Component.text("<missing key: " + key + ">");
        }
        message = message.replace("{prefix}", prefix);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return Utils.deserialize(message);
    }

    public String getRaw(String key) {
        return messages.getOrDefault(key, "<missing key: " + key + ">");
    }

    public String getPrefix() {
        return prefix;
    }

    public Component getPrefixed(String message) {
        return Utils.deserialize(prefix + message);
    }
}
