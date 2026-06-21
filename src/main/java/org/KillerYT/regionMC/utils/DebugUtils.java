package org.KillerYT.regionMC.utils;

import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.logging.Logger;

/**
 * Утилита для красивого вывода отладочной информации.
 * Поддерживает ANSI-цвета в консоли, категории и уровни детализации.
 * Использование:
 *   DebugUtils.getInstance().debug("Сообщение");
 *   DebugUtils.getInstance().info("Информация");
 *   DebugUtils.getInstance().warning("Предупреждение");
 *   DebugUtils.getInstance().error("Ошибка");
 */
public class DebugUtils {

    // ANSI escape codes (работают в современных терминалах и IDE)
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_PURPLE = "\u001B[35m";

    private static DebugUtils instance;
    private Logger logger;
    private String pluginName = "RegionMCBlock";

    // Настройки отладки
    private boolean debugEnabled = false;
    private DebugLevel level = DebugLevel.BASIC;
    private final EnumSet<DebugCategory> enabledCategories = EnumSet.noneOf(DebugCategory.class);
    private boolean allCategoriesEnabled = false;

    // Таймер для замеров
    private long startTime;
    private String lastOperation;

    private DebugUtils() {}

    public static DebugUtils getInstance() {
        if (instance == null) {
            instance = new DebugUtils();
        }
        return instance;
    }

    /**
     * Инициализация из конфигурационного файла (config.yml).
     */
    public void initialize(Logger logger, FileConfiguration config) {
        this.logger = logger;

        if (config == null) {
            this.debugEnabled = false;
            this.level = DebugLevel.NONE;
            return;
        }

        this.debugEnabled = config.getBoolean("debug.enabled", false);
        String levelStr = config.getString("debug.level", "BASIC").toUpperCase();
        this.level = DebugLevel.fromString(levelStr);

        enabledCategories.clear();
        this.allCategoriesEnabled = config.getBoolean("debug.categories.ALL", false);

        if (!allCategoriesEnabled) {
            if (config.getBoolean("debug.categories.gui", false)) enabledCategories.add(DebugCategory.GUI);
            if (config.getBoolean("debug.categories.regions", false)) enabledCategories.add(DebugCategory.REGIONS);
            if (config.getBoolean("debug.categories.flags", false)) enabledCategories.add(DebugCategory.FLAGS);
            if (config.getBoolean("debug.categories.economy", false)) enabledCategories.add(DebugCategory.ECONOMY);
            if (config.getBoolean("debug.categories.commands", false)) enabledCategories.add(DebugCategory.COMMANDS);
            if (config.getBoolean("debug.categories.events", false)) enabledCategories.add(DebugCategory.EVENTS);
            if (config.getBoolean("debug.categories.performance", false)) enabledCategories.add(DebugCategory.PERFORMANCE);
            if (config.getBoolean("debug.categories.database", false)) enabledCategories.add(DebugCategory.DATABASE);
            // ДОБАВЛЕНО: отдельная категория для AdminManager
            if (config.getBoolean("debug.categories.adminmanager", false)) enabledCategories.add(DebugCategory.ADMIN);

            // НОВЫЕ КАТЕГОРИИ
            if (config.getBoolean("debug.categories.cfg", false)) enabledCategories.add(DebugCategory.CFG);
            if (config.getBoolean("debug.categories.generatorcfg", false)) enabledCategories.add(DebugCategory.GENERATOR_CFG);
            if (config.getBoolean("debug.categories.gcfg", false)) enabledCategories.add(DebugCategory.GCFG);
        }

        if (debugEnabled) {
            separator();
            info("§6" + pluginName + " - Режим отладки: §e" + level.getDisplayName());
            if (allCategoriesEnabled) {
                info("§7Активные категории: §aВСЕ");
            } else if (!enabledCategories.isEmpty()) {
                info("§7Активные категории: §e" + enabledCategories);
            }
            separator();
        }
    }

    public void initialize(Logger logger, boolean debugEnabled) {
        this.logger = logger;
        this.debugEnabled = debugEnabled;
        this.level = debugEnabled ? DebugLevel.BASIC : DebugLevel.NONE;
    }

    public void setDebugEnabled(boolean enabled) { this.debugEnabled = enabled; }
    public boolean isDebugEnabled() { return debugEnabled; }
    public void setLevel(DebugLevel level) { this.level = level; }
    public DebugLevel getLevel() { return level; }

