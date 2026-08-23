package com.github.athanh.royAutoRestart.manager;

import com.github.athanh.royAutoRestart.config.ConfigManager;
import com.github.athanh.royAutoRestart.config.LanguageManager;
import com.github.athanh.royAutoRestart.models.RestartTime;
import com.github.athanh.royAutoRestart.scheduler.TaskScheduler;
import com.github.athanh.royAutoRestart.scheduler.TaskWrapper;
import com.github.athanh.royAutoRestart.utils.ColorUtil;
import com.github.athanh.royAutoRestart.utils.ServerUtil;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Coordinates and executes server restart sequences smoothly and safely with TimeZone awareness.
 */
public class RestartManager {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final LanguageManager languageManager;
    private final DiscordManager discordManager;
    private final TaskScheduler scheduler;

    private TaskWrapper scheduleCheckerTask;
    private final List<TaskWrapper> countdownTasks = new ArrayList<>();

    private boolean isRestarting = false;
    private int lastTriggeredMinute = -1;
    private int lastTriggeredHour = -1;
    private int lastTriggeredDay = -1;

    public RestartManager(Plugin plugin, ConfigManager configManager, LanguageManager languageManager, DiscordManager discordManager, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.languageManager = languageManager;
        this.discordManager = discordManager;
        this.scheduler = scheduler;

        startScheduleChecker();
    }

    /**
     * Get current LocalDateTime according to the configured TimeZone.
     */
    public LocalDateTime getNow() {
        return ZonedDateTime.now(configManager.getZoneId()).toLocalDateTime();
    }

    /**
     * Start periodic task to check if current time matches scheduled restart times.
     */
    private void startScheduleChecker() {
        if (scheduleCheckerTask != null) {
            scheduleCheckerTask.cancel();
        }

        // Check every 20 seconds (400 ticks)
        scheduleCheckerTask = scheduler.runTaskTimer(() -> {
            if (isRestarting) return;

            LocalDateTime now = getNow();
            int currentDay = now.getDayOfYear();
            int currentHour = now.getHour();
            int currentMinute = now.getMinute();

            // Prevent duplicate triggers within the same minute
            if (lastTriggeredDay == currentDay && lastTriggeredHour == currentHour && lastTriggeredMinute == currentMinute) {
                return;
            }

            for (RestartTime time : configManager.getRestartTimes()) {
                if (time.isMatch(now)) {
                    lastTriggeredDay = currentDay;
                    lastTriggeredHour = currentHour;
                    lastTriggeredMinute = currentMinute;

                    plugin.getLogger().info("[RoyAutoRestart] Scheduled restart time matched: " + time + " in timezone " + configManager.getZoneId().getId() + ". Starting restart sequence...");
                    startRestartSequence();
                    break;
                }
            }
        }, 200L, 400L);
    }

    /**
     * Start the standard countdown sequence (default 60 seconds or longest countdown interval).
     */
    public void startRestartSequence() {
        int initialTime = 60;
        List<Integer> countdowns = configManager.getCountdownTimes();
        if (!countdowns.isEmpty() && countdowns.get(0) > 0) {
            initialTime = countdowns.get(0);
        }
        startCountdown(initialTime, null);
    }

    /**
     * Start a manual server restart with optional custom message and delay.
     */
    public boolean startManualRestart(int seconds, String customMessage, int delaySeconds) {
        if (isRestarting) {
            return false;
        }

        if (delaySeconds > 0) {
            scheduler.runTaskLater(() -> startCountdown(seconds, customMessage), delaySeconds * 20L);
        } else {
            startCountdown(seconds, customMessage);
        }
        return true;
    }

    /**
     * Cancel any active restart sequence.
     */
    public boolean cancelRestart() {
        if (!isRestarting) {
            return false;
        }

        clearCountdownTasks();
        isRestarting = false;

        // Notify in game
        String cancelMsg = languageManager.getMessage("messages.commands.cancel-success");
        Bukkit.broadcastMessage(cancelMsg);

        // Notify discord
        discordManager.sendCancelled();

        plugin.getLogger().info("[RoyAutoRestart] Server restart sequence was cancelled.");
        return true;
    }

    private void clearCountdownTasks() {
        for (TaskWrapper task : countdownTasks) {
            if (task != null) {
                task.cancel();
            }
        }
        countdownTasks.clear();
    }

