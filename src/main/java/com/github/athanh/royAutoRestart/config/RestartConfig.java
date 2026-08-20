package com.github.athanh.royAutoRestart.config;

import com.github.athanh.royAutoRestart.models.RestartTime;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class RestartConfig {
    private final Plugin plugin;
    private final List<RestartTime> restartTimes;
    private final boolean bungeecordEnabled;
    private final String lobbyServer;
    private final String kickMessage;

    public RestartConfig(Plugin plugin) {
        this.plugin = plugin;
        this.restartTimes = new ArrayList<>();

        FileConfiguration config = plugin.getConfig();
        this.bungeecordEnabled = config.getBoolean("bungeecord.enabled", true);
        this.lobbyServer = config.getString("bungeecord.lobby-server", "lobby");
        this.kickMessage = config.getString("bungeecord.kick-message", "§cServer is restarting!");

        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        List<String> schedules = config.getStringList("restart-schedule");
        for (String schedule : schedules) {
            try {
                RestartTime restartTime = new RestartTime(schedule);
                restartTimes.add(restartTime);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid schedule entry: " + schedule + " (" + e.getMessage() + ")");
            }
        }
    }

    public List<RestartTime> getRestartTimes() {
        return restartTimes;
    }

    public boolean isBungeecordEnabled() {
        return bungeecordEnabled;
    }

    public String getLobbyServer() {
        return lobbyServer;
    }

    public String getKickMessage() {
        return kickMessage;
    }
}