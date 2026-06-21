package org.KillerYT.regionMC.utils;

import org.KillerYT.regionMC.RegionMC;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final RegionMC plugin;
    private FileConfiguration languageConfig;
    private String currentLanguage;
    private final Map<String, String> messageCache = new HashMap<>();

    public LanguageManager(RegionMC plugin) {
        this.plugin = plugin;
        this.currentLanguage = "ru";

        if (plugin.getSettingsManager() != null && plugin.getSettingsManager().getConfig() != null) {
            this.currentLanguage = plugin.getSettingsManager().getConfig().getString("settings.default-language", "ru");
        }

        loadLanguage();
    }

    private void loadLanguage() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create lang directory");
        }

        File languageFile = new File(langFolder, currentLanguage + ".yml");

        if (!languageFile.exists()) {
            if (!currentLanguage.equals("ru")) {
                plugin.getLogger().warning("Language file not found: " + currentLanguage + ".yml, using Russian as fallback");
                currentLanguage = "ru";
                languageFile = new File(langFolder, "ru.yml");
            }
            if (!languageFile.exists()) {
                createDefaultLanguageFile(languageFile);
            }
        }

        this.languageConfig = YamlConfiguration.loadConfiguration(languageFile);
        messageCache.clear();
        plugin.getLogger().info("Loaded language file: " + currentLanguage + ".yml");
    }

    private void createDefaultLanguageFile(File file) {
        languageConfig = new YamlConfiguration();

        languageConfig.set("prefix", "&6[RegionMC]&f");
        languageConfig.set("commands.no-permission", "&cУ вас нет прав на использование этой команды!");
        languageConfig.set("commands.usage", "&cИспользование: &f{usage}");
        languageConfig.set("commands.reload.start", "&eПерезагрузка RegionMC...");
        languageConfig.set("commands.reload.success", "&aRegionMC успешно перезагружен!");
        languageConfig.set("commands.reload.error", "&cОшибка при перезагрузке RegionMC!");

        languageConfig.set("region.create.invalid-name", "&cНеверное имя региона!");
        languageConfig.set("region.already-exists", "&cРегион &e{name} &cуже существует!");
        languageConfig.set("region.not-found", "&cРегион &e{name} &cне найден!");
        languageConfig.set("region.deleted", "&aРегион &e{name} &aуспешно удален!");
        languageConfig.set("region.different-worlds", "&cПозиции должны быть в одном мире!");

        languageConfig.set("region.pos1-set", "&aПозиция 1 установлена: &e{location}");
        languageConfig.set("region.pos2-set", "&aПозиция 2 установлена: &e{location}");

        languageConfig.set("protection.break", "&cВы не можете разрушать блоки в этом регионе!");
        languageConfig.set("protection.build", "&cВы не можете строить в этом регионе!");
        languageConfig.set("protection.interact", "&cВы не можете взаимодействовать с этим блоком!");
        languageConfig.set("protection.use", "&cВы не можете использовать предметы в этом регионе!");
        languageConfig.set("protection.pvp", "&cPvP отключен в этом регионе!");
        languageConfig.set("protection.tnt", "&cВы не можете ставить TNT в этом регионе!");

        languageConfig.set("selection-wand.no-permission", "&cУ вас нет прав на использование палочки выделения!");

        languageConfig.set("notifier.message-sent", "&aСообщение отправлено!");
        languageConfig.set("notifier.usage", "&cИспользование: /region notifier <сообщение>");
        languageConfig.set("notifier.test-started", "&eТест уведомлений запущен...");
        languageConfig.set("notifier.test-completed", "&aТест уведомлений завершен!");

        languageConfig.set("help.header", "&6=== RegionMC Помощь ===");
        languageConfig.set("help.notifier-test", "&7/region notifier test &f- Тест уведомлений");
        languageConfig.set("help.notifier-message", "&7/region notifier <message> &f- Отправить сообщение всем");

        try {
            languageConfig.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create default language file: " + e.getMessage());
        }
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, new String[0]);
    }

    public void sendMessage(CommandSender sender, String key, String... replacements) {
        if (sender == null) return;

        String message = getMessage(key, replacements);
        if (message.startsWith("&cMessage not found:")) {
            plugin.getLogger().warning("Missing language key: " + key);
            return;
        }

        sendFormattedMessage(sender, message);
    }

    private void sendFormattedMessage(CommandSender sender, String message) {
        if (message.contains("\n")) {
            String[] lines = message.split("\n");
            for (String line : lines) {
                sender.sendMessage(ColorUtil.colorize(line));
            }
        } else {
            sender.sendMessage(ColorUtil.colorize(message));
        }
    }

    public String getMessage(String key) {
        return getMessage(key, new String[0]);
    }

    public String getMessage(String key, String... replacements) {
        if (messageCache.containsKey(key)) {
            String message = messageCache.get(key);
            return applyReplacements(message, replacements);
        }

        String message = languageConfig.getString(key);
        if (message == null) {
            return "&cMessage not found: " + key;
        }

        if (!key.equals("prefix") && languageConfig.contains("prefix") && !message.contains("\n")) {
            String prefix = languageConfig.getString("prefix", "");
            message = prefix + message;
        }

        messageCache.put(key, message);
        return applyReplacements(message, replacements);
    }

    private String applyReplacements(String message, String... replacements) {
        String result = message;
        for (int i = 0; i < replacements.length; i++) {
            result = result.replace("{" + i + "}", replacements[i] != null ? replacements[i] : "");
            result = result.replace("%" + i + "%", replacements[i] != null ? replacements[i] : "");
        }
        return ColorUtil.colorize(result);
    }

    public void reloadLanguage() {
        messageCache.clear();
        loadLanguage();
        plugin.getLogger().info("✓ Language files reloaded");
    }

    public boolean hasMessage(String key) {
        return languageConfig.contains(key);
    }
}