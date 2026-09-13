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

public class EntryFlag extends ListenerFlag<Boolean> {

    public EntryFlag(RegionMC plugin) {
        super(plugin, "entry", true);
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
        return "Вход в регион";
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
        if (toRegion == null) return;

        if (!toRegion.equals(fromRegion)) {
            if (shouldBypassRegionProtection(p, toRegion)) return;

            // Проверка права на вход
            if (!regionManager.canEnterRegion(p.getUniqueId(), toRegion)) {
                event.setCancelled(true);

                Region region = regionManager.getRegion(toRegion);
                String customMsg = null;
                if (region != null) {
                    Object raw = region.getFlag("entry-deny-message");
                    if (raw instanceof String) {
                        customMsg = (String) raw;
                    }
                }
                if (customMsg != null && !customMsg.trim().isEmpty()) {
                    p.sendMessage(ColorUtil.colorize(customMsg));
                } else {
                    p.sendMessage("§cВы не можете войти в этот регион!");
                }
                return;
            }

            // Сообщение самому игроку
            if (plugin.getConfig().getBoolean("regions.messages.show-entry-messages", true)) {
                p.sendMessage("§aВы вошли в регион §e" + toRegion);
            }

            // Уведомление других (send by RegionEnterLeaveListener — единая точка)
        }
    }
}