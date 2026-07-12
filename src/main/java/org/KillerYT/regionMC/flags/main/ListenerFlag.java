package org.KillerYT.regionMC.flags.main;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public abstract class ListenerFlag<T> extends AbstractRegionFlag<T> implements Listener {

    protected final RegionMC plugin;
    protected final RegionManager regionManager;
    protected final boolean debug;

    public ListenerFlag(RegionMC plugin, String name, T defaultValue) {
        super(name, defaultValue);
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.debug = plugin.isDebugEnabled();
    }

    protected boolean isFlagDenied(Location loc, String flag) {
        for (Region region : regionManager.getRegionsAtLocation(loc)) {
            Object value = region.getFlag(flag);
            if (value != null) return !getFlagBoolean(region, flag);
        }
        return false;
    }

    protected boolean isActionDenied(Player player, Location loc, String flag) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] isActionDenied: игрок=" + player.getName() + ", локация=" + loc + ", флаг=" + flag);
        }
        String regionName = regionManager.findRegionAtLocation(loc);
        if (debug) {
            plugin.getLogger().info("[DEBUG] isActionDenied: регион локации=" + regionName);
        }
        if (regionName != null && regionName.startsWith("__Global__")) {
            if (debug) plugin.getLogger().info("[DEBUG] isActionDenied: глобальный регион, разрешено");
            return false;
        }
        if (regionName == null || shouldBypassRegionProtection(player, regionName)) {
            if (debug) plugin.getLogger().info("[DEBUG] isActionDenied: регион null или байпас, разрешено");
            return false;
        }
        if (regionManager.isOwner(player, regionName) || regionManager.isMember(player, regionName)) {
            if (debug) plugin.getLogger().info("[DEBUG] isActionDenied: игрок владелец/участник, разрешено");
            return false;
        }
        Region region = regionManager.getRegion(regionName);
        boolean denied = region != null && !getFlagBoolean(region, flag);
        if (debug) {
            plugin.getLogger().info("[DEBUG] isActionDenied: результат=" + denied);
        }
        return denied;
    }

    /**
     * Специализированный метод для строительства/ломки.
     * Глобальный регион всегда разрешён, даже если игрок в чужом регионе.
     * НЕ проверяет флаг use-items – это задача UseItemsFlag.
     */
    protected boolean isBuildActionDenied(Player player, Location blockLoc, String flag) {
        if (debug) {
            plugin.getLogger().info("[DEBUG] isBuildActionDenied: игрок=" + player.getName() + ", блок=" + blockLoc + ", флаг=" + flag);
            plugin.getLogger().info("[DEBUG] isBuildActionDenied: позиция игрока=" + player.getLocation());
        }
        String blockRegionName = regionManager.findRegionAtLocation(blockLoc);
        if (debug) {
            plugin.getLogger().info("[DEBUG] isBuildActionDenied: регион блока=" + blockRegionName);
        }
        if (blockRegionName != null && blockRegionName.startsWith("__Global__")) {
            if (debug) plugin.getLogger().info("[DEBUG] isBuildActionDenied: блок в глобальном регионе, разрешено");
            return false;
        }
        if (shouldBypassRegionProtection(player, null)) {
            if (debug) plugin.getLogger().info("[DEBUG] isBuildActionDenied: байпас, разрешено");
            return false;
        }
        String playerRegionName = regionManager.findRegionAtLocation(player.getLocation());
        if (debug) {
            plugin.getLogger().info("[DEBUG] isBuildActionDenied: регион игрока=" + playerRegionName);
        }
        if (playerRegionName != null) {
            Region playerRegion = regionManager.getRegion(playerRegionName);
            if (playerRegion != null) {
                boolean isOwnerOrMember = playerRegion.isOwner(player.getUniqueId()) || playerRegion.isMember(player.getUniqueId());
                if (!isOwnerOrMember) {
                    if (debug) plugin.getLogger().info("[DEBUG] isBuildActionDenied: игрок в чужом регионе, запрещено");
                    sendDenyMessage(player, blockLoc, "protection.build", "§cВы не можете строить, находясь в чужом регионе!");
                    return true;
                }
            }
        }
        if (blockRegionName == null) {
            if (debug) plugin.getLogger().info("[DEBUG] isBuildActionDenied: блок не в регионе, разрешено");
            return false;
        }
        if (shouldBypassRegionProtection(player, blockRegionName)) {
            if (debug) plugin.getLogger().info("[DEBUG] isBuildActionDenied: байпас региона, разрешено");
            return false;
        }
        if (regionManager.isOwner(player, blockRegionName) || regionManager.isMember(player, blockRegionName)) {
            if (debug) plugin.getLogger().info("[DEBUG] isBuildActionDenied: игрок владелец/участник региона блока, разрешено");
            return false;
        }
        Region region = regionManager.getRegion(blockRegionName);
        boolean result = region != null && !getFlagBoolean(region, flag);
        if (debug) {
            plugin.getLogger().info("[DEBUG] isBuildActionDenied: результат стандартной проверки=" + result);
        }
        return result;
    }

    protected boolean getFlagBoolean(Region region, String flag) {
        Object value = region.getFlag(flag);
        if (value == null) return true;
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) {
            String l = s.toLowerCase().trim();
            if (l.equals("true") || l.equals("allow") || l.equals("yes") || l.equals("1") ||
                    l.equals("разрешить") || l.equals("enable") || l.equals("enabled") || l.equals("on")) {
                return true;
            }
            if (l.equals("false") || l.equals("deny") || l.equals("no") || l.equals("0") ||
                    l.equals("запретить") || l.equals("disable") || l.equals("disabled") || l.equals("off")) {
                return false;
            }
        }
        return true;
    }

    protected boolean shouldBypassRegionProtection(Player player, String regionName) {
        if (player.isOp() || player.hasPermission("*") || player.hasPermission("regionmc.admin")) return true;
        if (regionName == null) return false;
        String normalized = regionName.toLowerCase();
        return player.hasPermission("regionmc.bypass." + normalized)
                || player.hasPermission("regionmc.bypass.all")
                || player.hasPermission("regionmc.bypass.*");
    }

    protected void sendMessage(Player player, String key, String... replacements) {
        if (player == null) return;
        try {
            String message = plugin.getLanguageManager().getMessage(key, replacements);
            if (message != null && !message.isEmpty() && !message.contains("Message not found")) {
                player.sendMessage(message);
            } else {
                if (replacements.length > 0) {
                    player.sendMessage(replacements[0]);
                }
            }
        } catch (Exception e) {
            if (replacements.length > 0) {
                player.sendMessage(replacements[0]);
            }
            if (debug) plugin.getLogger().warning("Failed to send message for key: " + key);
        }
    }

    protected void sendMessage(Player player, String key) {
        sendMessage(player, key, new String[0]);
    }

    protected void sendMessage(Player player, String key, String param) {
        sendMessage(player, key, new String[]{param});
    }

    protected void sendDenyMessage(Player player, Location loc, String key, String... replacements) {
        if (player == null) return;
        if (debug) {
            plugin.getLogger().info("[DEBUG] sendDenyMessage called: player=" + player.getName() + ", loc=" + loc + ", key=" + key);
        }

        String customMsg = null;
        if (loc != null) {
            String regionName = regionManager.findRegionAtLocation(loc);
            if (debug) {
                plugin.getLogger().info("[DEBUG] regionName=" + regionName);
            }
            if (regionName != null) {
                Region region = regionManager.getRegion(regionName);
                if (region != null) {
                    Object raw = region.getFlag("deny-message");
                    if (debug) {
                        plugin.getLogger().info("[DEBUG] raw deny-message=" + raw);
                    }
                    if (raw instanceof String) {
                        String msg = (String) raw;
                        if (msg != null && !msg.trim().isEmpty()) {
                            customMsg = msg;
                            if (debug) {
                                plugin.getLogger().info("[DEBUG] customMsg=" + customMsg);
                            }
                        }
                    }
                }
            }
        }

        if (customMsg != null) {
            player.sendMessage(ColorUtil.colorize(customMsg));
            if (debug) {
                plugin.getLogger().info("[DEBUG] Отправлено кастомное сообщение: " + customMsg);
            }
        } else {
            sendMessage(player, key, replacements);
            if (debug) {
                plugin.getLogger().info("[DEBUG] Отправлено стандартное сообщение по ключу: " + key);
            }
        }
    }

    protected void debugLog(String msg) {
        if (debug) plugin.getLogger().info(msg);
    }
}