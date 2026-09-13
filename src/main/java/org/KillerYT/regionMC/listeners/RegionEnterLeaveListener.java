package org.KillerYT.regionMC.listeners;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.ColorUtil;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.utils.NotifyGuard;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
        this.notificationsEnabled = plugin.getConfig()
                .getBoolean("settings.notifications.enabled", true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // 1) Отсекаем микродвижения — игрок остался в том же блоке
        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()
                && from.getWorld().equals(to.getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        Region fromRegion = regionManager.getRegionAtLocation(from);
        Region toRegion = regionManager.getRegionAtLocation(to);

        // 2) Регион не сменился
        if (sameRegion(fromRegion, toRegion)) return;

        // ---- ВЫХОД из старого региона ----
        if (fromRegion != null
                && NotifyGuard.shouldSend(player.getUniqueId(), fromRegion.getName(), "exit")) {

            // time-lock: сброс персонального времени
            if (plugin.getPlayerTimeManager() != null) {
                plugin.getPlayerTimeManager().handlePlayerLeaveRegion(player, fromRegion);
            }

            // уведомление владельцам/участникам
            if (notificationsEnabled && canNotifyExit(player, fromRegion)) {
                sendNotification(player, fromRegion, "exit");
            }
        }

        // ---- ВХОД в новый регион ----
        if (toRegion != null
                && NotifyGuard.shouldSend(player.getUniqueId(), toRegion.getName(), "enter")) {

            // time-lock
            if (plugin.getPlayerTimeManager() != null) {
                plugin.getPlayerTimeManager().handlePlayerEnterRegion(player, toRegion);
            }

            // уведомление владельцам/участникам
            if (notificationsEnabled && canNotifyEnter(player, toRegion)) {
                sendNotification(player, toRegion, "enter");
            }
        }
    }

    private boolean sameRegion(Region a, Region b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getName().equalsIgnoreCase(b.getName());
    }

    private boolean canNotifyEnter(Player player, Region region) {
        if (player.hasPermission("regionmc.bypass.notify")) return false;
        if (!regionManager.canEnterRegion(player.getUniqueId(), region.getName())) return false;
        Boolean flag = region.getFlagBoolean("enter-notify");
        return flag != null && flag;
    }

    private boolean canNotifyExit(Player player, Region region) {
        if (player.hasPermission("regionmc.bypass.notify")) return false;
        if (!regionManager.canExitRegion(player.getUniqueId(), region.getName())) return false;
        Boolean flag = region.getFlagBoolean("exit-notify");
        return flag != null && flag;
    }

    private void sendNotification(Player player, Region region, String type) {
        String messageKey = "region.notify." + type;
        String message = plugin.getLanguageManager()
                .getMessage(messageKey, region.getName(), player.getName());

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