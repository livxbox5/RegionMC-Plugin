package org.KillerYT.regionMC.flags.chat;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@SuppressWarnings("deprecation") // подавление возможных устаревших вызовов в родительском классе
public class BlockChatFlag extends ListenerFlag<Boolean> {

    private static final String MESSAGE_BLOCKED = "§c[RegionMC] Чат заблокирован в этом регионе!";

    public BlockChatFlag(RegionMC plugin) {
        super(plugin, "block-chat", true);
    }

    @Override
    public Boolean parseInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return getDefaultValue();
        }
        String lower = input.toLowerCase().trim();
        return switch (lower) {
            case "deny", "false", "no", "0", "запретить" -> true;
            case "allow", "true", "yes", "1", "разрешить" -> false;
            default -> getDefaultValue();
        };
    }

    @Override
    public boolean isAllowed(Boolean value) {
        return value == null ? !getDefaultValue() : !value;
    }

    @Override
    public String getDescription() {
        return "Блокировка чата в регионе";
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // ✅ Современная проверка обхода – без deprecated
        if (player.hasPermission("regionmc.bypass.chat") || regionManager.isAdminBypass(player)) {
            return;
        }

        String regionName = regionManager.findRegionAtLocation(player.getLocation());
        if (regionName == null) return;

        Region region = regionManager.getRegion(regionName);
        if (region == null) return;

        if (region.isOwner(player.getUniqueId()) || region.isMember(player.getUniqueId())) {
            return;
        }

        Boolean blocked = getFlagValue(region);
        if (blocked == null) {
            blocked = getDefaultValue();
        }

        if (blocked) {
            event.setCancelled(true);
            player.sendMessage(MESSAGE_BLOCKED);
        }
    }

    private Boolean getFlagValue(Region region) {
        Object raw = region.getFlag(getName());
        if (raw == null) return null;
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof String) return parseInput((String) raw);
        return getDefaultValue();
    }
}