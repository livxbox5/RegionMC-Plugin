package org.KillerYT.regionMC.Placeholders;

import org.KillerYT.regionMC.RegionMC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
/**
 * Упрощенная утилита для работы с PlaceholderAPI.
 * Расположена в пакете Placeholders для удобства.
 */
public class PlaceholderAPIUtils {

    private static RegionMC plugin;
    private static boolean placeholderApiEnabled = false;

    /**
     * Инициализация утилиты.
     */
    public static void initialize(RegionMC plugin) {
        PlaceholderAPIUtils.plugin = plugin;
        placeholderApiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        if (placeholderApiEnabled) {
            plugin.getLogger().info("✓ PlaceholderAPI доступен");
        } else {
            plugin.getLogger().info("✗ PlaceholderAPI недоступен");
        }
    }

    /**
     * Проверка, доступен ли PlaceholderAPI.
     */
    public static boolean isPlaceholderApiEnabled() {
        return placeholderApiEnabled;
    }

    /**
     * Обработка строки с плейсхолдерами.
     */
    public static String setPlaceholders(Player player, String text) {
        if (!placeholderApiEnabled || text == null) {
            return text;
        }

        try {
            // Используем рефлексию для избежания ошибок компиляции
            Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            java.lang.reflect.Method method = papiClass.getMethod("setPlaceholders", Player.class, String.class);
            return (String) method.invoke(null, player, text);
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при обработке плейсхолдеров: " + e.getMessage());
            return text;
        }
    }

    /**
     * Отмена регистрации плейсхолдеров (пустой метод, т.к. не регистрируем свои).
     */
    public static void unregisterPlaceholders() {
        // Ничего не делаем, т.к. не регистрировали свои плейсхолдеры
    }
}