package com.github.athanh.royAutoRestart;

import com.github.athanh.royAutoRestart.commands.RoyAutoRestartCommand;
import com.github.athanh.royAutoRestart.config.ConfigManager;
import com.github.athanh.royAutoRestart.config.LanguageManager;
import com.github.athanh.royAutoRestart.manager.DiscordManager;
import com.github.athanh.royAutoRestart.manager.RestartManager;
import com.github.athanh.royAutoRestart.papi.RestartPlaceholders;
import com.github.athanh.royAutoRestart.scheduler.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class RoyAutoRestart extends JavaPlugin {

    private TaskScheduler taskScheduler;
    private LanguageManager languageManager;
    private ConfigManager configManager;
    private DiscordManager discordManager;
    private RestartManager restartManager;

    @Override
    public void onEnable() {
        // Initialize Scheduler Abstraction (Spigot, Paper, Purpur, Leaf, Folia)
        this.taskScheduler = TaskScheduler.create(this);

        // Initialize Language and Config Managers
        this.languageManager = new LanguageManager(this);
        this.configManager = new ConfigManager(this, languageManager);
        this.discordManager = new DiscordManager(this, configManager.getDiscordConfig(), configManager.getZoneId());

        // Register BungeeCord outgoing channel if enabled
        if (configManager.isBungeecordEnabled()) {
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            getLogger().info("[RoyAutoRestart] BungeeCord messaging channel registered.");
        }

        // Initialize Restart Manager
        this.restartManager = new RestartManager(this, configManager, languageManager, discordManager, taskScheduler);

        // Register Commands and Tab Completers
        RoyAutoRestartCommand mainCommand = new RoyAutoRestartCommand(this);
        if (getCommand("royautorestart") != null) {
            getCommand("royautorestart").setExecutor(mainCommand);
            getCommand("royautorestart").setTabCompleter(mainCommand);
        }

        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RestartPlaceholders(this).register();
            getLogger().info("[RoyAutoRestart] PlaceholderAPI expansion registered successfully.");
        }

        getLogger().info("[RoyAutoRestart] Plugin v" + getDescription().getVersion() + " enabled successfully! Active schedules: " + configManager.getRestartTimes().size());
    }

    @Override
    public void onDisable() {
        if (restartManager != null) {
            restartManager.shutdown();
        }
        if (taskScheduler != null) {
            taskScheduler.cancelAll();
        }
        if (discordManager != null) {
            discordManager.shutdown();
        }
        getLogger().info("[RoyAutoRestart] Plugin disabled.");
    }

    /**
     * Reload all configuration files, language files, and schedules.
     */
    public void reloadConfiguration() {
        if (restartManager != null) {
            restartManager.shutdown();
        }
        if (taskScheduler != null) {
            taskScheduler.cancelAll();
        }

        configManager.load();
        discordManager.loadConfig(configManager.getDiscordConfig(), configManager.getZoneId());
        restartManager = new RestartManager(this, configManager, languageManager, discordManager, taskScheduler);

        getLogger().info("[RoyAutoRestart] Configuration reloaded successfully.");
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }
}