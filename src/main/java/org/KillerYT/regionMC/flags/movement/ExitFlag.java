package org.KillerYT.regionMC.flags.movement;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.KillerYT.regionMC.utils.ColorUtil;

import java.util.UUID;

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
                notifyOthers(p, region, "exit");
                return;
            }

            // Проверка флага exit
            if (!getFlagBoolean(region, "exit")) {
                event.setCancelled(true);
                p.sendMessage("§cВы не можете покинуть этот регион!");
            } else {
                if (plugin.getConfig().getBoolean("regions.messages.show-exit-messages", true)) {
                    p.sendMessage("§cВы вышли из региона §e" + fromRegion);
                }
                notifyOthers(p, region, "exit");
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