    /**
     * Execute the countdown steps and final restart sequence.
     */
    private void startCountdown(int totalSeconds, String customMessage) {
        clearCountdownTasks();
        isRestarting = true;

        List<Integer> configuredTimes = configManager.getCountdownTimes();
        List<Integer> intervalsToRun = new ArrayList<>();

        // Add matching interval points
        for (int t : configuredTimes) {
            if (t <= totalSeconds && !intervalsToRun.contains(t)) {
                intervalsToRun.add(t);
            }
        }
        if (!intervalsToRun.contains(totalSeconds)) {
            intervalsToRun.add(0, totalSeconds);
        }

        // Schedule notification for each interval
        for (int time : intervalsToRun) {
            long delayTicks = (long) (totalSeconds - time) * 20L;
            TaskWrapper task = scheduler.runTaskLater(() -> {
                if (!isRestarting) return;

                // 1. Chat Message
                String chatMsg;
                if (customMessage != null && !customMessage.isEmpty()) {
                    chatMsg = ColorUtil.colorize(customMessage.replace("%time%", String.valueOf(time)));
                } else {
                    chatMsg = languageManager.getCountdownMessage(time);
                }
                if (!chatMsg.isEmpty()) {
                    Bukkit.broadcastMessage(chatMsg);
                }

                // 2. Title & Subtitle
                if (languageManager.isTitleEnabled()) {
                    String title = languageManager.getTitle();
                    String subtitle = languageManager.getSubtitle(time);
                    int fadeIn = languageManager.getTitleFadeIn();
                    int stay = languageManager.getTitleStay();
                    int fadeOut = languageManager.getTitleFadeOut();

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        ServerUtil.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut);
                    }
                }

                // 3. Action Bar
                if (languageManager.isActionBarEnabled()) {
                    String actionBarMsg = languageManager.getActionBarMessage(time);
                    if (!actionBarMsg.isEmpty()) {
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            ServerUtil.sendActionBar(player, actionBarMsg);
                        }
                    }
                }

                // 4. Sound
                if (configManager.isSoundEnabled()) {
                    String soundName = configManager.getSoundName();
                    float volume = configManager.getSoundVolume();
                    float pitch = configManager.getSoundPitch();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        ServerUtil.playSound(player, soundName, volume, pitch);
                    }
                }

