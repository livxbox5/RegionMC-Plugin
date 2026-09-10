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

import java.util.UUID;

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

                // ===== КАСТОМНОЕ СООБЩЕНИЕ (entry-deny-message) =====
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

            // Уведомление другим (если флаг enter-notify = allow)
            Region region = regionManager.getRegion(toRegion);
            if (region != null) {
                notifyOthers(p, region, "enter");
            }
        }
    }

    private void notifyOthers(Player player, Region region, String type) {
        String flagName = type.equals("enter") ? "enter-notify" : "exit-notify";
        Boolean flagValue = region.getFlagBoolean(flagName);
        if (flagValue == null || !flagValue) return;

        String messageKey = "region.notify." + type;
        String message = plugin.getLanguageManager().getMessage(messageKey, region.getName(), player.getName());

        for (UUID uuid : region.getOwners()) {
            Player target = plugin.getServer().getPlayer(uuid);
            if (target != null && !target.equals(player)) {
                target.sendMessage(ColorUtil.colorize(message));
            }
        }
        for (UUID uuid : region.getMembers()) {
            Player target = plugin.getServer().getPlayer(uuid);
            if (target != null && !target.equals(player)) {
                target.sendMessage(ColorUtil.colorize(message));
            }
        }
    }
}