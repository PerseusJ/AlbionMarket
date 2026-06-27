package com.perseus.albionmarket.events;

import com.perseus.albionmarket.config.MarketNode;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class MarketNodeInteractEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final MarketNode node;
    private final String nodeId;

    public MarketNodeInteractEvent(Player who, MarketNode node, String nodeId) {
        super(who);
        this.node = node;
        this.nodeId = nodeId;
    }

    public MarketNode getNode() {
        return node;
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
