package org.KillerYT.regionMC.flags.mobs;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class EndermanGriefFlag extends ListenerFlag<Boolean> {

    public EndermanGriefFlag(RegionMC plugin) {
        super(plugin, "enderman-grief", true);
    }

    @Override
    public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override
    public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override
    public String getDescription() { return "Гриферство эндерменов"; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEndermanPickup(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof Enderman)) return;
        if (isFlagDenied(event.getBlock().getLocation(), getName())) event.setCancelled(true);
    }
}