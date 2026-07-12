package org.KillerYT.regionMC.utils;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class RegionEnterLeaveListener implements Listener {
    private final RegionMC plugin;
    private final RegionManager regionManager;
    private final boolean notificationsEnabled;

    public RegionEnterLeaveListener(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.notificationsEnabled = plugin.getConfig().getBoolean("settings.notifications.enabled", true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Если уведомления отключены глобально – ничего не делаем
        if (!notificationsEnabled) return;

        Player player = event.getPlayer();
        Region fromRegion = regionManager.getRegionAtLocation(event.getFrom());
        Region toRegion = regionManager.getRegionAtLocation(event.getTo());

        if (fromRegion != toRegion) {
            // Выход из региона
            if (fromRegion != null && canNotifyExit(player, fromRegion)) {
                sendNotification(player, fromRegion, "exit");
            }
            // Вход в регион
            if (toRegion != null && canNotifyEnter(player, toRegion)) {
                sendNotification(player, toRegion, "enter");
            }
        }
    }

    private boolean canNotifyEnter(Player player, Region region) {
        // Если есть байпас – не уведомляем (админы и т.д.)
        if (player.hasPermission("regionmc.bypass.notify")) return false;
        // Проверяем, разрешён ли вход в регион (флаг entry)
        if (!regionManager.canEnterRegion(player.getUniqueId(), region.getName())) return false;
        // Проверяем флаг enter-notify
        Boolean flag = region.getFlagBoolean("enter-notify");
        return flag != null && flag;
    }

    private boolean canNotifyExit(Player player, Region region) {
        if (player.hasPermission("regionmc.bypass.notify")) return false;
        // Проверяем, разрешён ли выход (флаг exit)
        if (!regionManager.canExitRegion(player.getUniqueId(), region.getName())) return false;
        Boolean flag = region.getFlagBoolean("exit-notify");
        return flag != null && flag;
    }

    private void sendNotification(Player player, Region region, String type) {
        String messageKey = "region.notify." + type;
        String message = plugin.getLanguageManager().getMessage(messageKey, region.getName(), player.getName());

        // Отправляем всем владельцам и участникам (кроме самого игрока)
        for (UUID uuid : region.getOwners()) {
            Player target = plugin.getServer().getPlayer(uuid);
            if (target != null && !target.equals(player)) {
                target.sendMessage(ColorUtil.colorize(message));
            }
        }
        for (UUID uuid : region.getMembers()) {
            Player target = plugin.getServer().getPlayer(uuid);
            if (target != null && !target.equals(player)) {
                target.sendMessage(ColorUtil.colorize(message));
            }
        }
    }
}