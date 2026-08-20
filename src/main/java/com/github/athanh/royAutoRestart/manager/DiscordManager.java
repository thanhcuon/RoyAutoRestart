package com.github.athanh.royAutoRestart.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Handles asynchronous Discord Webhook notifications with embed support.
 */
public class DiscordManager {

    private final Plugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean enabled;
    private String webhookUrl;
    private String username;
    private String avatarUrl;
    private List<Integer> notifyIntervals;

    private boolean embedEnabled;
    private int embedColor;
    private String authorName;
    private String authorIcon;
    private String footerText;
    private String footerIcon;

    private String msgRestartCountdown;
    private String msgRestartingNow;
    private String msgCompleted;
    private String msgCancelled;
    private String msgFailed;

    public DiscordManager(Plugin plugin, FileConfiguration discordConfig) {
        this.plugin = plugin;
        loadConfig(discordConfig);
    }

    public void loadConfig(FileConfiguration config) {
        if (config == null) {
            this.enabled = false;
            return;
        }

        this.enabled = config.getBoolean("enabled", false);
        this.webhookUrl = config.getString("webhook-url", "");
        this.username = config.getString("username", "RoyAutoRestart");
        this.avatarUrl = config.getString("avatar-url", "");
        this.notifyIntervals = config.getIntegerList("notify-intervals");

        this.embedEnabled = config.getBoolean("embed.enabled", true);
        String colorHex = config.getString("embed.color", "#FFAA00").replace("#", "");
        try {
            this.embedColor = Integer.parseInt(colorHex, 16);
        } catch (NumberFormatException e) {
            this.embedColor = 0xFFAA00;
        }

        this.authorName = config.getString("embed.author.name", "RoyAutoRestart System");
        this.authorIcon = config.getString("embed.author.icon-url", "");
        this.footerText = config.getString("embed.footer.text", "Server Auto Restart • %time_now%");
        this.footerIcon = config.getString("embed.footer.icon-url", "");

        this.msgRestartCountdown = config.getString("messages.restart-countdown", "🔄 **Server Restart:** Server will restart in **%time%** seconds!");
        this.msgRestartingNow = config.getString("messages.restarting-now", "⚠️ **Server Restart:** Server is restarting now! Saving world data...");
        this.msgCompleted = config.getString("messages.completed", "✅ **Server Online:** Server has restarted successfully!");
        this.msgCancelled = config.getString("messages.cancelled", "❌ **Server Restart:** Server restart sequence was cancelled.");
        this.msgFailed = config.getString("messages.failed", "❌ **Server Alert:** Server restart did not complete within expected time!");
    }

    public boolean isEnabled() {
        return enabled && webhookUrl != null && !webhookUrl.trim().isEmpty() && !webhookUrl.contains("YOUR_DISCORD_WEBHOOK_URL");
    }

    public boolean shouldNotify(int seconds) {
        return isEnabled() && notifyIntervals != null && notifyIntervals.contains(seconds);
    }

    public void sendCountdown(int seconds) {
        if (!shouldNotify(seconds)) return;
        String text = msgRestartCountdown.replace("%time%", String.valueOf(seconds));
        sendWebhook(text);
    }

    public void sendRestartingNow() {
        if (!isEnabled()) return;
        sendWebhook(msgRestartingNow);
    }

    public void sendCompleted() {
        if (!isEnabled()) return;
        sendWebhook(msgCompleted);
    }

    public void sendCancelled() {
        if (!isEnabled()) return;
        sendWebhook(msgCancelled);
    }

    public void sendFailed() {
        if (!isEnabled()) return;
        sendWebhook(msgFailed);
    }

    private void sendWebhook(String messageContent) {
        if (!isEnabled()) return;

        executor.submit(() -> {
            try {
                String formattedNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                String finalContent = messageContent.replace("%time_now%", formattedNow);

                String jsonPayload;
                if (embedEnabled) {
                    jsonPayload = buildEmbedJson(finalContent, formattedNow);
                } else {
                    jsonPayload = buildPlainJson(finalContent);
                }

                URL url = java.net.URI.create(webhookUrl).toURL();
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setRequestProperty("User-Agent", "RoyAutoRestart-Discord-Bot");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                byte[] out = jsonPayload.getBytes(StandardCharsets.UTF_8);
                try (OutputStream stream = connection.getOutputStream()) {
                    stream.write(out);
                    stream.flush();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogger().warning("[RoyAutoRestart] Discord Webhook returned HTTP " + responseCode);
                }
                connection.disconnect();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "[RoyAutoRestart] Failed to send Discord Webhook: " + t.getMessage());
            }
        });
    }

    private String buildPlainJson(String content) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"content\": \"").append(escapeJson(content)).append("\"");
        if (username != null && !username.isEmpty()) {
            json.append(", \"username\": \"").append(escapeJson(username)).append("\"");
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            json.append(", \"avatar_url\": \"").append(escapeJson(avatarUrl)).append("\"");
        }
        json.append("}");
        return json.toString();
    }

    private String buildEmbedJson(String description, String timeNow) {
        StringBuilder json = new StringBuilder("{");
        if (username != null && !username.isEmpty()) {
            json.append("\"username\": \"").append(escapeJson(username)).append("\", ");
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            json.append("\"avatar_url\": \"").append(escapeJson(avatarUrl)).append("\", ");
        }

        json.append("\"embeds\": [{");
        json.append("\"description\": \"").append(escapeJson(description)).append("\", ");
        json.append("\"color\": ").append(embedColor);

        if (authorName != null && !authorName.isEmpty()) {
            json.append(", \"author\": { \"name\": \"").append(escapeJson(authorName)).append("\"");
            if (authorIcon != null && !authorIcon.isEmpty()) {
                json.append(", \"icon_url\": \"").append(escapeJson(authorIcon)).append("\"");
            }
            json.append("}");
        }

        String finalFooter = footerText != null ? footerText.replace("%time_now%", timeNow) : "";
        if (!finalFooter.isEmpty()) {
            json.append(", \"footer\": { \"text\": \"").append(escapeJson(finalFooter)).append("\"");
            if (footerIcon != null && !footerIcon.isEmpty()) {
                json.append(", \"icon_url\": \"").append(escapeJson(footerIcon)).append("\"");
            }
            json.append("}");
        }

        json.append("}]}");
        return json.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public void shutdown() {
        executor.shutdown();
    }
}
