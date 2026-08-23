package com.github.athanh.royAutoRestart.config;

import com.github.athanh.royAutoRestart.models.RestartTime;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages configuration loading for config.yml and discord.yml.
 */
public class ConfigManager {

    private final Plugin plugin;
    private final LanguageManager languageManager;

    private ZoneId zoneId;
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

        // 2. TimeZone
        String timeZoneStr = config.getString("timezone", "Asia/Ho_Chi_Minh");
        this.zoneId = parseZoneId(timeZoneStr);
        plugin.getLogger().info("[RoyAutoRestart] Timezone configured: " + zoneId.getId() + " (" + zoneId.getRules().getOffset(Instant.now()) + ")");

        // 3. Restart Schedule
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

        // 4. Countdown Times
        List<Integer> rawCountdown = config.getIntegerList("countdown-times");
        if (rawCountdown.isEmpty()) {
            rawCountdown = List.of(60, 30, 10, 5, 4, 3, 2, 1);
        }
        this.countdownTimes = new ArrayList<>(rawCountdown);
        // Sort descending
        this.countdownTimes.sort(Collections.reverseOrder());

        // 5. BungeeCord
        this.bungeecordEnabled = config.getBoolean("bungeecord.enabled", true);
        this.lobbyServer = config.getString("bungeecord.lobby-server", "lobby");
        this.sendDelayTicks = config.getInt("bungeecord.send-delay-ticks", 20);

        // 6. Safe Restart
        this.saveWorlds = config.getBoolean("safe-restart.save-worlds", true);
        this.kickPlayersBeforeShutdown = config.getBoolean("safe-restart.kick-players-before-shutdown", true);
        this.restartCommand = config.getString("safe-restart.restart-command", "restart");
        this.commandsBeforeRestart = config.getStringList("safe-restart.commands-before-restart");

        // 7. Sound
        this.soundEnabled = config.getBoolean("sound.enabled", true);
        this.soundName = config.getString("sound.name", "BLOCK_NOTE_BLOCK_PLING");
        this.soundVolume = (float) config.getDouble("sound.volume", 1.0);
        this.soundPitch = (float) config.getDouble("sound.pitch", 1.2);

        // 8. Load discord.yml
        loadDiscordConfig();
    }

    private ZoneId parseZoneId(String zoneInput) {
        if (zoneInput == null || zoneInput.trim().isEmpty()) {
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }
        String trimmed = zoneInput.trim();
        if ("SYSTEM".equalsIgnoreCase(trimmed) || "DEFAULT".equalsIgnoreCase(trimmed) || "AUTO".equalsIgnoreCase(trimmed)) {
            return ZoneId.systemDefault();
        }
        if (trimmed.equalsIgnoreCase("VN") || trimmed.equalsIgnoreCase("VIETNAM") || trimmed.equalsIgnoreCase("VN_TZ")) {
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }
        // Handle GMT+7, UTC+7, +07:00, -05:00, etc.
        if (trimmed.matches("(?i)^(GMT|UTC)?[+-]\\d{1,2}(:\\d{2})?$")) {
            String normalized = trimmed.toUpperCase();
            if (!normalized.startsWith("GMT") && !normalized.startsWith("UTC")) {
                normalized = "GMT" + normalized;
            } else if (normalized.startsWith("UTC")) {
                normalized = "GMT" + normalized.substring(3);
            }
            try {
                return ZoneId.of(normalized);
            } catch (Exception ignored) {}
        }
        try {
            return ZoneId.of(trimmed);
        } catch (Exception e) {
            plugin.getLogger().warning("[RoyAutoRestart] Invalid timezone: '" + zoneInput + "'. Falling back to 'Asia/Ho_Chi_Minh' (GMT+7).");
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }
    }

    private void loadDiscordConfig() {
        File discordFile = new File(plugin.getDataFolder(), "discord.yml");
        if (!discordFile.exists()) {
            plugin.saveResource("discord.yml", false);
        }
        this.discordConfig = YamlConfiguration.loadConfiguration(discordFile);
    }

    public ZoneId getZoneId() {
        return zoneId != null ? zoneId : ZoneId.of("Asia/Ho_Chi_Minh");
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
