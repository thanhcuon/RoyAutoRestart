package com.github.athanh.royAutoRestart.papi;

import com.github.athanh.royAutoRestart.RoyAutoRestart;
import com.github.athanh.royAutoRestart.models.RestartTime;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

public class RestartPlaceholders extends PlaceholderExpansion {
    private final RoyAutoRestart plugin;

    public RestartPlaceholders(RoyAutoRestart plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "royrestart";
    }

    @Override
    public @NotNull String getAuthor() {
        return "athanh";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params.equals("time")) {
            return getFormattedTimeUntilNextRestart();
        }
        if (params.equals("day")) {
            return getNextRestartDay();
        }
        if (params.equals("hour")) {
            return getNextRestartHour();
        }
        if (params.equals("minute")) {
            return getNextRestartMinute();
        }
        return null;
    }

    private String getFormattedTimeUntilNextRestart() {
        LocalDateTime nextRestart = plugin.getRestartManager().getNextRestartDateTime();
        if (nextRestart == null) {
            return "N/A";
        }

        Duration duration = Duration.between(LocalDateTime.now(), nextRestart);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format("%d days %d hours %d minutes %d seconds", days, hours, minutes, seconds);
    }

    private String getNextRestartDay() {
        LocalDateTime nextRestart = plugin.getRestartManager().getNextRestartDateTime();
        if (nextRestart == null) {
            return "N/A";
        }

        String day = nextRestart.getDayOfWeek().name();
        return plugin.getConfig().getString("day-names." + day, day);
    }

    private String getNextRestartHour() {
        LocalDateTime nextRestart = plugin.getRestartManager().getNextRestartDateTime();
        return nextRestart == null ? "N/A" : String.valueOf(nextRestart.getHour());
    }

    private String getNextRestartMinute() {
        LocalDateTime nextRestart = plugin.getRestartManager().getNextRestartDateTime();
        return nextRestart == null ? "N/A" : String.format("%02d", nextRestart.getMinute());
    }
}