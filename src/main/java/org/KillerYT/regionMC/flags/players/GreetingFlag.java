package org.KillerYT.regionMC.flags.players;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.utils.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

public class GreetingFlag extends ListenerFlag<String> {

    public GreetingFlag(RegionMC plugin) {
        super(plugin, "greeting", null);
    }

    @Override
    public String parseInput(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        // Удаляем кавычки, если они есть
        String trimmed = input.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.equalsIgnoreCase("allow") || trimmed.equalsIgnoreCase("deny") ||
                trimmed.equalsIgnoreCase("default") || trimmed.equalsIgnoreCase("remove")) {
            return null; // сброс
        }
        return trimmed;
    }

    @Override
    public boolean isAllowed(String value) {
        return true; // этот флаг не влияет на разрешение/запрет
    }

    @Override
    public String getDescription() {
        return "Приветственное сообщение при входе в регион";
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getTo() == null) return;

        String fromRegion = regionManager.findRegionAtLocation(event.getFrom());
        String toRegion = regionManager.findRegionAtLocation(event.getTo());

        // Если вошли в регион (или сменили регион)
        if (toRegion != null && !toRegion.equals(fromRegion)) {
            Region region = regionManager.getRegion(toRegion);
            if (region == null) return;
            Object greeting = region.getFlag("greeting");
            if (greeting instanceof String) {
                String msg = (String) greeting;
                if (msg != null && !msg.trim().isEmpty()) {
                    player.sendMessage(ColorUtil.colorize(msg));
                }
            }
        }
    }
}