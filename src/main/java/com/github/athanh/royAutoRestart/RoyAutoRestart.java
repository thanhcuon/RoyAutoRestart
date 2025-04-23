package com.github.athanh.royAutoRestart;
import com.github.athanh.royAutoRestart.config.RestartConfig;
import com.github.athanh.royAutoRestart.manager.RestartManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RoyAutoRestart extends JavaPlugin {
    private RestartConfig config;
    private RestartManager restartManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new RestartConfig(this);
        restartManager = new RestartManager(this, config);
        getLogger().info("RoyAutoRestart has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("RoyAutoRestart has been disabled!");
    }
}
