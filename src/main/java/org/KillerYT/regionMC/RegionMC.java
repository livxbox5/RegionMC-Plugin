package org.KillerYT.regionMC;

import org.KillerYT.regionMC.commands.RegionCommand;
import org.KillerYT.regionMC.commands.subcommands.RegionWandCommand;
import org.KillerYT.regionMC.flags.main.FlagManager;
import org.KillerYT.regionMC.listeners.*;
import org.KillerYT.regionMC.managers.*;
import org.KillerYT.regionMC.utils.ConfigGenerator;
import org.killeryt.killerCoreAPI.utils.DebugUtils;
import org.KillerYT.regionMC.utils.LanguageManager;
import org.KillerYT.regionMC.utils.RegionEnterLeaveListener;
import org.bukkit.plugin.java.JavaPlugin;
import lombok.Getter;

import java.io.File;

@Getter
@SuppressWarnings("unused")
public class RegionMC extends JavaPlugin {

    private RegionManager regionManager;
    private RegionCommand regionCommand;
    private RegionWandCommand wandCommand;
    private FlagManager flagManager;
    private TimeLockManager timeLockManager;
    private PlayerTimeManager playerTimeManager;
    private LanguageManager languageManager;
    private SettingsManager settingsManager;
    private ConfigGenerator configGenerator;
    private AdminManager adminManager;          // геттер будет сгенерирован
    private RentManager rentManager;            // геттер будет сгенерирован

    @Override
    public void onEnable() {
        // статический геттер будет сгенерирован
        RegionMC instance = this;

        DebugUtils.getInstance().initialize(getLogger(), getConfig());
        this.configGenerator = new ConfigGenerator(this);
        checkAndRestoreMissingFiles();

        this.settingsManager = new SettingsManager(this);
        this.languageManager = new LanguageManager(this);
        this.regionManager = new RegionManager(this);
        this.rentManager = new RentManager(this);
        this.timeLockManager = new TimeLockManager(this);
        this.playerTimeManager = new PlayerTimeManager(this);
        adminManager = new AdminManager(this);

        FlagManager.initialize(this);
        this.flagManager = FlagManager.getInstance();

        this.wandCommand = new RegionWandCommand(this);

        getServer().getPluginManager().registerEvents(new RegionListener(regionManager, this), this);
        getServer().getPluginManager().registerEvents(new EnderDragonBlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this, wandCommand), this);
        getServer().getPluginManager().registerEvents(new InfoListener(this), this);
        getServer().getPluginManager().registerEvents(new RegionEnterLeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new RentSignListener(this), this);

        this.regionCommand = new RegionCommand(this);
        this.regionCommand.registerAllCommands();

        checkConfigFolders();

        getLogger().info("RegionMC успешно загружен!");
        getLogger().info("WandListener и InfoListener зарегистрированы!");
    }

    @Override
    public void onDisable() {
        if (regionManager != null) {
            regionManager.saveRegions();
        }
        getLogger().info("RegionMC выгружен!");
    }

    private void checkConfigFolders() {
        File settingsFolder = new File(getDataFolder(), "Settings");
        if (!settingsFolder.exists()) {
            boolean created = settingsFolder.mkdirs();
            if (created) {
                getLogger().info("Created Settings folder");
            }
        }

        File limitFile = new File(settingsFolder, "limit.yml");
        if (limitFile.exists()) {
            getLogger().info("limit.yml found in Settings folder");
        } else {
            getLogger().info("limit.yml not found, will be created if needed");
        }

        File mainFile = new File(settingsFolder, "main.yml");
        if (mainFile.exists()) {
            getLogger().info("main.yml found in Settings folder");
        } else {
            getLogger().info("main.yml not found, will be created if needed");
        }
    }

    public void reloadManagers() {
        getLogger().info("Перезагрузка менеджеров RegionMC...");

        reloadConfig();
        checkAndRestoreMissingFiles();

        if (settingsManager != null) {
            settingsManager.reloadConfig();
        }
        if (languageManager != null) {
            languageManager.reloadLanguage();
        }
        if (regionManager != null) {
            regionManager.reload();
        }
        if (playerTimeManager != null) {
            playerTimeManager.reload();
        }
        if (adminManager != null) {
            adminManager.reload();
        }

        getLogger().info("Все менеджеры перезагружены");
    }

    private void checkAndRestoreMissingFiles() {
        File ruLang = new File(getDataFolder(), "lang/ru.yml");
        File enLang = new File(getDataFolder(), "lang/en.yml");
        File settingsFolder = new File(getDataFolder(), "Settings");
        File mainConfig = new File(settingsFolder, "main.yml");
        File limitConfig = new File(settingsFolder, "limit.yml");

        boolean needRestore = false;

        if (!ruLang.exists()) {
            getLogger().info("Обнаружен отсутствующий файл: lang/ru.yml");
            needRestore = true;
        }
        if (!enLang.exists()) {
            getLogger().info("Обнаружен отсутствующий файл: lang/en.yml");
            needRestore = true;
        }
        if (!mainConfig.exists()) {
            getLogger().info("Обнаружен отсутствующий файл: Settings/main.yml");
            needRestore = true;
        }
        if (!limitConfig.exists()) {
            getLogger().info("Обнаружен отсутствующий файл: Settings/limit.yml");
            needRestore = true;
        }

        if (needRestore) {
            getLogger().info("Восстанавливаем отсутствующие файлы...");
            if (configGenerator != null) {
                configGenerator.generateAllConfigs();
            }
        }
    }

    public boolean isDebugEnabled() {
        return getConfig().getBoolean("debug.enabled", false);
    }
}