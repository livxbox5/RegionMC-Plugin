package org.KillerYT.regionMC.flags.players;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.region.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

public class FarewellFlag extends ListenerFlag<String> {

    public FarewellFlag(RegionMC plugin) {
        super(plugin, "farewell", null);
    }

    @Override
    public String parseInput(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String trimmed = input.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.equalsIgnoreCase("allow") || trimmed.equalsIgnoreCase("deny") ||
                trimmed.equalsIgnoreCase("default") || trimmed.equalsIgnoreCase("remove")) {
            return null;
        }
        return trimmed;
    }

    @Override
    public boolean isAllowed(String value) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Прощальное сообщение при выходе из региона";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo() == null) return;

        String fromRegion = regionManager.findRegionAtLocation(event.getFrom());
        String toRegion = regionManager.findRegionAtLocation(event.getTo());

        // Если вышли из региона (или сменили регион)
        if (fromRegion != null && !fromRegion.equals(toRegion)) {
            Region region = regionManager.getRegion(fromRegion);
            if (region == null) return;
            Object farewell = region.getFlag("farewell");
            if (farewell instanceof String) {
                String msg = (String) farewell;
                if (msg != null && !msg.trim().isEmpty()) {
                    player.sendMessage(ColorUtil.colorize(msg));
                }
            }
        }
    }
}