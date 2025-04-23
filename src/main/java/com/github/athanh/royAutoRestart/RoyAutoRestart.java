package com.github.athanh.royAutoRestart;

import com.github.athanh.royAutoRestart.commands.RoyAutoRestartCommand;
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

        // Register commands
        RoyAutoRestartCommand mainCommand = new RoyAutoRestartCommand(this);
        getCommand("royautorestart").setExecutor(mainCommand);
        getCommand("royautorestart").setTabCompleter(mainCommand);

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
