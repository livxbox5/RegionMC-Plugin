package org.KillerYT.regionMC.flags.environment;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockGrowEvent;

public class GrassGrowthFlag extends ListenerFlag<Boolean> {
    public GrassGrowthFlag(RegionMC plugin) { super(plugin, "grass-growth", true); }

    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Рост травы"; }

    /**
     *
     *  Логика grass-growth
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        Material type = event.getBlock().getType();
        boolean isGrass = switch (type) {
            case GRASS_BLOCK, MYCELIUM -> true;
            default -> type.name().contains("GRASS");
        };
        if (isGrass && isFlagDenied(event.getBlock().getLocation(), getName())) event.setCancelled(true);
    }
}