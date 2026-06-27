package com.perseus.albionmarket.commands;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.MarketNode;
import com.perseus.albionmarket.config.MarketNodeRegistry;
import com.perseus.albionmarket.config.MessageManager;
import com.perseus.albionmarket.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;

public class AdminSubcommand implements MarketCommand.Subcommand {

    private static final NamespacedKey NODE_KEY = new NamespacedKey("albionmarket", "node_id");

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("albionmarket.admin")) {
            MessageManager msg = AlbionMarket.getInstance().getMessageManager();
            sender.sendMessage(msg.getMessage("admin.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(Utils.deserialize(
                "<gold>/market admin link <node_id></gold> <gray>- Link target entity to market node</gray>\n" +
                "<gold>/market admin reload</gold> <gray>- Reload configuration</gray>\n" +
                "<gold>/market admin info <node_id></gold> <gray>- View node info</gray>"
            ));
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "link":
                return handleLink(sender, args);
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender, args);
            default:
                MessageManager msg = AlbionMarket.getInstance().getMessageManager();
                sender.sendMessage(msg.getPrefixed("<red>Unknown admin action.</red>"));
                return true;
        }
    }

    private boolean handleLink(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Utils.deserialize("<red>Usage: /market admin link <node_id></red>"));
            return true;
        }

        String nodeId = args[1];
        MarketNodeRegistry registry = AlbionMarket.getInstance().getMarketNodeRegistry();
        MessageManager msg = AlbionMarket.getInstance().getMessageManager();

        if (!registry.containsNode(nodeId)) {
            sender.sendMessage(msg.getMessage("admin.invalid-node", Map.of("node_id", nodeId)));
            return true;
        }

        Entity target = player.getTargetEntity(5);
        if (target == null) {
            sender.sendMessage(msg.getPrefixed("<red>Look at an entity within 5 blocks.</red>"));
            return true;
        }

        PersistentDataContainer pdc = target.getPersistentDataContainer();
        pdc.set(NODE_KEY, PersistentDataType.STRING, nodeId);

        MarketNode node = registry.getNode(nodeId);
        sender.sendMessage(msg.getMessage("admin.linked", Map.of(
            "node_name", node.getDisplayName(),
            "node_id", nodeId
        )));

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        AlbionMarket plugin = AlbionMarket.getInstance();
        plugin.getConfigManager().load();
        plugin.getMarketNodeRegistry().load();
        plugin.getMessageManager().load();

        MessageManager msg = plugin.getMessageManager();
        sender.sendMessage(msg.getMessage("admin.reloaded"));
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Utils.deserialize("<red>Usage: /market admin info <node_id></red>"));
            return true;
        }

        String nodeId = args[1];
        MarketNodeRegistry registry = AlbionMarket.getInstance().getMarketNodeRegistry();

        if (!registry.containsNode(nodeId)) {
            MessageManager msg = AlbionMarket.getInstance().getMessageManager();
            sender.sendMessage(msg.getMessage("admin.invalid-node", Map.of("node_id", nodeId)));
            return true;
        }

        MarketNode node = registry.getNode(nodeId);
        sender.sendMessage(Utils.deserialize(
            "<gold>=== Node: " + nodeId + " ===</gold>\n" +
            "<gray>Display: " + node.getDisplayName() + "</gray>\n" +
            "<gray>Setup Fee: " + node.getSetupFeePercent() + "%</gray>\n" +
            "<gray>Tax: " + node.getTransactionTaxPercent() + "%</gray>\n" +
            "<gray>Max Duration: " + node.getMaxOrderDurationDays() + " days</gray>\n" +
            "<gray>Max Orders/Player: " + node.getMaxActiveOrdersPerPlayer() + "</gray>"
        ));
        return true;
    }
}
