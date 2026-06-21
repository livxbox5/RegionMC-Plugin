package org.KillerYT.regionMC.flags.main;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public abstract class ListenerFlag<T> extends AbstractRegionFlag<T> implements Listener {

    protected final RegionMC plugin;
    protected final RegionManager regionManager;
    protected final boolean debug;

    public ListenerFlag(RegionMC plugin, String name, T defaultValue) {
        super(name, defaultValue);
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.debug = plugin.isDebugEnabled();
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Проверяет, запрещён ли флаг в локации
     */
    protected boolean isFlagDenied(Location loc, String flag) {
        for (Region region : regionManager.getRegionsAtLocation(loc)) {
            Object value = region.getFlag(flag);
            if (value != null) return !getFlagBoolean(region, flag);
        }
        return false;
    }

    /**
     * Проверяет, запрещено ли действие для игрока в локации
     */
    protected boolean isActionDenied(Player player, Location loc, String flag) {
        String regionName = regionManager.findRegionAtLocation(loc);
        if (regionName == null || shouldBypassRegionProtection(player, regionName)) return false;
        if (regionManager.isOwner(player, regionName) || regionManager.isMember(player, regionName)) return false;
        Region region = regionManager.getRegion(regionName);
        return region != null && !getFlagBoolean(region, flag);
    }

    /**
     * Получает булево значение флага из региона
     * С ПОДДЕРЖКОЙ deny/allow
     */
    protected boolean getFlagBoolean(Region region, String flag) {
        Object value = region.getFlag(flag);
        if (value == null) return true;
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) {
            String l = s.toLowerCase().trim();
            // Разрешающие значения
            if (l.equals("true") || l.equals("allow") || l.equals("yes") || l.equals("1") ||
                    l.equals("разрешить") || l.equals("enable") || l.equals("enabled") || l.equals("on")) {
                return true;
            }
            // Запрещающие значения
            if (l.equals("false") || l.equals("deny") || l.equals("no") || l.equals("0") ||
                    l.equals("запретить") || l.equals("disable") || l.equals("disabled") || l.equals("off")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверяет байпас защиты региона
     */
    protected boolean shouldBypassRegionProtection(Player player, String regionName) {
        if (player.isOp() || player.hasPermission("*") || player.hasPermission("regionmc.admin")) return true;
        if (regionName == null) return false;
        String normalized = regionName.toLowerCase();
        return player.hasPermission("regionmc.bypass." + normalized)
                || player.hasPermission("regionmc.bypass.all")
                || player.hasPermission("regionmc.bypass.*");
    }

    /**
     * Отправляет сообщение игроку (с поддержкой замены параметров)
     */
    protected void sendMessage(Player player, String key, String... replacements) {
        if (player == null) return;
        try {
            String message = plugin.getLanguageManager().getMessage(key, replacements);
            if (message != null && !message.isEmpty() && !message.contains("Message not found")) {
                player.sendMessage(message);
            } else {
                // Если ключ не найден, пробуем отправить fallback
                if (replacements.length > 0) {
                    player.sendMessage(replacements[0]);
                }
            }
        } catch (Exception e) {
            if (replacements.length > 0) {
                player.sendMessage(replacements[0]);
            }
            if (debug) plugin.getLogger().warning("Failed to send message for key: " + key);
        }
    }

    /**
     * Отправляет сообщение игроку (упрощённая версия)
     */
    protected void sendMessage(Player player, String key) {
        sendMessage(player, key, new String[0]);
    }

    /**
     * Отправляет сообщение игроку с одним параметром
     */
    protected void sendMessage(Player player, String key, String param) {
        sendMessage(player, key, new String[]{param});
    }

    /**
     * Логирование в дебаг-режиме
     */
    protected void debugLog(String msg) {
        if (debug) plugin.getLogger().info(msg);
    }
}