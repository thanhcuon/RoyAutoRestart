package com.github.athanh.royAutoRestart.manager;

import com.github.athanh.royAutoRestart.config.RestartConfig;
import com.github.athanh.royAutoRestart.models.RestartTime;
import com.github.athanh.royAutoRestart.utils.DiscordWebhook;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class RestartManager {
    private final Plugin plugin;
    private final RestartConfig config;
    private final int[] COUNTDOWN_TIMES = {60, 30, 10, 5, 4, 3, 2, 1};
    private boolean isRestarting = false;
    private int taskId = -1;
    private DiscordWebhook discord;
    private boolean serverFullyStarted = false;
    private final int CHECK_INTERVAL = 20;
    private final int MAX_CHECKS = 60;
    private int checkCount = 0;
    public RestartManager(Plugin plugin, RestartConfig config) {
        this.plugin = plugin;
        this.config = config;
        if (plugin.getConfig().getBoolean("discord.enabled")) {
            this.discord = new DiscordWebhook(plugin.getConfig().getString("discord.webhook-url"));
        }
        scheduleRestart();
    }

    private void scheduleRestart() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (isRestarting) return;

            LocalDateTime now = LocalDateTime.now();
            String currentDay = now.getDayOfWeek().name();

            for (RestartTime time : config.getRestartTimes()) {
                if (currentDay.equalsIgnoreCase(time.getDay()) &&
                        now.getHour() == time.getHour() &&
                        now.getMinute() == time.getMinute()) {
                    startRestartSequence();
                    break;
                }
            }
        }, 1200L, 1200L).getTaskId();
    }

    private void checkServerStatus() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (checkCount >= MAX_CHECKS) {
                serverFullyStarted = true;
                if (discord != null) {
                    String discordMessage = plugin.getConfig().getString("discord.messages.failed", "❌ Server khởi động không thành công sau 60 giây!");
                    discord.sendMessage(discordMessage);
                }
                return;
            }


            if (Bukkit.getOnlinePlayers().size() >= 0 &&
                    !Bukkit.hasWhitelist() &&
                    Bukkit.getServer().getTPS()[0] > 18.0) {

                serverFullyStarted = true;
                if (discord != null) {
                    String discordMessage = plugin.getConfig().getString("discord.messages.completed", "✅ Server đã khởi động lại thành công!");
                    discord.sendMessage(discordMessage);
                }
                Bukkit.getScheduler().cancelTask(taskId);
            }

            checkCount++;
        }, CHECK_INTERVAL, CHECK_INTERVAL).getTaskId();
    }

    private void startRestartSequence() {
        isRestarting = true;

        for (int time : COUNTDOWN_TIMES) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                String title = plugin.getConfig().getString("messages.title", "§c§lSERVER RESTART");
                String subtitle = plugin.getConfig().getString("messages.subtitle", "§eKhởi động lại sau %time% giây")
                        .replace("%time%", String.valueOf(time));

                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.sendTitle(title, subtitle, 10, 20, 10);
                });

                String chatMessage = plugin.getConfig().getString("messages.chat", "§c[Server] Server sẽ khởi động lại sau %time% giây!")
                        .replace("%time%", String.valueOf(time));
                Bukkit.broadcastMessage(chatMessage);

                if (discord != null && (time == 60 || time == 30 || time == 10 || time == 5)) {
                    String discordMessage = plugin.getConfig().getString("discord.messages.restart", "🔄 Server sẽ khởi động lại sau %time% giây!")
                            .replace("%time%", String.valueOf(time));
                    discord.sendMessage(discordMessage);
                }
            }, (COUNTDOWN_TIMES[0] - time) * 20L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            saveAllWorlds();
            logRestartInfo();

            if (discord != null) {
                String discordMessage = plugin.getConfig().getString("discord.messages.completed", "✅ Server đã khởi động lại thành công!");
                discord.sendMessage(discordMessage);
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
        }, COUNTDOWN_TIMES[0] * 20L);
    }

    private void saveAllWorlds() {
        String savingMessage = plugin.getConfig().getString("messages.saving", "§e[Server] Đang lưu thế giới...");
        Bukkit.broadcastMessage(savingMessage);

        for (World world : Bukkit.getWorlds()) {
            world.save();
        }

        String savedMessage = plugin.getConfig().getString("messages.saved", "§a[Server] Đã lưu xong thế giới!");
        Bukkit.broadcastMessage(savedMessage);
    }
    public String getNextRestartInfo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRestart = null;
        RestartTime nextTime = null;

        for (RestartTime time : config.getRestartTimes()) {
            LocalDateTime restartTime = getNextOccurrence(time);

            if (nextRestart == null || restartTime.isBefore(nextRestart)) {
                nextRestart = restartTime;
                nextTime = time;
            }
        }

        if (nextRestart == null || nextTime == null) {
            return plugin.getConfig().getString("messages.infotime.not-found");
        }

        Duration duration = Duration.between(now, nextRestart);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        String dayName = plugin.getConfig().getString("day-names." + nextTime.getDay().toUpperCase(), nextTime.getDay());

        return String.join("\n",
                plugin.getConfig().getString("messages.infotime.header"),
                plugin.getConfig().getString("messages.infotime.time")
                        .replace("%hour%", String.valueOf(nextTime.getHour()))
                        .replace("%minute%", String.format("%02d", nextTime.getMinute())),
                plugin.getConfig().getString("messages.infotime.day")
                        .replace("%day%", dayName),
                plugin.getConfig().getString("messages.infotime.remaining")
                        .replace("%days%", String.valueOf(days))
                        .replace("%hours%", String.valueOf(hours))
                        .replace("%minutes%", String.valueOf(minutes))
                        .replace("%seconds%", String.valueOf(seconds))
        );
    }
    private LocalDateTime getNextOccurrence(RestartTime time) {
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek targetDay = DayOfWeek.valueOf(time.getDay().toUpperCase());
        LocalDateTime target = now
                .with(ChronoField.HOUR_OF_DAY, time.getHour())
                .with(ChronoField.MINUTE_OF_HOUR, time.getMinute())
                .with(ChronoField.SECOND_OF_MINUTE, 0);

        if (targetDay == now.getDayOfWeek() && target.isBefore(now) ||
                targetDay.getValue() < now.getDayOfWeek().getValue()) {
            target = target.plusWeeks(1);
        }

        return target.with(ChronoField.DAY_OF_WEEK, targetDay.getValue());
    }

    public void startManualRestart(int seconds, String customMessage, int delaySeconds) {
        if (isRestarting) {
            return;
        }

        isRestarting = true;

        if (delaySeconds > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                startCountdown(seconds, customMessage);
            }, delaySeconds * 20L);
        } else {
            startCountdown(seconds, customMessage);
        }
    }
    private void kickPlayersToLobby() {
        if (!config.isBungeecordEnabled()) {
            return;
        }

        String lobbyServer = config.getLobbyServer();
        String kickMessage = config.getKickMessage();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int playerCount = Bukkit.getOnlinePlayers().size();
            int processedPlayers = 0;

            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF("Connect");
                    out.writeUTF(lobbyServer);

                    player.sendMessage(kickMessage);
                    player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
                    processedPlayers++;

                    plugin.getLogger().info("Sending player " + player.getName() + " to lobby server (" + processedPlayers + "/" + playerCount + ")");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to send player " + player.getName() + " to lobby server: " + e.getMessage());
                }
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (discord != null) {
                    checkServerStatus();
                }
                plugin.getLogger().info("All players have been sent to lobby, restarting server...");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
            }, 40L);

        }, 20L);
    }

    private void startCountdown(int seconds, String customMessage) {

        List<Integer> times = new ArrayList<>();
        if (seconds > 60) times.add(60);
        if (seconds > 30) times.add(30);
        if (seconds > 10) times.add(10);
        for (int i = 5; i > 0; i--) {
            if (seconds > i) times.add(i);
        }

        for (int time : times) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String title = plugin.getConfig().getString("messages.title", "§c§lSERVER RESTART");
                String subtitle = plugin.getConfig().getString("messages.subtitle")
                        .replace("%time%", String.valueOf(time));

                String message = customMessage != null ?
                        customMessage.replace("%time%", String.valueOf(time)) :
                        plugin.getConfig().getString("messages.chat")
                                .replace("%time%", String.valueOf(time));

                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.sendTitle(title, subtitle, 10, 20, 10);
                });
                Bukkit.broadcastMessage(message);

                if (discord != null && (time == 60 || time == 30 || time == 10 || time == 5)) {
                    String discordMessage = plugin.getConfig().getString("discord.messages.restart", "🔄 Server sẽ khởi động lại sau %time% giây!")
                            .replace("%time%", String.valueOf(time));
                    discord.sendMessage(discordMessage);
                }
            }, (seconds - time) * 20L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            saveAllWorlds();
            logRestartInfo();

            kickPlayersToLobby();

        }, seconds * 20L);
    }


    private void logRestartInfo() {
        Logger logger = plugin.getLogger();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        logger.info("=== Server Restart Log ===");
        logger.info("Restart time: " + now.format(formatter));
        logger.info("========================");
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        isRestarting = false;
    }
    public LocalDateTime getNextRestartDateTime() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRestart = null;

        for (RestartTime time : config.getRestartTimes()) {
            LocalDateTime restartTime = getNextOccurrence(time);

            if (nextRestart == null || restartTime.isBefore(nextRestart)) {
                nextRestart = restartTime;
            }
        }

        return nextRestart;
    }
}