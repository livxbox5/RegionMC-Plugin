package org.KillerYT.regionMC.flags.protection;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

public class PlaceFlag extends ListenerFlag<Boolean> {
    public PlaceFlag(RegionMC plugin) { super(plugin, "place", false); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Установка блоков"; }

    /**
     *
     *  Логика place
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player p = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        String region = regionManager.findRegionAtLocation(loc);
        if (region == null || shouldBypassRegionProtection(p, region)) return;
        if (!regionManager.canPlaceInRegion(p.getUniqueId(), region)) {
            event.setCancelled(true);
            sendMessage(p, "protection.place", "§cВы не можете устанавливать блоки здесь!");
        }
    }
}