    public void enableCategory(DebugCategory category) { enabledCategories.add(category); }
    public void disableCategory(DebugCategory category) { enabledCategories.remove(category); }
    public void enableAllCategories() { allCategoriesEnabled = true; enabledCategories.clear(); }
    public void disableAllCategories() { allCategoriesEnabled = false; enabledCategories.clear(); }

    public boolean isCategoryEnabled(DebugCategory category) {
        if (!debugEnabled) return false;
        if (allCategoriesEnabled) return true;
        if (enabledCategories.isEmpty()) return true;
        return enabledCategories.contains(category);
    }

    private boolean shouldLog(DebugLevel requiredLevel) {
        if (!debugEnabled) return false;
        if (level == DebugLevel.NONE) return false;
        if (level == DebugLevel.ALL) return true;
        if (level == DebugLevel.BASIC && requiredLevel == DebugLevel.BASIC) return true;
        if (level == DebugLevel.DETAILED && (requiredLevel == DebugLevel.BASIC || requiredLevel == DebugLevel.DETAILED)) return true;
        return level == DebugLevel.ALL;
    }

    private boolean shouldLogCategory(DebugCategory category, DebugLevel requiredLevel) {
        if (!shouldLog(requiredLevel)) return false;
        if (allCategoriesEnabled) return true;
        if (enabledCategories.isEmpty()) return true;
        return enabledCategories.contains(category);
    }

    private void log(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    public void debug(String message) {
        if (shouldLog(DebugLevel.BASIC)) {
            log(ANSI_CYAN + "[DEBUG] " + message + ANSI_RESET);
        }
    }

    public void debug(DebugCategory category, String message) {
        if (shouldLogCategory(category, DebugLevel.BASIC)) {
            log(ANSI_CYAN + "[DEBUG:" + category.getPrefix() + "] " + message + ANSI_RESET);
        }
    }

    public void debugDetailed(String message) {
        if (shouldLog(DebugLevel.DETAILED)) {
            log(ANSI_PURPLE + "[DEBUG] " + message + ANSI_RESET);
        }
    }

    public void debugDetailed(DebugCategory category, String message) {
        if (shouldLogCategory(category, DebugLevel.DETAILED)) {
            log(ANSI_PURPLE + "[DEBUG:" + category.getPrefix() + "] " + message + ANSI_RESET);
        }
    }

    public void debugParams(String operation, Object... params) {
        if (shouldLog(DebugLevel.ALL)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.length; i += 2) {
                if (i > 0) sb.append(", ");
                sb.append(params[i]).append("=").append(params[i + 1]);
            }
            log(ANSI_PURPLE + "[DEBUG] " + operation + "(" + sb + ")" + ANSI_RESET);
        }
    }

