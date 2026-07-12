package org.KillerYT.regionMC.managers;

import org.KillerYT.regionMC.RegionMC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class SettingsManager {
    private final RegionMC plugin;
    private FileConfiguration config;
    private File configFile;

    public SettingsManager(RegionMC plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        // Создаем config.yml если не существует
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            plugin.getLogger().info("Created default config.yml");
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("✓ SettingsManager loaded successfully");
    }

    public FileConfiguration getConfig() {
        if (config == null) loadConfig();
        return config;
    }

    public FileConfiguration getMainConfig() {
        return getConfig();
    }

    public void saveConfig() {
        if (config == null || configFile == null) return;
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        if (configFile != null && configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("✓ Config reloaded");
        }
    }

    public boolean isGlobalRegionEnabled() {
        return getConfig().getBoolean("global-region.enabled", true);
    }

    public int getGlobalRegionPriority() {
        return getConfig().getInt("global-region.priority", -1000);
    }

    public boolean isWorldEnabled(String worldName) {
        return getConfig().getBoolean("worlds." + worldName + ".enabled", true);
    }

    // Метод для получения значения из конфига
    public String getString(String path, String defaultValue) {
        return getConfig().getString(path, defaultValue);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return getConfig().getBoolean(path, defaultValue);
    }

    public int getInt(String path, int defaultValue) {
        return getConfig().getInt(path, defaultValue);
    }
}