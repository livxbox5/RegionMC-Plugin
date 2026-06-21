package org.KillerYT.regionMC.utils;

import org.KillerYT.regionMC.RegionMC;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdaterUtils {
    private final RegionMC plugin;
    private final FileConfiguration config;
    private final LanguageManager languageManager;

    // Простые проверки обновлений без внешних библиотек
    private static final String VERSION_CHECK_URL = "https://api.github.com/repos/KillerYT/RegionMC/releases/latest";

    private String latestVersion;
    private boolean updateAvailable;

    public UpdaterUtils(RegionMC plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.languageManager = plugin.getLanguageManager();
    }

    /**
     * Простая проверка обновлений через GitHub API
     */
    public void checkForUpdates() {
        if (!config.getBoolean("settings.updates.check-for-updates", true)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getLogger().info("Checking for updates...");

                try {
                    String response = makeSimpleHttpRequest(VERSION_CHECK_URL);
                    if (response != null) {
                        // Простой парсинг JSON без Jackson
                        String version = extractValue(response, "tag_name");
                        if (version != null) {
                            latestVersion = version.replace("v", "").trim();
                            updateAvailable = isNewerVersion(plugin.getDescription().getVersion(), latestVersion);

                            if (updateAvailable) {
                                plugin.getLogger().info("✓ Update available: " + latestVersion);
                                notifyAdmins();
                            } else {
                                plugin.getLogger().info("✓ Plugin is up to date");
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Update check failed: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Простой HTTP запрос
     */
    private String makeSimpleHttpRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "RegionMC-Plugin");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return response.toString();
            }
        } catch (Exception e) {
            // Игнорируем ошибки при проверке обновлений
        }
        return null;
    }

    /**
     * Извлечение значения из простого JSON
     */
    private String extractValue(String json, String key) {
        try {
            Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // Игнорируем ошибки парсинга
        }
        return null;
    }

    /**
     * Сравнение версий
     */
    private boolean isNewerVersion(String currentVersion, String newVersion) {
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] newParts = newVersion.split("\\.");

            int length = Math.max(currentParts.length, newParts.length);
            for (int i = 0; i < length; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int newPart = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;

                if (newPart > currentPart) return true;
                if (newPart < currentPart) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Уведомление администраторов
     */
    private void notifyAdmins() {
        if (!config.getBoolean("settings.updates.notify-admins", true)) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                String message = "§6[RegionMC] §eUpdate available! §a" +
                        plugin.getDescription().getVersion() +
                        " §7→ §a" + latestVersion +
                        "\n§eDownload: §bhttps://github.com/KillerYT/RegionMC/releases";

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("regionmc.admin") || player.isOp()) {
                        player.sendMessage(message);
                    }
                }

                plugin.getLogger().info("Update available: " + latestVersion);
            }
        }.runTask(plugin);
    }

    /**
     * Периодическая проверка обновлений
     */
    public void startUpdateScheduler() {
        if (!config.getBoolean("settings.updates.check-for-updates", true)) return;

        int intervalHours = config.getInt("settings.updates.check-interval", 24);
        long intervalTicks = intervalHours * 60 * 60 * 20;

        new BukkitRunnable() {
            @Override
            public void run() {
                checkForUpdates();
            }
        }.runTaskTimer(plugin, 60 * 20L, intervalTicks);
    }
}