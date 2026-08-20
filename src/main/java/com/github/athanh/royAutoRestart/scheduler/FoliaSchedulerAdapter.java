package com.github.athanh.royAutoRestart.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Folia scheduler adapter using reflection to support Folia's GlobalRegionScheduler and AsyncScheduler.
 */
public class FoliaSchedulerAdapter implements TaskScheduler {

    private final Plugin plugin;
    private final Set<Object> activeTasks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Object globalRegionScheduler;
    private Method globalRun;
    private Method globalRunDelayed;
    private Method globalRunAtFixedRate;

    private Object asyncScheduler;
    private Method asyncRunNow;
    private Method asyncRunDelayed;
    private Method asyncRunAtFixedRate;

    private Method taskCancelMethod;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        initReflection();
    }

    private void initReflection() {
        try {
            Object server = Bukkit.getServer();
            Method getGlobalSched = server.getClass().getMethod("getGlobalRegionScheduler");
            globalRegionScheduler = getGlobalSched.invoke(server);

            Class<?> globalClass = globalRegionScheduler.getClass();
            globalRun = globalClass.getMethod("run", Plugin.class, Consumer.class);
            globalRunDelayed = globalClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
            globalRunAtFixedRate = globalClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);

            Method getAsyncSched = server.getClass().getMethod("getAsyncScheduler");
            asyncScheduler = getAsyncSched.invoke(server);

            Class<?> asyncClass = asyncScheduler.getClass();
            asyncRunNow = asyncClass.getMethod("runNow", Plugin.class, Consumer.class);
            asyncRunDelayed = asyncClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class, TimeUnit.class);
            asyncRunAtFixedRate = asyncClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);

            Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            taskCancelMethod = scheduledTaskClass.getMethod("cancel");
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "[RoyAutoRestart] Failed to initialize Folia scheduler reflection, fallback to Bukkit", t);
        }
    }

    @Override
    public TaskWrapper runTask(Runnable runnable) {
        if (globalRegionScheduler != null && globalRun != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = globalRun.invoke(globalRegionScheduler, plugin, consumer);
                if (scheduledTask != null) {
                    activeTasks.add(scheduledTask);
                    return () -> cancelTask(scheduledTask);
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "[RoyAutoRestart] Error running Folia task", t);
            }
        }
        // Fallback
        return new BukkitSchedulerAdapter(plugin).runTask(runnable);
    }

    @Override
    public TaskWrapper runTaskLater(Runnable runnable, long delayTicks) {
        if (globalRegionScheduler != null && globalRunDelayed != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = globalRunDelayed.invoke(globalRegionScheduler, plugin, consumer, Math.max(1, delayTicks));
                if (scheduledTask != null) {
                    activeTasks.add(scheduledTask);
                    return () -> cancelTask(scheduledTask);
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "[RoyAutoRestart] Error running Folia delayed task", t);
            }
        }
        // Fallback
        return new BukkitSchedulerAdapter(plugin).runTaskLater(runnable, delayTicks);
    }

    @Override
    public TaskWrapper runTaskTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (globalRegionScheduler != null && globalRunAtFixedRate != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = globalRunAtFixedRate.invoke(globalRegionScheduler, plugin, consumer, Math.max(1, delayTicks), Math.max(1, periodTicks));
                if (scheduledTask != null) {
                    activeTasks.add(scheduledTask);
                    return () -> cancelTask(scheduledTask);
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "[RoyAutoRestart] Error running Folia timer task", t);
            }
        }
        // Fallback
        return new BukkitSchedulerAdapter(plugin).runTaskTimer(runnable, delayTicks, periodTicks);
    }

    @Override
    public TaskWrapper runTaskAsync(Runnable runnable) {
        if (asyncScheduler != null && asyncRunNow != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = asyncRunNow.invoke(asyncScheduler, plugin, consumer);
                if (scheduledTask != null) {
                    activeTasks.add(scheduledTask);
                    return () -> cancelTask(scheduledTask);
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "[RoyAutoRestart] Error running Folia async task", t);
            }
        }
        // Fallback
        return new BukkitSchedulerAdapter(plugin).runTaskAsync(runnable);
    }

    @Override
    public TaskWrapper runTaskLaterAsync(Runnable runnable, long delayTicks) {
        if (asyncScheduler != null && asyncRunDelayed != null) {
            try {
                Consumer<Object> consumer = task -> runnable.run();
                long delayMs = delayTicks * 50L;
                Object scheduledTask = asyncRunDelayed.invoke(asyncScheduler, plugin, consumer, delayMs, TimeUnit.MILLISECONDS);
                if (scheduledTask != null) {
                    activeTasks.add(scheduledTask);
                    return () -> cancelTask(scheduledTask);
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "[RoyAutoRestart] Error running Folia delayed async task", t);
            }
        }
        // Fallback
        return new BukkitSchedulerAdapter(plugin).runTaskLaterAsync(runnable, delayTicks);
    }

    private void cancelTask(Object scheduledTask) {
        if (scheduledTask != null && taskCancelMethod != null) {
            try {
                taskCancelMethod.invoke(scheduledTask);
            } catch (Throwable ignored) {}
        }
        activeTasks.remove(scheduledTask);
    }

    @Override
    public void cancelAll() {
        for (Object task : activeTasks) {
            if (taskCancelMethod != null) {
                try {
                    taskCancelMethod.invoke(task);
                } catch (Throwable ignored) {}
            }
        }
        activeTasks.clear();
    }
}
