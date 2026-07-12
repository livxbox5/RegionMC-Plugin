package org.KillerYT.regionMC.listeners;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class EnderDragonBlockBreakListener implements Listener {

    private final RegionManager regionManager;
    private final RegionMC plugin;

    public EnderDragonBlockBreakListener() {
        this.plugin = RegionMC.getInstance();
        this.regionManager = plugin.getRegionManager();
    }

    @EventHandler
    public void onEnderDragonBlockBreak(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof EnderDragon)) {
            return;
        }

        Region region = regionManager.getRegionAtLocation(event.getBlock().getLocation());
        if (region == null) {
            return;
        }

        boolean allowDamage = regionManager.getFlagBooleanValue(region, "ender-dragon-damage", true);

        if (!allowDamage) {
            event.setCancelled(true);
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("EnderDragon block break prevented in region " + region.getName());
            }
        }
    }
}