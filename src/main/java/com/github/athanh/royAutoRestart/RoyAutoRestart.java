package com.github.athanh.royAutoRestart;

import com.github.athanh.royAutoRestart.commands.RoyAutoRestartCommand;
import com.github.athanh.royAutoRestart.config.RestartConfig;
import com.github.athanh.royAutoRestart.manager.RestartManager;
import com.github.athanh.royAutoRestart.papi.RestartPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class RoyAutoRestart extends JavaPlugin {
    private RestartConfig config;
    private RestartManager restartManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new RestartConfig(this);
        if (config.isBungeecordEnabled()) {
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            getLogger().info("BungeeCord messaging enabled!");
        }
        restartManager = new RestartManager(this, config);
        RoyAutoRestartCommand mainCommand = new RoyAutoRestartCommand(this);
        getCommand("royautorestart").setExecutor(mainCommand);
        getCommand("royautorestart").setTabCompleter(mainCommand);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RestartPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }
        getLogger().info("RoyAutoRestart has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("RoyAutoRestart has been disabled!");
    }

    public void reloadConfiguration() {
        reloadConfig();
        config = new RestartConfig(this);

        if (restartManager != null) {
            restartManager.stop();
        }
        restartManager = new RestartManager(this, config);
    }
    public RestartManager getRestartManager() {
        return restartManager;
    }
}