    public void debugParams(DebugCategory category, String operation, Object... params) {
        if (shouldLogCategory(category, DebugLevel.ALL)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.length; i += 2) {
                if (i > 0) sb.append(", ");
                sb.append(params[i]).append("=").append(params[i + 1]);
            }
            log(ANSI_PURPLE + "[DEBUG:" + category.getPrefix() + "] " + operation + "(" + sb + ")" + ANSI_RESET);
        }
    }

    public void info(String message) {
        if (logger != null) {
            logger.info(ANSI_GREEN + "[INFO] " + message + ANSI_RESET);
        }
    }

    public void info(DebugCategory category, String message) {
        if (logger != null) {
            logger.info(ANSI_GREEN + "[INFO:" + category.getPrefix() + "] " + message + ANSI_RESET);
        }
    }

    public void warning(String message) {
        if (logger != null) {
            logger.warning(ANSI_YELLOW + "[WARN] " + message + ANSI_RESET);
        }
    }

    public void warning(DebugCategory category, String message) {
        if (logger != null) {
            logger.warning(ANSI_YELLOW + "[WARN:" + category.getPrefix() + "] " + message + ANSI_RESET);
        }
    }

    public void error(String message) {
        if (logger != null) {
            logger.severe(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
        }
    }

    public void error(String message, Exception e) {
        if (logger != null) {
            logger.severe(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
            if (debugEnabled && (level == DebugLevel.DETAILED || level == DebugLevel.ALL)) {
                e.printStackTrace();
            }
        }
    }

    public void debugToPlayer(Player player, String message) {
        if (debugEnabled && player != null && shouldLog(DebugLevel.BASIC)) {
            player.sendMessage("§7[§bDEBUG§7] §f" + message);
        }
    }

    public void infoToPlayer(Player player, String message) {
        if (player != null) {
            player.sendMessage("§7[§aINFO§7] §f" + message);
        }
    }

    public void warningToPlayer(Player player, String message) {
        if (player != null) {
            player.sendMessage("§7[§6WARN§7] §e" + message);
        }
    }

    public void errorToPlayer(Player player, String message) {
        if (player != null) {
            player.sendMessage("§7[§cERROR§7] §c" + message);
        }
    }

    public void startTimer(String operation) {
        if (shouldLogCategory(DebugCategory.PERFORMANCE, DebugLevel.DETAILED)) {
            lastOperation = operation;
            startTime = System.currentTimeMillis();
            debugDetailed(DebugCategory.PERFORMANCE, "▶ Начало: " + operation);
        }
    }

    public void endTimer() {
        if (shouldLogCategory(DebugCategory.PERFORMANCE, DebugLevel.DETAILED) && lastOperation != null) {
            long duration = System.currentTimeMillis() - startTime;
            debugDetailed(DebugCategory.PERFORMANCE, "◀ Завершено: " + lastOperation + " §7(§e" + duration + "ms§7)");
            lastOperation = null;
        }
    }

    public void endTimer(String operation) {
        if (shouldLogCategory(DebugCategory.PERFORMANCE, DebugLevel.DETAILED)) {
            long duration = System.currentTimeMillis() - startTime;
            debugDetailed(DebugCategory.PERFORMANCE, "◀ " + operation + " §7(§e" + duration + "ms§7)");
            lastOperation = null;
        }
    }

    public void separator() {
        if (logger != null) {
            logger.info(ANSI_CYAN + "========================================" + ANSI_RESET);
        }
    }

    public void separator(String title) {
        if (logger != null) {
            logger.info(ANSI_CYAN + "========== §e" + title + " §7==========" + ANSI_RESET);
        }
    }

    public void logRegionStats(int totalRegions, int playerRegions, int loadedCount) {
        separator("Статистика регионов");
        info("Всего регионов: §e" + totalRegions);
        info("Регионов игроков: §e" + playerRegions);
        info("Загружено из файлов: §e" + loadedCount);
        separator();
    }

    public void logGuiRegistration(int count, java.util.Map<String, ?> menus) {
        if (debugEnabled) {
            separator("Регистрация GUI");
            info("Зарегистрировано GUI меню: §e" + count);
            if (level == DebugLevel.DETAILED || level == DebugLevel.ALL) {
                for (String name : menus.keySet()) {
                    debug("  §7- §f" + name);
                }
            }
            separator();
        }
    }

    public void printStartupBanner(String version) {
        separator();
        info("§6  " + pluginName + " v" + version + " §aзапущен!");
        info("§7  Режим отладки: " + (debugEnabled ? "§aВКЛЮЧЁН" : "§cВЫКЛЮЧЁН"));
        if (debugEnabled) {
            info("§7  Уровень: §e" + level.getDisplayName());
            if (allCategoriesEnabled) {
                info("§7  Категории: §aВСЕ");
            } else if (!enabledCategories.isEmpty()) {
                info("§7  Категории: §e" + enabledCategories);
            }
        }
        separator();
    }

    public void printShutdownBanner() {
        separator();
        info("§c  " + pluginName + " выключен.");
        separator();
    }

    // ==================== ENUM ====================

    public enum DebugLevel {
        NONE("NONE", "Только ошибки"),
        BASIC("BASIC", "Основные действия"),
        DETAILED("DETAILED", "Детальная отладка"),
        ALL("ALL", "Полная отладка");

        private final String key;
        private final String displayName;

        DebugLevel(String key, String displayName) {
            this.key = key;
            this.displayName = displayName;
        }

        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }

        public static DebugLevel fromString(String str) {
            for (DebugLevel level : values()) {
                if (level.key.equalsIgnoreCase(str)) {
                    return level;
                }
            }
            return BASIC;
        }
    }

    public enum DebugCategory {
        GUI("GUI", "gui"),
        REGIONS("RG", "regions"),
        FLAGS("FLG", "flags"),
        ECONOMY("ECO", "economy"),
        COMMANDS("CMD", "commands"),
        EVENTS("EVT", "events"),
        PERFORMANCE("PRF", "performance"),
        DATABASE("DB", "database"),
        ADMIN("ADMIN", "adminmanager"),
        // НОВЫЕ КАТЕГОРИИ
        CFG("CFG", "cfg"),
        GENERATOR_CFG("GenCFG", "generatorcfg"),
        GCFG("GCFG", "gcfg");

        private final String prefix;
        private final String configKey;

        DebugCategory(String prefix, String configKey) {
            this.prefix = prefix;
            this.configKey = configKey;
        }

        public String getPrefix() { return prefix; }
        public String getConfigKey() { return configKey; }
    }
}