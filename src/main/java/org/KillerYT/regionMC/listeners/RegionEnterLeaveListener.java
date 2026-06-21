package org.KillerYT.regionMC.listeners;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class RegionEnterLeaveListener implements Listener {
    private final RegionMC plugin;
    private final RegionManager regionManager;

    public RegionEnterLeaveListener(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        Region fromRegion = regionManager.getRegionAtLocation(event.getFrom());
        Region toRegion = regionManager.getRegionAtLocation(event.getTo());

        if (fromRegion != toRegion) {
            if (fromRegion != null && plugin.getPlayerTimeManager() != null) {
                plugin.getPlayerTimeManager().handlePlayerLeaveRegion(event.getPlayer(), fromRegion);
            }

            if (toRegion != null && plugin.getPlayerTimeManager() != null) {
                plugin.getPlayerTimeManager().handlePlayerEnterRegion(event.getPlayer(), toRegion);
            }
        }
    }
}