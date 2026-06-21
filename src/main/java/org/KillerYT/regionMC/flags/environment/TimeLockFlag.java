package org.KillerYT.regionMC.flags.environment;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TimeLockFlag extends ListenerFlag<String> {
    public TimeLockFlag(RegionMC plugin) {
        super(plugin, "time-lock", "allow"); // allow/deny/day/night
    }

    @Override
    public String parseInput(String input) {
        if (input == null || input.isEmpty()) return getDefaultValue();
        String value = input.toLowerCase().trim();
        if (value.equals("day") || value.equals("night") || value.equals("deny") || value.equals("allow"))
            return value;
        return getDefaultValue();
    }

    @Override
    public boolean isAllowed(String value) {
        return value != null && value.equals("allow");
    }

    @Override
    public String getDescription() {
        return "Блокировка времени в регионе (day/night/allow/deny)";
    }

    /**
     * Логика time-lock
     **/
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String cmd = event.getMessage().toLowerCase().trim();
        if (!cmd.startsWith("/time set") && !cmd.startsWith("/time add")) return;

        Location loc = p.getLocation();
        String regionName = regionManager.findRegionAtLocation(loc);
        if (regionName != null && !shouldBypassRegionProtection(p, regionName)) {
            org.KillerYT.regionMC.region.Region region = regionManager.getRegion(regionName);
            if (region != null) {
                Object flag = region.getFlag("time-lock");
                if (flag != null) {
                    String timeValue = flag.toString().toLowerCase();
                    if (!timeValue.equals("allow")) {
                        event.setCancelled(true);
                        sendMessage(p, "protection.time-change", "§cВремя не может быть изменено в этом регионе!");
                        if (plugin.getPlayerTimeManager() != null)
                            plugin.getPlayerTimeManager().updatePlayerTime(p);
                    }
                }
            }
        }
    }
}