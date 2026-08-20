package com.github.athanh.royAutoRestart.config;

import com.github.athanh.royAutoRestart.models.RestartTime;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages configuration loading for config.yml and discord.yml.
 */
public class ConfigManager {

    private final Plugin plugin;
    private final LanguageManager languageManager;

    private List<RestartTime> restartTimes;
    private List<Integer> countdownTimes;
    private boolean bungeecordEnabled;
    private String lobbyServer;
    private int sendDelayTicks;

    private boolean saveWorlds;
    private boolean kickPlayersBeforeShutdown;
    private String restartCommand;
    private List<String> commandsBeforeRestart;

    private boolean soundEnabled;
    private String soundName;
    private float soundVolume;
    private float soundPitch;

    // Discord Config
    private FileConfiguration discordConfig;

    public ConfigManager(Plugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        load();
    }

    public void load() {
        // Save default config.yml & discord.yml if not existing
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        // 1. Language
        String language = config.getString("language", "vi_vn");
        languageManager.loadLanguage(language);

        // 2. Restart Schedule
        this.restartTimes = new ArrayList<>();
        List<String> schedules = config.getStringList("restart-schedule");
        for (String schedule : schedules) {
            try {
                RestartTime rt = new RestartTime(schedule);
                restartTimes.add(rt);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[RoyAutoRestart] Invalid restart-schedule entry: '" + schedule + "' (" + e.getMessage() + ")");
            }
        }

        // 3. Countdown Times
        List<Integer> rawCountdown = config.getIntegerList("countdown-times");
        if (rawCountdown.isEmpty()) {
            rawCountdown = List.of(60, 30, 10, 5, 4, 3, 2, 1);
        }
        this.countdownTimes = new ArrayList<>(rawCountdown);
        // Sort descending
        this.countdownTimes.sort(Collections.reverseOrder());

        // 4. BungeeCord
        this.bungeecordEnabled = config.getBoolean("bungeecord.enabled", true);
        this.lobbyServer = config.getString("bungeecord.lobby-server", "lobby");
        this.sendDelayTicks = config.getInt("bungeecord.send-delay-ticks", 20);

        // 5. Safe Restart
        this.saveWorlds = config.getBoolean("safe-restart.save-worlds", true);
        this.kickPlayersBeforeShutdown = config.getBoolean("safe-restart.kick-players-before-shutdown", true);
        this.restartCommand = config.getString("safe-restart.restart-command", "restart");
        this.commandsBeforeRestart = config.getStringList("safe-restart.commands-before-restart");

        // 6. Sound
        this.soundEnabled = config.getBoolean("sound.enabled", true);
        this.soundName = config.getString("sound.name", "BLOCK_NOTE_BLOCK_PLING");
        this.soundVolume = (float) config.getDouble("sound.volume", 1.0);
        this.soundPitch = (float) config.getDouble("sound.pitch", 1.2);

        // 7. Load discord.yml
        loadDiscordConfig();
    }

    private void loadDiscordConfig() {
        File discordFile = new File(plugin.getDataFolder(), "discord.yml");
        if (!discordFile.exists()) {
            plugin.saveResource("discord.yml", false);
        }
        this.discordConfig = YamlConfiguration.loadConfiguration(discordFile);
    }

    public List<RestartTime> getRestartTimes() {
        return restartTimes;
    }

    public List<Integer> getCountdownTimes() {
        return countdownTimes;
    }

    public boolean isBungeecordEnabled() {
        return bungeecordEnabled;
    }

    public String getLobbyServer() {
        return lobbyServer;
    }

    public int getSendDelayTicks() {
        return sendDelayTicks;
    }

    public boolean isSaveWorlds() {
        return saveWorlds;
    }

    public boolean isKickPlayersBeforeShutdown() {
        return kickPlayersBeforeShutdown;
    }

    public String getRestartCommand() {
        return restartCommand;
    }

    public List<String> getCommandsBeforeRestart() {
        return commandsBeforeRestart;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public String getSoundName() {
        return soundName;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public FileConfiguration getDiscordConfig() {
        return discordConfig;
    }
}
