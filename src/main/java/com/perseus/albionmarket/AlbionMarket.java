package com.perseus.albionmarket;

import org.bukkit.plugin.java.JavaPlugin;
import com.perseus.albionmarket.managers.PluginManager;
import com.perseus.albionmarket.listeners.PlayerListener;

public class AlbionMarket extends JavaPlugin {
    
    @Override
    public void onEnable() {
        
        // Initialize managers
        PluginManager.getInstance().initialize();
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        
        getLogger().info("AlbionMarket has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AlbionMarket has been disabled!");
    }
    
}