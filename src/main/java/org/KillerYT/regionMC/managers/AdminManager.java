package org.KillerYT.regionMC.managers;

import org.KillerYT.regionMC.RegionMC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.KillerYT.regionMC.utils.DebugUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AdminManager {

    private final RegionMC plugin;
    private final List<String> adminUsers = new ArrayList<>();
    private final DebugUtils debug = DebugUtils.getInstance();

    public AdminManager(RegionMC plugin) {
        this.plugin = plugin;
        loadAdminUsers();
    }

    private void loadAdminUsers() {
        File mainFile = new File(plugin.getDataFolder(), "Settings/main.yml");
        debug.debug(DebugUtils.DebugCategory.ADMIN, "Поиск main.yml: " + mainFile.getAbsolutePath());

        if (!mainFile.exists()) {
            debug.debug(DebugUtils.DebugCategory.ADMIN, "main.yml не найден, создаём новый...");
            mainFile.getParentFile().mkdirs();
            try {
                YamlConfiguration config = new YamlConfiguration();
                List<String> defaultAdmins = new ArrayList<>();
                defaultAdmins.add("KillerYTuber1");
                config.set("admin-user", defaultAdmins);
                config.save(mainFile);
                debug.info("Создан Settings/main.yml с администратором KillerYTuber1");
            } catch (IOException e) {
                debug.error("Не удалось создать main.yml", e);
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(mainFile);

        // ==================== ДИАГНОСТИКА ====================
        debug.info("Путь к main.yml: " + mainFile.getAbsolutePath());
        debug.info("Файл существует: " + mainFile.exists());
        debug.info("Размер файла: " + mainFile.length());
        debug.info("Все ключи в конфиге: " + config.getKeys(false));
        // ====================================================

        List<?> list = null;

        if (config.contains("admin-user")) {
            list = config.getList("admin-user");
            debug.debug(DebugUtils.DebugCategory.ADMIN, "Найден admin-user на верхнем уровне");
        } else if (config.contains("regions.admin-user")) {
            list = config.getList("regions.admin-user");
            debug.debug(DebugUtils.DebugCategory.ADMIN, "Найден admin-user внутри regions");
        }

        if (list != null) {
            adminUsers.clear();
            for (Object obj : list) {
                if (obj instanceof String) {
                    adminUsers.add((String) obj);
                }
            }
            debug.info("Загружено администраторов: " + adminUsers.size() + " из " + mainFile.getPath());
            debug.debug(DebugUtils.DebugCategory.ADMIN, "Список администраторов: " + String.join(", ", adminUsers));
        } else {
            debug.warning("admin-user не найден в main.yml");
        }
    }

    public boolean isAdmin(Player player) {
        if (player == null) return false;
        boolean result = adminUsers.contains(player.getName());
        debug.debug(DebugUtils.DebugCategory.ADMIN, "Проверка администратора: " + player.getName() + " -> " + result);
        return result;
    }

    public void reload() {
        loadAdminUsers();
        debug.info("Список администраторов перезагружен");
    }
}