package org.KillerYT.regionMC.managers;

import org.KillerYT.regionMC.RegionMC;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimeLockManager {
    private final RegionMC plugin;
    private final Map<String, Long> fixedWorldTimes = new ConcurrentHashMap<>();
    private final Map<String, String> worldTimeLockModes = new ConcurrentHashMap<>();
    private final boolean debug;

    public TimeLockManager(RegionMC plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("time-lock.debug", false);
        loadFixedTimes();
        startTimeLockChecker();
    }

    private void loadFixedTimes() {
        if (debug) plugin.getLogger().info("TimeLockManager загружен");
    }

    private void startTimeLockChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                String worldName = world.getName();
                if (!fixedWorldTimes.containsKey(worldName)) continue;

                long fixedTime = fixedWorldTimes.get(worldName);
                String mode = worldTimeLockModes.getOrDefault(worldName, "deny");

                long targetTime = switch (mode) {
                    case "day" -> 6000L;
                    case "night" -> 18000L;
                    default -> fixedTime;
                };

                if (world.getTime() != targetTime) {
                    world.setTime(targetTime);
                    if (debug) plugin.getLogger().info("Исправлено время в мире " + worldName);
                }
            }
        }, 20L, 20L);
    }

    public void updateFixedTime(String worldName, long time) {
        fixedWorldTimes.put(worldName, time);
        worldTimeLockModes.put(worldName, "deny");
        if (debug) plugin.getLogger().info("Установлено фиксированное время " + time + " для мира " + worldName);
    }

    public void setTimeLockMode(String worldName, String mode, Long time) {
        worldTimeLockModes.put(worldName, mode);
        if (time != null) fixedWorldTimes.put(worldName, time);
        if (debug) plugin.getLogger().info("Установлен режим time-lock " + mode + " для мира " + worldName);
    }

    public Long getFixedTime(String worldName) {
        return fixedWorldTimes.get(worldName);
    }

    public String getTimeLockMode(String worldName) {
        return worldTimeLockModes.getOrDefault(worldName, "allow");
    }

    public void removeFixedTime(String worldName) {
        fixedWorldTimes.remove(worldName);
        worldTimeLockModes.remove(worldName);
        if (debug) plugin.getLogger().info("Удалено фиксированное время для мира " + worldName);
    }

    public boolean isTimeFixed(String worldName) {
        return fixedWorldTimes.containsKey(worldName);
    }

    public boolean isTimeChangeAllowed(String worldName) {
        String mode = getTimeLockMode(worldName);
        return "allow".equals(mode) || !isTimeFixed(worldName);
    }

    public void saveFixedTimes() {
        if (debug) plugin.getLogger().info("Сохранено " + fixedWorldTimes.size() + " фиксированных времен");
    }

    public void clearAllFixedTimes() {
        fixedWorldTimes.clear();
        worldTimeLockModes.clear();
        if (debug) plugin.getLogger().info("Все фиксированные времена очищены");
    }
}