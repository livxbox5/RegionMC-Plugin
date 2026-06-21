package org.KillerYT.regionMC.listeners;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.chat.BlockChatFlag;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.managers.RegionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BlockChatHandler implements Listener {

    private final RegionMC plugin;
    private final RegionManager regionManager;
    private final boolean debug;

    public BlockChatHandler(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.debug = plugin.getConfig().getBoolean("settings.debug", false);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Администраторы из списка admin-user имеют полный bypass
        if (regionManager.isAdminBypass(player)) {
            debugLog("Player " + player.getName() + " bypassed chat block as admin");
            return;
        }

        if (player.hasPermission("regionmc.bypass.chat")) {
            debugLog("Player " + player.getName() + " bypassed chat block with permission");
            return;
        }

        String regionName = regionManager.findRegionAtLocation(player.getLocation());

        if (regionName == null) {
            debugLog("Player " + player.getName() + " not in any region");
            return;
        }

        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            debugLog("Region not found: " + regionName);
            return;
        }

        Object blockChatFlag = region.getFlag("block-chat");
        debugLog("Checking block-chat flag for player " + player.getName() +
                " in region " + regionName + ": " + blockChatFlag);

        if (blockChatFlag == null) {
            debugLog("No block-chat flag for region " + regionName);
            return;
        }

        org.KillerYT.regionMC.flags.main.AbstractRegionFlag<?> flag =
                plugin.getFlagManager().getFlag("block-chat");

        if (flag instanceof BlockChatFlag chatFlag) {
            Boolean flagValue = convertToBoolean(blockChatFlag);
            boolean isChatAllowed = chatFlag.isAllowed(flagValue);

            if (!isChatAllowed) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Чат заблокирован в этом регионе!", NamedTextColor.RED));
                debugLog("Chat blocked for player " + player.getName() +
                        " in region " + regionName + " (block-chat: " + blockChatFlag + ")");
            } else {
                debugLog("Chat allowed for player " + player.getName() +
                        " in region " + regionName + " (block-chat: " + blockChatFlag + ")");
            }
        } else {
            Boolean flagValue = convertToBoolean(blockChatFlag);
            if (flagValue != null && flagValue) {
                event.setCancelled(true);
                player.sendMessage(Component.text("Чат заблокирован в этом регионе!", NamedTextColor.RED));
                debugLog("Chat blocked (boolean) for player " + player.getName() +
                        " in region " + regionName);
            }
        }
    }

    private Boolean convertToBoolean(Object value) {
        return switch (value) {
            case Boolean b -> b;
            case String s -> {
                String lower = s.toLowerCase();
                yield lower.equals("true") || lower.equals("1") || lower.equals("yes");
            }
            case Number n -> n.intValue() != 0;
            default -> null;
        };
    }

    private void debugLog(String message) {
        if (debug) {
            plugin.getLogger().info(message);
        }
    }
}