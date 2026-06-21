package org.KillerYT.regionMC.managers;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Objects;   // ← ДОБАВИТЬ

public class PlayerMoveListener implements Listener {
    private final RegionMC plugin;
    private final RegionManager regionManager;

    public PlayerMoveListener(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ())
            return;

        String fromRegion = regionManager.findRegionAtLocation(from);
        String toRegion = regionManager.findRegionAtLocation(to);
        // Обновление времени при смене региона
        if (!Objects.equals(fromRegion, toRegion)) {
            if (toRegion != null) {
                Region region = regionManager.getRegion(toRegion);
                if (region != null && region.getFlag("time-lock") != null) {
                    plugin.getPlayerTimeManager().updatePlayerTime(p);
                }
            }
        }
    }
}