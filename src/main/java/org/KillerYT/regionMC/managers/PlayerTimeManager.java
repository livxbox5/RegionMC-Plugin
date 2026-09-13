package org.KillerYT.regionMC.managers;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTimeManager {
    private final RegionMC plugin;
    private final RegionManager regionManager;
    private final Map<UUID, Long> playerPersonalTimes = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTimeLockModes = new ConcurrentHashMap<>();
    private final boolean debug;

    public PlayerTimeManager(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.debug = plugin.isDebugEnabled();
        startPlayerTimeUpdater();
    }

    private void startPlayerTimeUpdater() {
        Bukkit.getScheduler().runTaskTimer(plugin,
                () -> Bukkit.getOnlinePlayers().forEach(this::updatePlayerTime),
                20L, 20L);
    }

    public void updatePlayerTime(Player player) {
        Region region = regionManager.getRegionAtLocation(player.getLocation());
        UUID playerId = player.getUniqueId();

        if (region != null) {
            String timeLockValue = region.getFlagValue("time-lock", String.class);
            if (isTimeLocked(timeLockValue)) {
                applyTimeLock(player, playerId, timeLockValue);
                return;
            }
        }

        if (playerTimeLockModes.containsKey(playerId)) {
            resetPlayerTime(player);
        }
    }

    private void applyTimeLock(Player player, UUID playerId, String mode) {
        Long fixedTime = getFixedTime(mode);

        if ("deny".equals(mode)) {
            if (!playerTimeLockModes.getOrDefault(playerId, "").equals("deny")) {
                playerPersonalTimes.put(playerId, player.getWorld().getTime());
                playerTimeLockModes.put(playerId, "deny");
                debugLog("Фиксируем время для игрока " + player.getName());
            }
            player.setPlayerTime(playerPersonalTimes.get(playerId), false);
        } else if (fixedTime != null) {
            if (!mode.equals(playerTimeLockModes.get(playerId))) {
                playerTimeLockModes.put(playerId, mode);
                playerPersonalTimes.put(playerId, fixedTime);
                debugLog("Устанавливаем " + mode + " время для игрока " + player.getName());
            }
            player.setPlayerTime(fixedTime, false);
        }
    }

    private boolean isTimeLocked(String value) {
        return value != null && (value.equals("deny") || value.equals("day") || value.equals("night"));
    }

    private Long getFixedTime(String value) {
        return switch (value) {
            case "day" -> 6000L;
            case "night" -> 18000L;
            default -> null;
        };
    }

    public void resetPlayerTime(Player player) {
        UUID playerId = player.getUniqueId();
        player.resetPlayerTime();
        playerPersonalTimes.remove(playerId);
        playerTimeLockModes.remove(playerId);
        debugLog("Сброшено персональное время для игрока " + player.getName());
    }

    public void handleRegionTimeLockChange(Region region) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> region.contains(p.getLocation()))
                .forEach(this::updatePlayerTime);
    }

    // =====================================================
    // Вход в регион — time-lock
    // Защита от повторов: в RegionEnterLeaveListener
    // =====================================================
    public void handlePlayerEnterRegion(Player player, Region region) {
        if (player == null || region == null) return;
        updatePlayerTime(player);
    }

    // =====================================================
    // Выход из региона — time-lock
    // Защита от повторов: в RegionEnterLeaveListener
    // =====================================================
    public void handlePlayerLeaveRegion(Player player, Region region) {
        if (player == null || region == null) return;

        String timeLockValue = region.getFlagValue("time-lock", String.class);
        if (isTimeLocked(timeLockValue)) {
            resetPlayerTime(player);
        }
    }

    public void reload() {
        playerPersonalTimes.clear();
        playerTimeLockModes.clear();
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info("PlayerTimeManager reloaded");
        }
    }

    private void debugLog(String message) {
        if (debug) plugin.getLogger().info(message);
    }

    public Long getPlayerPersonalTime(UUID playerId) {
        return playerPersonalTimes.get(playerId);
    }

    public String getPlayerTimeLockMode(UUID playerId) {
        return playerTimeLockModes.get(playerId);
    }
}