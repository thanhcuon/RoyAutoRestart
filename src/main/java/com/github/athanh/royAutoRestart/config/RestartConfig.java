package com.github.athanh.royAutoRestart.config;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
public class RestartConfig {
    private final Plugin plugin;
    private int hour;
    private int minute;

    public RestartConfig(Plugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    private void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        hour = config.getInt("restart.hour", 4);
        minute = config.getInt("restart.minute", 0);
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }
}
