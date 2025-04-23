package com.github.athanh.royAutoRestart.manager;

import com.github.athanh.royAutoRestart.config.RestartConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class RestartManager {
    private final Plugin plugin;
    private final RestartConfig config;
    private final int[] COUNTDOWN_TIMES = {60, 30, 10, 5, 4, 3, 2, 1};
    private boolean isRestarting = false;

    public RestartManager(Plugin plugin, RestartConfig config) {
        this.plugin = plugin;
        this.config = config;
        scheduleRestart();
    }

    private void scheduleRestart() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            LocalDateTime now = LocalDateTime.now();

            if (now.getHour() == config.getHour() && now.getMinute() == config.getMinute() && !isRestarting) {
                startRestartSequence();
            }
        }, 20L, 20L); // Check every second
    }

    private void startRestartSequence() {
        isRestarting = true;

        for (int time : COUNTDOWN_TIMES) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.broadcastMessage("§c[Server] Server sẽ khởi động lại sau " + time + " giây!");
            }, (COUNTDOWN_TIMES[0] - time) * 20L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            saveAllWorlds();
            logRestartInfo();
            Bukkit.shutdown();
        }, COUNTDOWN_TIMES[0] * 20L);
    }

    private void saveAllWorlds() {
        Bukkit.broadcastMessage("§e[Server] Đang lưu thế giới...");
        for (World world : Bukkit.getWorlds()) {
            world.save();
        }
        Bukkit.broadcastMessage("§a[Server] Đã lưu xong thế giới!");
    }

    private void logRestartInfo() {
        Logger logger = plugin.getLogger();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        long uptime = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime();
        long uptimeHours = uptime / (60 * 60 * 1000);

        logger.info("=== Server Restart Log ===");
        logger.info("Restart time: " + LocalDateTime.now().format(formatter));
        logger.info("Uptime: " + uptimeHours + " hours");
        logger.info("========================");
    }
}