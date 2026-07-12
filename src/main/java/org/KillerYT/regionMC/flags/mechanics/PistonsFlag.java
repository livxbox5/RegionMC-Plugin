package org.KillerYT.regionMC.flags.mechanics;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;

public class PistonsFlag extends ListenerFlag<Boolean> {
    public PistonsFlag(RegionMC plugin) { super(plugin, "pistons", true); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Работа поршней"; }

    /**
     *
     *  Логика pistons
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExtend(BlockPistonExtendEvent event) {
        if (isFlagDenied(event.getBlock().getLocation(), getName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRetract(BlockPistonRetractEvent event) {
        if (isFlagDenied(event.getBlock().getLocation(), getName())) event.setCancelled(true);
    }
}