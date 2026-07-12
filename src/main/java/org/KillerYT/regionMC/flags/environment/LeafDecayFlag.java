package org.KillerYT.regionMC.flags.environment;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.LeavesDecayEvent;

public class LeafDecayFlag extends ListenerFlag<Boolean> {
    public LeafDecayFlag(RegionMC plugin) { super(plugin, "leaf-decay", true); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }

    @Override public String getDescription() { return "Опадание листьев"; }

    /**
     *
     *  Логика leaf-decay
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent event) {
        if (isFlagDenied(event.getBlock().getLocation(), getName())) event.setCancelled(true);
    }
}