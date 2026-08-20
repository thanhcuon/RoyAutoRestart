package com.github.athanh.royAutoRestart.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.logging.Level;

/**
 * Server utility providing multi-version compatible and safe operations
 * for Spigot, Paper, Purpur, Leaf, Folia and versions 1.18.x to 26.x.
 */
public final class ServerUtil {

    private ServerUtil() {
        // Private constructor
    }

    /**
     * Send Title and Subtitle safely to a player.
     *
     * @param player Target player
     * @param title Title text
     * @param subtitle Subtitle text
     * @param fadeIn Fade in ticks
     * @param stay Stay ticks
     * @param fadeOut Fade out ticks
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            player.sendTitle(
                    title != null ? ColorUtil.colorize(title) : "",
                    subtitle != null ? ColorUtil.colorize(subtitle) : "",
                    Math.max(0, fadeIn),
                    Math.max(0, stay),
                    Math.max(0, fadeOut)
            );
        } catch (Throwable t) {
            // Fallback for edge cases
            try {
                player.sendMessage((title != null ? ColorUtil.colorize(title) : "") + " " + (subtitle != null ? ColorUtil.colorize(subtitle) : ""));
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Send Action Bar safely to a player.
     *
     * @param player Target player
     * @param message Action bar message
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline() || message == null || message.isEmpty()) {
            return;
        }
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(ColorUtil.colorize(message)));
        } catch (Throwable t) {
            try {
                player.sendMessage(ColorUtil.colorize(message));
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Play sound safely to a player with string lookup.
     *
     * @param player Target player
     * @param soundName Sound name enum (e.g. BLOCK_NOTE_BLOCK_PLING, ENTITY_EXPERIENCE_ORB_PICKUP)
     * @param volume Sound volume
     * @param pitch Sound pitch
     */
    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || !player.isOnline() || soundName == null || soundName.trim().isEmpty()) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().trim());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Try standard fallback sounds
            try {
                Sound fallback = Sound.valueOf("BLOCK_NOTE_BLOCK_PLING");
                player.playSound(player.getLocation(), fallback, volume, pitch);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }

    /**
     * Save all loaded worlds safely.
     */
    public static void saveWorlds() {
        try {
            for (World world : Bukkit.getWorlds()) {
                if (world != null) {
                    world.save();
                }
            }
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.WARNING, "[RoyAutoRestart] Error while saving worlds: " + t.getMessage(), t);
        }
    }

    /**
     * Kick player safely with a message.
     *
     * @param player Target player
     * @param message Kick message
     */
    public static void kickPlayer(Player player, String message) {
        if (player == null || !player.isOnline()) {
            return;
        }
        try {
            player.kickPlayer(ColorUtil.colorize(message != null ? message : "Server is restarting."));
        } catch (Throwable t) {
            try {
                player.kickPlayer("Server restarting.");
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Execute server restart or shutdown command safely.
     *
     * @param restartCommand Configured restart command (e.g. "restart", "stop")
     */
    public static void restartServer(String restartCommand) {
        Bukkit.getLogger().info("[RoyAutoRestart] Executing server restart...");

        // Try Spigot restart API first if available and restartCommand is "restart"
        if (restartCommand == null || restartCommand.trim().isEmpty() || restartCommand.equalsIgnoreCase("restart")) {
            try {
                Bukkit.spigot().restart();
                return;
            } catch (Throwable ignored) {
                // Bukkit.spigot().restart() might not be supported or failed, fallback to command
            }
        }

        // Fallback to console dispatch command
        String cmd = (restartCommand != null && !restartCommand.trim().isEmpty()) ? restartCommand.trim() : "restart";
        try {
            boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            if (!dispatched) {
                Bukkit.shutdown();
            }
        } catch (Throwable t) {
            Bukkit.getLogger().log(Level.SEVERE, "[RoyAutoRestart] Failed to dispatch restart command, invoking Bukkit.shutdown()", t);
            try {
                Bukkit.shutdown();
            } catch (Throwable ignored) {}
        }
    }
}
