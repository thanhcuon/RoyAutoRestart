package com.github.athanh.royAutoRestart.papi;

import com.github.athanh.royAutoRestart.RoyAutoRestart;
import com.github.athanh.royAutoRestart.config.LanguageManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * PlaceholderAPI expansion for RoyAutoRestart.
 * Placeholders:
 * %royrestart_time% - Formatted remaining time until next restart
 * %royrestart_day% - Day name of next restart
 * %royrestart_hour% - Hour of next restart
 * %royrestart_minute% - Minute of next restart
 * %royrestart_total_seconds% - Total seconds remaining
 * %royrestart_is_restarting% - "true" if countdown active, "false" otherwise
 */
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
        return "2.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return null;
        }

        String lower = params.toLowerCase();

        switch (lower) {
            case "time":
            case "remaining":
                return getFormattedTimeUntilNextRestart();

            case "day":
                return getNextRestartDay();

            case "hour":
                return getNextRestartHour();

            case "minute":
                return getNextRestartMinute();

            case "total_seconds":
                return getTotalSecondsUntilNextRestart();

            case "is_restarting":
                return String.valueOf(plugin.getRestartManager().isRestarting());

            default:
                return null;
        }
    }

    private String getFormattedTimeUntilNextRestart() {
        LocalDateTime nextRestart = plugin.getRestartManager().getNextRestartDateTime();
        if (nextRestart == null) {
            return "N/A";
        }

        Duration duration = Duration.between(LocalDateTime.now(), nextRestart);
        if (duration.isNegative() || duration.isZero()) {
            return "0";
        }

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        LanguageManager lang = plugin.getLanguageManager();
        String daysUnit = lang.getTimeUnit("days");
        String hoursUnit = lang.getTimeUnit("hours");
        String minutesUnit = lang.getTimeUnit("minutes");
        String secondsUnit = lang.getTimeUnit("seconds");

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(" ").append(daysUnit).append(" ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append(" ").append(hoursUnit).append(" ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append(" ").append(minutesUnit).append(" ");
        }
        sb.append(seconds).append(" ").append(secondsUnit);

        return sb.toString().trim();
    }

    private String getNextRestartDay() {
        LocalDateTime nextRestart = plugin.getRestartManager().getNextRestartDateTime();
        if (nextRestart == null) {
            return "N/A";
        }

        String day = nextRestart.getDayOfWeek().name();
        return plugin.getLanguageManager().getDayName(day);
    }

    private String getNextRestartHour() {
        LocalDateTime nextRestart = plugin.getRestartManager().getNextRestartDateTime();
        return nextRestart == null ? "N/A" : String.format("%02d", nextRestart.getHour());
    }

    private String getNextRestartMinute() {
        LocalDateTime nextRestart = plugin.getRestartManager().getNextRestartDateTime();
        return nextRestart == null ? "N/A" : String.format("%02d", nextRestart.getMinute());
    }

    private String getTotalSecondsUntilNextRestart() {
        LocalDateTime nextRestart = plugin.getRestartManager().getNextRestartDateTime();
        if (nextRestart == null) {
            return "0";
        }
        Duration duration = Duration.between(LocalDateTime.now(), nextRestart);
        return String.valueOf(Math.max(0, duration.toSeconds()));
    }
}