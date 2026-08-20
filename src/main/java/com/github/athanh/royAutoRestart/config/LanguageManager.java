package com.github.athanh.royAutoRestart.config;

import com.github.athanh.royAutoRestart.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages multi-language files and translations from lang/*.yml.
 */
public class LanguageManager {

    private final Plugin plugin;
    private FileConfiguration langConfig;
    private FileConfiguration fallbackConfig;
    private String currentLanguage;

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load language configuration according to language setting in config.yml.
     */
    public void loadLanguage(String langCode) {
        if (langCode == null || langCode.trim().isEmpty()) {
            langCode = "vi_vn";
        }
        langCode = langCode.trim().toLowerCase();
        this.currentLanguage = langCode;

        // Ensure default language files exist in plugins/RoyAutoRestart/lang/
        saveDefaultLanguageFile("vi_vn.yml");
        saveDefaultLanguageFile("en_us.yml");

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        String fileName = langCode.endsWith(".yml") ? langCode : langCode + ".yml";
        File langFile = new File(langFolder, fileName);

        if (!langFile.exists()) {
            plugin.getLogger().warning("[RoyAutoRestart] Language file '" + fileName + "' not found! Falling back to 'vi_vn.yml'.");
            langFile = new File(langFolder, "vi_vn.yml");
            this.currentLanguage = "vi_vn";
        }

        this.langConfig = YamlConfiguration.loadConfiguration(langFile);

        // Load fallback config from embedded resource
        InputStream fallbackStream = plugin.getResource("lang/" + (this.currentLanguage.equals("en_us") ? "en_us.yml" : "vi_vn.yml"));
        if (fallbackStream != null) {
            this.fallbackConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(fallbackStream, StandardCharsets.UTF_8));
        }

        plugin.getLogger().info("[RoyAutoRestart] Loaded language: " + this.currentLanguage);
    }

    private void saveDefaultLanguageFile(String resourceName) {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        File targetFile = new File(langFolder, resourceName);
        if (!targetFile.exists()) {
            try {
                plugin.saveResource("lang/" + resourceName, false);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Failed to save default lang file: " + resourceName, t);
            }
        }
    }

    /**
     * Retrieve a raw string with fallback.
     */
    public String getRaw(String path, String defaultValue) {
        if (langConfig != null && langConfig.contains(path)) {
            return langConfig.getString(path, defaultValue);
        }
        if (fallbackConfig != null && fallbackConfig.contains(path)) {
            return fallbackConfig.getString(path, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Retrieve a formatted colorized message with placeholder replacements.
     *
     * @param path YAML path
     * @param placeholders Key-value replacement pairs (e.g. "%time%", "60", "%label%", "rar")
     * @return Colorized string
     */
    public String getMessage(String path, Object... placeholders) {
        String msg = getRaw(path, "");
        if (msg == null || msg.isEmpty()) {
            return "";
        }
        msg = applyPlaceholders(msg, placeholders);
        return ColorUtil.colorize(msg);
    }

    /**
     * Retrieve a list of formatted colorized messages with placeholder replacements.
     */
    public List<String> getMessageList(String path, Object... placeholders) {
        List<String> list = null;
        if (langConfig != null && langConfig.contains(path)) {
            list = langConfig.getStringList(path);
        }
        if ((list == null || list.isEmpty()) && fallbackConfig != null && fallbackConfig.contains(path)) {
            list = fallbackConfig.getStringList(path);
        }

        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>(list.size());
        for (String line : list) {
            String processed = applyPlaceholders(line, placeholders);
            result.add(ColorUtil.colorize(processed));
        }
        return result;
    }

    private String applyPlaceholders(String text, Object... placeholders) {
        if (text == null || placeholders == null || placeholders.length < 2) {
            return text;
        }
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String key = String.valueOf(placeholders[i]);
            String val = String.valueOf(placeholders[i + 1]);
            text = text.replace(key, val);
        }
        return text;
    }

    // --- Helper translation getters ---

    public String getDayName(String dayKey) {
        if (dayKey == null) return "N/A";
        String upper = dayKey.toUpperCase();
        String translated = getRaw("day-names." + upper, null);
        if (translated != null && !translated.isEmpty()) {
            return ColorUtil.colorize(translated);
        }
        return upper;
    }

    public String getTimeUnit(String unitKey) {
        String translated = getRaw("time-units." + unitKey, unitKey);
        return ColorUtil.colorize(translated);
    }

    public boolean isTitleEnabled() {
        return langConfig != null ? langConfig.getBoolean("title.enabled", true) : true;
    }

    public String getTitle() {
        return getMessage("title.title");
    }

    public String getSubtitle(int time) {
        return getMessage("title.subtitle", "%time%", String.valueOf(time));
    }

    public int getTitleFadeIn() {
        return langConfig != null ? langConfig.getInt("title.fade-in", 10) : 10;
    }

    public int getTitleStay() {
        return langConfig != null ? langConfig.getInt("title.stay", 25) : 25;
    }

    public int getTitleFadeOut() {
        return langConfig != null ? langConfig.getInt("title.fade-out", 10) : 10;
    }

    public boolean isActionBarEnabled() {
        return langConfig != null ? langConfig.getBoolean("actionbar.enabled", true) : true;
    }

    public String getActionBarMessage(int time) {
        return getMessage("actionbar.message", "%time%", String.valueOf(time));
    }

    public String getCountdownMessage(int time) {
        return getMessage("messages.chat-countdown", "%time%", String.valueOf(time));
    }

    public String getSavingMessage() {
        return getMessage("messages.saving");
    }

    public String getSavedMessage() {
        return getMessage("messages.saved");
    }

    public String getKickLobbyMessage() {
        return getMessage("messages.kick-lobby");
    }

    public String getKickShutdownMessage() {
        return getMessage("messages.kick-shutdown");
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
