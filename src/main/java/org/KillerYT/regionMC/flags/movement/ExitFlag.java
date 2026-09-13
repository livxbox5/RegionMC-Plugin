package org.KillerYT.regionMC.flags.movement;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.region.ColorUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

public class ExitFlag extends ListenerFlag<Boolean> {

    public ExitFlag(RegionMC plugin) {
        super(plugin, "exit", true);
    }

    @Override
    public Boolean parseInput(String input) {
        return convertToBoolean(input);
    }

    @Override
    public boolean isAllowed(Boolean value) {
        return value != null ? value : getDefaultValue();
    }

    @Override
    public String getDescription() {
        return "Выход из региона";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        String fromRegion = regionManager.findRegionAtLocation(from);
        String toRegion = regionManager.findRegionAtLocation(to);

        if (fromRegion != null && fromRegion.equals(toRegion)) return;
        if (fromRegion == null) return;

        if (!fromRegion.equals(toRegion)) {
            if (shouldBypassRegionProtection(p, fromRegion)) return;

            Region region = regionManager.getRegion(fromRegion);
            if (region == null) return;

            // Владельцы и участники всегда могут выйти
            if (region.isOwner(p.getUniqueId()) || region.isMember(p.getUniqueId())) {
                if (plugin.getConfig().getBoolean("regions.messages.show-exit-messages", true)) {
                    p.sendMessage("§cВы вышли из региона §e" + fromRegion);
                }
                return;
            }

            // Проверка флага exit
            if (!getFlagBoolean(region, "exit")) {
                event.setCancelled(true);

                String customMsg = null;
                Object raw = region.getFlag("exit-deny-message");
                if (raw instanceof String) {
                    customMsg = (String) raw;
                }
                if (customMsg != null && !customMsg.trim().isEmpty()) {
                    p.sendMessage(ColorUtil.colorize(customMsg));
                } else {
                    p.sendMessage("§cВы не можете покинуть этот регион!");
                }
            } else {
                if (plugin.getConfig().getBoolean("regions.messages.show-exit-messages", true)) {
                    p.sendMessage("§cВы вышли из региона §e" + fromRegion);
                }
            }
        }
    }
}