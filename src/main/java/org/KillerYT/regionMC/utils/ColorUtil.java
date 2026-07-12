package org.KillerYT.regionMC.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.Map;

public class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(TagResolver.standard())
                    .build())
            .build();

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    // Паттерн для градиентных HEX цветов в формате {#ЦВЕТ>}текст{<#ЦВЕТ}
    private static final Pattern CUSTOM_GRADIENT_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})>\\}(.*?)\\{<#([A-Fa-f0-9]{6})\\}");

    // Паттерн для простых HEX цветов #RRGGBB
    private static final Pattern SIMPLE_HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    // Паттерн для MiniMessage градиентов
    private static final Pattern MINI_GRADIENT_PATTERN = Pattern.compile("<gradient:([^>]+)>([^<]+)</gradient>");

    // Паттерн для MiniMessage HEX цветов
    private static final Pattern MINI_HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>([^<]+)</#\\1>");

    // Паттерн для MiniMessage простых тегов
    private static final Pattern MINI_TAG_PATTERN = Pattern.compile("<([a-z_]+)>([^<]+)</\\1>");

    /**
     * Преобразует все цветовые коды в сообщении
     * Поддерживает:
     * 1. Стандартные цвета Minecraft (&a, &b, &c, &l, &o и т.д.)
     * 2. HEX цвета #RRGGBB
     * 3. Градиенты {#FF0000>}текст{<#00FF00}
     * 4. MiniMessage форматы (<red>текст</red>, <gradient:red:blue>текст</gradient>)
     */
    public static String colorize(String message) {
        if (message == null) return null;

        // 1. Сначала обрабатываем MiniMessage (конвертируем в legacy)
        message = processMiniMessage(message);

        // 2. Обрабатываем кастомные градиенты {#ЦВЕТ>}текст{<#ЦВЕТ}
        message = processCustomGradients(message);

        // 3. Обрабатываем простые HEX цвета #RRGGBB
        message = processSimpleHex(message);

        // 4. Наконец, обрабатываем стандартные цвета Minecraft
        message = message.replace('&', '§');

        return message;
    }

    /**
     * Конвертирует сообщение в Component (для Adventure API)
     */
    public static Component toComponent(String message) {
        if (message == null) return Component.empty();

        // Пробуем распарсить как MiniMessage
        try {
            return MINI_MESSAGE.deserialize(message);
        } catch (Exception e) {
            // Если не получилось, конвертируем legacy в Component
            return LEGACY_SERIALIZER.deserialize(colorize(message));
        }
    }

    /**
     * Обрабатывает MiniMessage форматы и конвертирует в legacy
     */
    private static String processMiniMessage(String message) {
        String result = message;

        // Конвертируем MiniMessage градиенты
        Matcher gradientMatcher = MINI_GRADIENT_PATTERN.matcher(result);
        StringBuffer gradientBuffer = new StringBuffer();
        while (gradientMatcher.find()) {
            String colors = gradientMatcher.group(1);
            String text = gradientMatcher.group(2);

            String[] colorArray = colors.split(":");
            if (colorArray.length >= 2) {
                String startHex = extractHexFromColorName(colorArray[0]);
                String endHex = extractHexFromColorName(colorArray[colorArray.length - 1]);
                String gradientText = createGradient(text, startHex, endHex);
                gradientMatcher.appendReplacement(gradientBuffer, Matcher.quoteReplacement(gradientText));
            } else {
                gradientMatcher.appendReplacement(gradientBuffer, Matcher.quoteReplacement(text));
            }
        }
        gradientMatcher.appendTail(gradientBuffer);
        result = gradientBuffer.toString();

        // Конвертируем MiniMessage HEX
        Matcher hexMatcher = MINI_HEX_PATTERN.matcher(result);
        StringBuffer hexBuffer = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            String text = hexMatcher.group(2);
            String coloredText = applyHexColor(text, hex);
            hexMatcher.appendReplacement(hexBuffer, Matcher.quoteReplacement(coloredText));
        }
        hexMatcher.appendTail(hexBuffer);
        result = hexBuffer.toString();

        // Конвертируем MiniMessage простые теги
        Map<String, String> tagMap = new HashMap<>();
        tagMap.put("red", "§c");
        tagMap.put("dark_red", "§4");
        tagMap.put("green", "§a");
        tagMap.put("dark_green", "§2");
        tagMap.put("blue", "§9");
        tagMap.put("dark_blue", "§1");
        tagMap.put("aqua", "§b");
        tagMap.put("dark_aqua", "§3");
        tagMap.put("yellow", "§e");
        tagMap.put("gold", "§6");
        tagMap.put("white", "§f");
        tagMap.put("gray", "§7");
        tagMap.put("dark_gray", "§8");
        tagMap.put("black", "§0");
        tagMap.put("light_purple", "§d");
        tagMap.put("dark_purple", "§5");
        tagMap.put("bold", "§l");
        tagMap.put("italic", "§o");
        tagMap.put("underlined", "§n");
        tagMap.put("strikethrough", "§m");
        tagMap.put("obfuscated", "§k");
        tagMap.put("reset", "§r");

        for (Map.Entry<String, String> entry : tagMap.entrySet()) {
            result = result.replaceAll("<" + entry.getKey() + ">", entry.getValue())
                    .replaceAll("</" + entry.getKey() + ">", "");
        }

        return result;
    }

    /**
     * Извлекает HEX из названия цвета MiniMessage
     */
    private static String extractHexFromColorName(String colorName) {
        if (colorName.startsWith("#")) {
            return colorName.substring(1);
        }

        Map<String, String> colorHexMap = new HashMap<>();
        colorHexMap.put("red", "FF5555");
        colorHexMap.put("dark_red", "AA0000");
        colorHexMap.put("green", "55FF55");
        colorHexMap.put("dark_green", "00AA00");
        colorHexMap.put("blue", "5555FF");
        colorHexMap.put("dark_blue", "0000AA");
        colorHexMap.put("aqua", "55FFFF");
        colorHexMap.put("dark_aqua", "00AAAA");
        colorHexMap.put("yellow", "FFFF55");
        colorHexMap.put("gold", "FFAA00");
        colorHexMap.put("white", "FFFFFF");
        colorHexMap.put("gray", "AAAAAA");
        colorHexMap.put("dark_gray", "555555");
        colorHexMap.put("black", "000000");
        colorHexMap.put("light_purple", "FF55FF");
        colorHexMap.put("dark_purple", "AA00AA");

        return colorHexMap.getOrDefault(colorName, "FFD700");
    }

    /**
     * Применяет HEX цвет к тексту
     */
    private static String applyHexColor(String text, String hex) {
        StringBuilder result = new StringBuilder();
        result.append("§x");
        for (char c : hex.toCharArray()) {
            result.append("§").append(c);
        }
        result.append(text);
        return result.toString();
    }

    /**
     * Обрабатывает кастомные градиенты в формате {#ЦВЕТ>}текст{<#ЦВЕТ}
     */
    private static String processCustomGradients(String message) {
        Matcher matcher = CUSTOM_GRADIENT_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String startHex = matcher.group(1);
            String text = matcher.group(2);
            String endHex = matcher.group(3);

            String gradientText = createGradient(text, startHex, endHex);
            matcher.appendReplacement(result, Matcher.quoteReplacement(gradientText));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Обрабатывает простые HEX цвета #RRGGBB
     */
    private static String processSimpleHex(String message) {
        Matcher matcher = SIMPLE_HEX_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            String minecraftHex = convertHexToMinecraft(hex);
            matcher.appendReplacement(result, Matcher.quoteReplacement(minecraftHex));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Конвертирует HEX в Minecraft формат §x§R§R§G§G§B§B
     */
    private static String convertHexToMinecraft(String hex) {
        StringBuilder result = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            result.append("§").append(c);
        }
        return result.toString();
    }

    /**
     * Создает градиентный текст
     */
    private static String createGradient(String text, String startHex, String endHex) {
        if (text.isEmpty()) return "";

        Color startColor = Color.decode("#" + startHex);
        Color endColor = Color.decode("#" + endHex);

        StringBuilder gradient = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) Math.max(1, length - 1);
            Color currentColor = interpolateColor(startColor, endColor, ratio);
            String hexColor = String.format("%06X", currentColor.getRGB() & 0xFFFFFF);

            gradient.append("§x");
            for (char c : hexColor.toCharArray()) {
                gradient.append("§").append(c);
            }
            gradient.append(text.charAt(i));
        }

        return gradient.toString();
    }

    /**
     * Интерполирует цвет между начальным и конечным
     */
    private static Color interpolateColor(Color start, Color end, float ratio) {
        int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
        int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
        int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));

        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return new Color(red, green, blue);
    }

    /**
     * Отправляет цветное сообщение игроку/консоли
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender != null && message != null) {
            sender.sendMessage(colorize(message));
        }
    }

    /**
     * Отправляет цветное сообщение с префиксом
     */
    public static void sendMessage(CommandSender sender, String prefix, String message) {
        if (sender != null) {
            sender.sendMessage(colorize(prefix + " " + message));
        }
    }

    // ========== СТАТИЧЕСКИЕ КОНСТАНТЫ ==========

    public static final String BLACK = "§0";
    public static final String DARK_BLUE = "§1";
    public static final String DARK_GREEN = "§2";
    public static final String DARK_AQUA = "§3";
    public static final String DARK_RED = "§4";
    public static final String DARK_PURPLE = "§5";
    public static final String GOLD = "§6";
    public static final String GRAY = "§7";
    public static final String DARK_GRAY = "§8";
    public static final String BLUE = "§9";
    public static final String GREEN = "§a";
    public static final String AQUA = "§b";
    public static final String RED = "§c";
    public static final String LIGHT_PURPLE = "§d";
    public static final String YELLOW = "§e";
    public static final String WHITE = "§f";

    public static final String OBFUSCATED = "§k";
    public static final String BOLD = "§l";
    public static final String STRIKETHROUGH = "§m";
    public static final String UNDERLINE = "§n";
    public static final String ITALIC = "§o";
    public static final String RESET = "§r";

    // ========== УТИЛИТНЫЕ МЕТОДЫ ==========

    public static String black(String text) { return colorize(BLACK + text); }
    public static String darkBlue(String text) { return colorize(DARK_BLUE + text); }
    public static String darkGreen(String text) { return colorize(DARK_GREEN + text); }
    public static String darkAqua(String text) { return colorize(DARK_AQUA + text); }
    public static String darkRed(String text) { return colorize(DARK_RED + text); }
    public static String darkPurple(String text) { return colorize(DARK_PURPLE + text); }
    public static String gold(String text) { return colorize(GOLD + text); }
    public static String gray(String text) { return colorize(GRAY + text); }
    public static String darkGray(String text) { return colorize(DARK_GRAY + text); }
    public static String blue(String text) { return colorize(BLUE + text); }
    public static String green(String text) { return colorize(GREEN + text); }
    public static String aqua(String text) { return colorize(AQUA + text); }
    public static String red(String text) { return colorize(RED + text); }
    public static String lightPurple(String text) { return colorize(LIGHT_PURPLE + text); }
    public static String yellow(String text) { return colorize(YELLOW + text); }
    public static String white(String text) { return colorize(WHITE + text); }

    public static String bold(String text) { return colorize(BOLD + text); }
    public static String italic(String text) { return colorize(ITALIC + text); }
    public static String underline(String text) { return colorize(UNDERLINE + text); }
    public static String strikethrough(String text) { return colorize(STRIKETHROUGH + text); }
    public static String obfuscated(String text) { return colorize(OBFUSCATED + text); }

    public static String rainbow(String text) {
        String[] rainbowColors = {"§c", "§6", "§e", "§a", "§9", "§5", "§d"};
        StringBuilder result = new StringBuilder();
        int colorIndex = 0;
        for (char c : text.toCharArray()) {
            if (c == ' ') {
                result.append(c);
                continue;
            }
            result.append(rainbowColors[colorIndex % rainbowColors.length]).append(c);
            colorIndex++;
        }
        return result.toString();
    }

    public static String gradient(String text, String startColor, String endColor) {
        return colorize("{#" + startColor + ">}" + text + "{<#" + endColor + "}");
    }

    public static TextColor hexToTextColor(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        try {
            return TextColor.color(Integer.parseInt(hex, 16));
        } catch (NumberFormatException e) {
            return TextColor.color(0xFFFFFF);
        }
    }

    public static Component createHexComponent(String text, String hex) {
        return Component.text(text).color(hexToTextColor(hex));
    }
}