package com.github.athanh.royAutoRestart.scheduler;

import org.bukkit.plugin.Plugin;

/**
 * Platform-independent task scheduler abstraction.
 * Supports Bukkit, Spigot, Paper, Purpur, Leaf, and Folia.
 */
public interface TaskScheduler {

    /**
     * Run a task synchronously on the main thread / global region.
     */
    TaskWrapper runTask(Runnable runnable);

    /**
     * Run a task after a delay in ticks.
     */
    TaskWrapper runTaskLater(Runnable runnable, long delayTicks);

    /**
     * Run a recurring task with delay and period in ticks.
     */
    TaskWrapper runTaskTimer(Runnable runnable, long delayTicks, long periodTicks);

    /**
     * Run a task asynchronously.
     */
    TaskWrapper runTaskAsync(Runnable runnable);

    /**
     * Run a task asynchronously after a delay in ticks.
     */
    TaskWrapper runTaskLaterAsync(Runnable runnable, long delayTicks);

    /**
     * Cancel all tasks managed by this scheduler.
     */
    void cancelAll();

    /**
     * Factory method to create appropriate scheduler adapter.
     */
    static TaskScheduler create(Plugin plugin) {
        if (isFolia()) {
            plugin.getLogger().info("[RoyAutoRestart] Detected Folia server environment. Using Folia Region Scheduler.");
            return new FoliaSchedulerAdapter(plugin);
        } else {
            return new BukkitSchedulerAdapter(plugin);
        }
    }

    /**
     * Check if the server is running on Folia.
     */
    static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
