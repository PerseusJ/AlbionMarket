package com.perseus.albionmarket.listeners;

import com.perseus.albionmarket.AlbionMarket;
import com.perseus.albionmarket.config.MarketNode;
import com.perseus.albionmarket.config.MarketNodeRegistry;
import com.perseus.albionmarket.events.MarketNodeInteractEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class EntityInteractionListener implements Listener {

    private static final NamespacedKey NODE_KEY = new NamespacedKey("albionmarket", "node_id");

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (entity.getType() == EntityType.INTERACTION || entity instanceof org.bukkit.entity.Villager) {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (pdc.has(NODE_KEY, PersistentDataType.STRING)) {
                String nodeId = pdc.get(NODE_KEY, PersistentDataType.STRING);
                MarketNodeRegistry registry = AlbionMarket.getInstance().getMarketNodeRegistry();

                if (nodeId != null && registry.containsNode(nodeId)) {
                    event.setCancelled(true);
                    MarketNode node = registry.getNode(nodeId);
                    MarketNodeInteractEvent interactEvent = new MarketNodeInteractEvent(player, node, nodeId);
                    player.getServer().getPluginManager().callEvent(interactEvent);
                }
            }
        }
    }

    public static NamespacedKey getNodeKey() {
        return NODE_KEY;
    }
}
