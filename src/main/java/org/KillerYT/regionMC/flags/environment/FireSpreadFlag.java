package org.KillerYT.regionMC.flags.environment;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;

public class FireSpreadFlag extends ListenerFlag<Boolean> {
    public FireSpreadFlag(RegionMC plugin) { super(plugin, "fire-spread", true); }

    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Распространение огня"; }

    /**
     *
     *  Логика fire-spread (загорание блока)
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (isFlagDenied(event.getBlock().getLocation(), getName())) {
            event.setCancelled(true);
        }
    }

    /**
     *
     *  Логика fire-spread (сгорание блока)
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (isFlagDenied(event.getBlock().getLocation(), getName())) {
            event.setCancelled(true);
        }
    }
}