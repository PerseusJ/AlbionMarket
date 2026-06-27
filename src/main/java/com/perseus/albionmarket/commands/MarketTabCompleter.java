package com.perseus.albionmarket.commands;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.MarketNodeRegistry;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MarketTabCompleter implements TabCompleter {

    private static final List<String> ADMIN_ACTIONS = List.of("link", "reload", "info");
    private static final List<String> ROOT_COMPLETIONS = List.of("admin");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filterCompletions(ROOT_COMPLETIONS, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return filterCompletions(ADMIN_ACTIONS, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("admin")
                && (args[1].equalsIgnoreCase("link") || args[1].equalsIgnoreCase("info"))) {
            MarketNodeRegistry registry = AlbionMarket.getInstance().getMarketNodeRegistry();
            List<String> nodeIds = new ArrayList<>();
            registry.getAllNodes().forEach(node -> nodeIds.add(node.getNodeId()));
            return filterCompletions(nodeIds, args[2]);
        }

        return List.of();
    }

    private List<String> filterCompletions(List<String> options, String prefix) {
        if (prefix.isEmpty()) return options;
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                filtered.add(option);
            }
        }
        return filtered;
    }
}
