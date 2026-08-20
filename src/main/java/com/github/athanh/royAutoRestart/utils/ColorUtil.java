package com.github.athanh.royAutoRestart.utils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for color processing in Minecraft messages.
 * Supports:
 * - Legacy color & formatting codes (&0-&f, &k-&o, &r, §0-§f...)
 * - Hex colors (&#RRGGBB, &#rrggbb, #RRGGBB, &x&r&r&g&g&b&b, §x§r§r§g§g§b§b)
 * - Multi-stop gradients: <gradient:#color1:#color2:...>text</gradient> or <gradient:#aaaaaa:#bbbbbb:#cccccc:#N>text</gradient>
 * - Rainbow text: <rainbow>text</rainbow>
 */
public final class ColorUtil {

    private static final char COLOR_CHAR = '§';

    // Regex patterns
    private static final Pattern AMPERSAND_HEX_PATTERN = Pattern.compile("(?i)[&§]#([0-9a-fA-F]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("(?i)<gradient:([^>]+)>([\\s\\S]*?)</gradient>");
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("(?i)<rainbow(?:\\:([^>]+))?>([\\s\\S]*?)</rainbow>");

    private ColorUtil() {
        // Private constructor
    }

    /**
     * Colorize a string with full support for legacy codes, hex codes, gradients, and rainbow.
     *
     * @param text Text to colorize
     * @return Formatted legacy string containing § color codes
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }

        // 1. Process rainbow tags first
        text = processRainbow(text);

        // 2. Process gradient tags
        text = processGradients(text);

        // 3. Process &#RRGGBB and §#RRGGBB hex patterns
        text = processHexColors(text);

        // 4. Process standard legacy & color codes
        text = translateLegacyColorCodes(text);

        return text;
    }

    /**
     * Colorize a list of strings.
     *
     * @param lines List of strings
     * @return Colorized list of strings
     */
    public static List<String> colorizeList(List<String> lines) {
        if (lines == null) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(colorize(line));
        }
        return result;
    }

    /**
     * Strip all color codes and format tags from text.
     *
     * @param text Input text
     * @return Plain text without formatting
     */
    public static String stripColor(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // Remove gradient and rainbow tags if any
        String stripped = text.replaceAll("(?i)<gradient:[^>]+>|</gradient>|<rainbow(?:\\:[^>]+)?>|</rainbow>", "");
        // Remove &#RRGGBB hex codes
        stripped = stripped.replaceAll("(?i)[&§]#[0-9a-fA-F]{6}", "");
        // Remove §x spigot hex codes
        stripped = stripped.replaceAll("(?i)§x(§[0-9a-fA-F]){6}", "");
        // Remove standard § and & codes
        stripped = stripped.replaceAll("(?i)[&§][0-9a-fk-or]", "");
        return stripped;
    }

    /**
     * Process <gradient:...>content</gradient> tags.
     */
    private static String processGradients(String text) {
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String colorsPart = matcher.group(1);
            String content = matcher.group(2);

            if (content == null || content.isEmpty()) {
                matcher.appendReplacement(sb, "");
                continue;
            }

            // Extract hex color stops
            String[] tokens = colorsPart.split(":");
            List<Color> colorStops = new ArrayList<>();
            for (String token : tokens) {
                token = token.trim();
                if (token.startsWith("#") && (token.length() == 7 || token.length() == 4)) {
                    try {
                        colorStops.add(parseHexColor(token));
                    } catch (Exception ignored) {}
                }
            }

            if (colorStops.isEmpty()) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(content));
            } else if (colorStops.size() == 1) {
                String hex = toSpigotHex(colorStops.get(0));
                matcher.appendReplacement(sb, Matcher.quoteReplacement(hex + content));
            } else {
                String gradientResult = applyGradient(content, colorStops);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(gradientResult));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Color parseHexColor(String hex) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() == 3) {
            // Expand #RGB to #RRGGBB
            char r = hex.charAt(0);
            char g = hex.charAt(1);
            char b = hex.charAt(2);
            hex = "" + r + r + g + g + b + b;
        }
        int rgb = Integer.parseInt(hex, 16);
        return new Color(rgb);
    }

    /**
     * Interpolate colors across a string, preserving inner formatting codes (&l, &o, &n, &m, &k).
     */
    private static String applyGradient(String text, List<Color> colors) {
        StringBuilder result = new StringBuilder();
        int visibleLength = getVisibleLength(text);
        if (visibleLength == 0) {
            return text;
        }

        int totalSegments = colors.size() - 1;
        int charIndex = 0;
        String activeFormats = "";

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Check if this is a format code (e.g. &l, §o, &r...)
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ("0123456789abcdef".indexOf(code) != -1) {
                    activeFormats = "";
                    i++;
                    continue;
                } else if ("klmno".indexOf(code) != -1) {
                    activeFormats += "§" + code;
                    i++;
                    continue;
                } else if (code == 'r') {
                    activeFormats = "";
                    i++;
                    continue;
                }
            }

            // Calculate interpolation factor (0.0 to 1.0)
            float ratio = visibleLength == 1 ? 0f : (float) charIndex / (visibleLength - 1);
            float scaledRatio = ratio * totalSegments;
            int segment = Math.min((int) scaledRatio, totalSegments - 1);
            float localRatio = scaledRatio - segment;

            Color c1 = colors.get(segment);
            Color c2 = colors.get(segment + 1);
            Color interpolated = interpolateColor(c1, c2, localRatio);

            result.append(toSpigotHex(interpolated));
            if (!activeFormats.isEmpty()) {
                result.append(activeFormats);
            }
            result.append(c);

            charIndex++;
        }

        return result.toString();
    }

    /**
     * Process <rainbow>content</rainbow> tags.
     */
    private static String processRainbow(String text) {
        Matcher matcher = RAINBOW_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String content = matcher.group(2);
            if (content == null || content.isEmpty()) {
                matcher.appendReplacement(sb, "");
                continue;
            }

            int visibleLength = getVisibleLength(content);
            if (visibleLength == 0) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(content));
                continue;
            }

            StringBuilder rainbowResult = new StringBuilder();
            int charIndex = 0;
            String activeFormats = "";

            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);

                if ((c == '&' || c == '§') && i + 1 < content.length()) {
                    char code = Character.toLowerCase(content.charAt(i + 1));
                    if ("klmno".indexOf(code) != -1) {
                        activeFormats += "§" + code;
                        i++;
                        continue;
                    } else if (code == 'r') {
                        activeFormats = "";
                        i++;
                        continue;
                    }
                }

                float hue = (float) charIndex / Math.max(1, visibleLength);
                Color color = Color.getHSBColor(hue, 0.85f, 1.0f);

                rainbowResult.append(toSpigotHex(color));
                if (!activeFormats.isEmpty()) {
                    rainbowResult.append(activeFormats);
                }
                rainbowResult.append(c);

                charIndex++;
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(rainbowResult.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Replace &#RRGGBB or §#RRGGBB with §x§r§r§g§g§b§b format (lowercase).
     */
    private static String processHexColors(String text) {
        Matcher matcher = AMPERSAND_HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1).toLowerCase();
            StringBuilder spigotHex = new StringBuilder("§x");
            for (char ch : hex.toCharArray()) {
                spigotHex.append('§').append(ch);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(spigotHex.toString()));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Translate alternate color codes '&' to '§'.
     */
    private static String translateLegacyColorCodes(String text) {
        char[] b = text.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(b[i + 1]) > -1) {
                b[i] = COLOR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    /**
     * Convert java.awt.Color to §x§r§r§g§g§b§b string (standard Spigot format).
     */
    public static String toSpigotHex(Color color) {
        String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()).toLowerCase();
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }

    /**
     * Linearly interpolate between two colors.
     */
    private static Color interpolateColor(Color c1, Color c2, float factor) {
        factor = Math.max(0f, Math.min(1f, factor));
        int r = (int) (c1.getRed() + factor * (c2.getRed() - c1.getRed()));
        int g = (int) (c1.getGreen() + factor * (c2.getGreen() - c1.getGreen()));
        int b = (int) (c1.getBlue() + factor * (c2.getBlue() - c1.getBlue()));
        return new Color(r, g, b);
    }

    /**
     * Count visible characters in text (ignoring & and § format codes).
     */
    private static int getVisibleLength(String text) {
        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if ("0123456789abcdefklmnorx".indexOf(code) != -1) {
                    i++;
                    continue;
                }
            }
            length++;
        }
        return length;
    }
}
