package org.KillerYT.regionMC.managers;

import org.KillerYT.regionMC.region.Region;
import org.bukkit.entity.Player;

public class RegionNotifier {

    public void notifyRegionEnter(Player player, Region region) {
        if (region != null) {
            player.sendMessage("§aВход в регион: §e" + region.getName());
        }
    }

    public void notifyRegionLeave(Player player, Region region) {
        if (region != null) {
            player.sendMessage("§cВыход из региона: §e" + region.getName());
        }
    }
}