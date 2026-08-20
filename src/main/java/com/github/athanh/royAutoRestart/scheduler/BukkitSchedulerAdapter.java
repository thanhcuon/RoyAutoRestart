package com.github.athanh.royAutoRestart.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standard Bukkit scheduler adapter for Spigot, Paper, Purpur, Leaf.
 */
public class BukkitSchedulerAdapter implements TaskScheduler {

    private final Plugin plugin;
    private final Set<BukkitTask> activeTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public TaskWrapper runTask(Runnable runnable) {
        BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
        activeTasks.add(task);
        return () -> {
            task.cancel();
            activeTasks.remove(task);
        };
    }

    @Override
    public TaskWrapper runTaskLater(Runnable runnable, long delayTicks) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        activeTasks.add(task);
        return () -> {
            task.cancel();
            activeTasks.remove(task);
        };
    }

    @Override
    public TaskWrapper runTaskTimer(Runnable runnable, long delayTicks, long periodTicks) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        activeTasks.add(task);
        return () -> {
            task.cancel();
            activeTasks.remove(task);
        };
    }

    @Override
    public TaskWrapper runTaskAsync(Runnable runnable) {
        BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        activeTasks.add(task);
        return () -> {
            task.cancel();
            activeTasks.remove(task);
        };
    }

    @Override
    public TaskWrapper runTaskLaterAsync(Runnable runnable, long delayTicks) {
        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks);
        activeTasks.add(task);
        return () -> {
            task.cancel();
            activeTasks.remove(task);
        };
    }

    @Override
    public void cancelAll() {
        for (BukkitTask task : activeTasks) {
            try {
                task.cancel();
            } catch (Throwable ignored) {}
        }
        activeTasks.clear();
    }
}