                // 5. Discord
                discordManager.sendCountdown(time);

            }, delayTicks);

            countdownTasks.add(task);
        }

        // Final shutdown task at T = totalSeconds
        TaskWrapper finalTask = scheduler.runTaskLater(this::executeFinalRestart, (long) totalSeconds * 20L);
        countdownTasks.add(finalTask);
    }

    /**
     * Execute the final restart sequence: save worlds, run pre-commands, transfer/kick players, and restart.
     */
    private void executeFinalRestart() {
        if (!isRestarting) return;

        plugin.getLogger().info("[RoyAutoRestart] Countdown finished. Executing safe server restart...");

        // 1. Save worlds
        if (configManager.isSaveWorlds()) {
            String savingMsg = languageManager.getSavingMessage();
            if (!savingMsg.isEmpty()) {
                Bukkit.broadcastMessage(savingMsg);
            }
            ServerUtil.saveWorlds();
            String savedMsg = languageManager.getSavedMessage();
            if (!savedMsg.isEmpty()) {
                Bukkit.broadcastMessage(savedMsg);
            }
        }

        // 2. Run custom pre-restart commands
        List<String> preCommands = configManager.getCommandsBeforeRestart();
        if (preCommands != null && !preCommands.isEmpty()) {
            for (String cmd : preCommands) {
                if (cmd != null && !cmd.trim().isEmpty()) {
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.trim());
                    } catch (Throwable t) {
                        plugin.getLogger().warning("[RoyAutoRestart] Error running pre-restart command '" + cmd + "': " + t.getMessage());
                    }
                }
            }
        }

        // 3. Discord notification
        discordManager.sendRestartingNow();

        // 4. BungeeCord player transfer or graceful kick
        if (configManager.isBungeecordEnabled()) {
            sendPlayersToLobbyAndRestart();
        } else {
            kickRemainingPlayersAndRestart();
        }
    }

    private void sendPlayersToLobbyAndRestart() {
        String lobby = configManager.getLobbyServer();
        String kickLobbyMsg = languageManager.getKickLobbyMessage();
        int sendDelay = Math.max(10, configManager.getSendDelayTicks());

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                if (!kickLobbyMsg.isEmpty()) {
                    player.sendMessage(kickLobbyMsg);
                }
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(lobby);
                player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            } catch (Throwable t) {
                plugin.getLogger().warning("[RoyAutoRestart] Failed to send player " + player.getName() + " to lobby: " + t.getMessage());
            }
        }

        // Wait send delay ticks to give proxy time to move players, then kick any remaining and restart
        scheduler.runTaskLater(this::kickRemainingPlayersAndRestart, sendDelay);
    }

    private void kickRemainingPlayersAndRestart() {
        if (configManager.isKickPlayersBeforeShutdown()) {
            String shutdownKickMsg = languageManager.getKickShutdownMessage();
            for (Player player : Bukkit.getOnlinePlayers()) {
                ServerUtil.kickPlayer(player, shutdownKickMsg);
            }
        }

        logRestartInfo();

        // Give a short 5 tick buffer for network buffers to flush before stopping
        scheduler.runTaskLater(() -> {
            ServerUtil.restartServer(configManager.getRestartCommand());
        }, 5L);
    }

    private void logRestartInfo() {
        Logger logger = plugin.getLogger();
        ZonedDateTime now = ZonedDateTime.now(configManager.getZoneId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        logger.info("========================================");
        logger.info("[RoyAutoRestart] Server Restart Triggered");
        logger.info("Timestamp: " + now.format(formatter) + " (" + configManager.getZoneId().getId() + ")");
        logger.info("========================================");
    }

    /**
     * Get human-readable information about the next scheduled restart time.
     */
    public String getNextRestartInfo() {
        LocalDateTime now = getNow();
        LocalDateTime nextRestart = null;
        RestartTime nextTime = null;

        for (RestartTime time : configManager.getRestartTimes()) {
            LocalDateTime occurrence = time.getNextOccurrence(now);
            if (nextRestart == null || occurrence.isBefore(nextRestart)) {
                nextRestart = occurrence;
                nextTime = time;
            }
        }

        if (nextRestart == null || nextTime == null) {
            return languageManager.getMessage("messages.infotime.not-found");
        }

        Duration duration = Duration.between(now, nextRestart);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        String dayName = languageManager.getDayName(nextTime.getDay());
        String daysUnit = languageManager.getTimeUnit("days");
        String hoursUnit = languageManager.getTimeUnit("hours");
        String minutesUnit = languageManager.getTimeUnit("minutes");
        String secondsUnit = languageManager.getTimeUnit("seconds");

        String timeStr = String.format("%02d:%02d", nextTime.getHour(), nextTime.getMinute());
        String remainingStr = days + " " + daysUnit + " " + hours + " " + hoursUnit + " " + minutes + " " + minutesUnit + " " + seconds + " " + secondsUnit;

        String header = languageManager.getMessage("messages.infotime.header");
        String timeLine = languageManager.getMessage("messages.infotime.time", "%hour%", String.format("%02d", nextTime.getHour()), "%minute%", String.format("%02d", nextTime.getMinute()), "%timezone%", configManager.getZoneId().getId());
        String dayLine = languageManager.getMessage("messages.infotime.day", "%day%", dayName);
        String remainingLine = languageManager.getMessage("messages.infotime.remaining",
                "%days%", String.valueOf(days) + " " + daysUnit,
                "%hours%", String.valueOf(hours) + " " + hoursUnit,
                "%minutes%", String.valueOf(minutes) + " " + minutesUnit,
                "%seconds%", String.valueOf(seconds) + " " + secondsUnit,
                "%remaining%", remainingStr
        );

        return String.join("\n", header, timeLine, dayLine, remainingLine);
    }

    /**
     * Get the exact LocalDateTime of the next scheduled restart.
     */
    public LocalDateTime getNextRestartDateTime() {
        LocalDateTime now = getNow();
        LocalDateTime nextRestart = null;

        for (RestartTime time : configManager.getRestartTimes()) {
            LocalDateTime occurrence = time.getNextOccurrence(now);
            if (nextRestart == null || occurrence.isBefore(nextRestart)) {
                nextRestart = occurrence;
            }
        }
        return nextRestart;
    }

    public boolean isRestarting() {
        return isRestarting;
    }

    public void shutdown() {
        if (scheduleCheckerTask != null) {
            scheduleCheckerTask.cancel();
            scheduleCheckerTask = null;
        }
        clearCountdownTasks();
        isRestarting = false;
    }
}