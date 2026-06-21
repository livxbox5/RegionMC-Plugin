package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.utils.LanguageManager;
import org.KillerYT.regionMC.utils.JsonMessageUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class RegionPosCommand {

    private final RegionMC plugin;
    private final LanguageManager lang;
    private final RegionManager regionManager;

    public RegionPosCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.regionManager = plugin.getRegionManager();
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            showPositionStatus(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        Location location = player.getLocation();

        switch (subCommand) {
            case "pos1":
                regionManager.setPos1(player, location);
                sendPositionSetMessage(player, "1", location);
                showQuickActions(player);
                return true;

            case "pos2":
                regionManager.setPos2(player, location);
                sendPositionSetMessage(player, "2", location);
                showQuickActions(player);
                return true;

            case "clear":
                clearPositions(player);
                lang.sendMessage(player, "region.pos.cleared");
                return true;

            case "status":
                showPositionStatus(player);
                return true;

            default:
                showHelp(player);
                return true;
        }
    }

    private void sendPositionSetMessage(Player player, String positionNumber, Location location) {
        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createSuggestComponent("§a✓ Position " + positionNumber + " set: §7" + formatLocation(location),
                        "§7Click to copy location",
                        formatLocation(location)
                ),
                Component.text(" "),
                JsonMessageUtil.createRunComponent("§6[📋 Status]",
                        "§7Check selection status", "/region pos status"),
                Component.text(" "),
                JsonMessageUtil.createRunComponent("§c[🗑 Clear]",
                        "§7Clear both positions", "/region pos clear")
        );
    }

    private void showPositionStatus(Player player) {
        Location pos1 = getPos1(player);
        Location pos2 = getPos2(player);

        player.sendMessage("§6=== Region Selection Status ===");

        if (pos1 != null) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent("§a✓ Pos1: §7" + formatLocation(pos1),
                            "§7Click to copy location", formatLocation(pos1)),
                    JsonMessageUtil.createRunComponent(" §e[✎ Update]",
                            "§7Update position 1", "/region pos1")
            );
        } else {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§c✗ Pos1: Not set",
                            "§7Click to set position 1",
                            "/region pos1")
            );
        }

        if (pos2 != null) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent("§a✓ Pos2: §7" + formatLocation(pos2),
                            "§7Click to copy location", formatLocation(pos2)),
                    JsonMessageUtil.createRunComponent(" §e[✎ Update]",
                            "§7Update position 2", "/region pos2")
            );
        } else {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§c✗ Pos2: Not set",
                            "§7Click to set position 2",
                            "/region pos2")
            );
        }

        if (hasBothPositions(player)) {
            player.sendMessage("§a✓ Ready to create region!");
            showQuickActions(player);
        } else {
            player.sendMessage("§7Set both positions to create a region");
        }
    }

    private void showQuickActions(Player player) {
        if (hasBothPositions(player)) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createClaimButton(),
                    Component.text(" "),
                    JsonMessageUtil.createExpandButton(),
                    Component.text(" "),
                    JsonMessageUtil.createRunComponent("§c[🗑 Clear]",
                            "§7Clear selection", "/region pos clear")
            );
        } else {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§b[🎯 Set Pos1]",
                            "§7Set first position", "/region pos1"),
                    Component.text(" "),
                    JsonMessageUtil.createRunComponent("§b[🎯 Set Pos2]",
                            "§7Set second position", "/region pos2"),
                    Component.text(" "),
                    JsonMessageUtil.createRunComponent("§6[❓ Help]",
                            "§7Show help", "/region help")
            );
        }
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== Region Position Commands ===");

        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§7• §e/region pos1 §7- Set position 1 at your location",
                        "§7Click to set position 1", "/region pos1")
        );

        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§7• §e/region pos2 §7- Set position 2 at your location",
                        "§7Click to set position 2", "/region pos2")
        );

        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§7• §e/region pos status §7- Show current selection status",
                        "§7Click to show status", "/region pos status")
        );

        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§7• §e/region pos clear §7- Clear both positions",
                        "§7Click to clear positions", "/region pos clear")
        );

        showQuickActions(player);
    }

    // Основные методы доступа - теперь используют RegionManager
    public Location getPos1(Player player) {
        return regionManager.getPos1(player);
    }

    public Location getPos2(Player player) {
        return regionManager.getPos2(player);
    }

    // Альтернативные названия методов для совместимости
    public Location getPosition1(Player player) {
        return getPos1(player);
    }

    public Location getPosition2(Player player) {
        return getPos2(player);
    }

    public void clearSelection(Player player) {
        UUID playerId = player.getUniqueId();
        // Очищаем через RegionManager
        regionManager.setPos1(player, null);
        regionManager.setPos2(player, null);
    }

    // Альтернативное название метода для совместимости
    public void clearPositions(Player player) {
        clearSelection(player);
    }

    public boolean hasBothPositions(Player player) {
        return getPos1(player) != null && getPos2(player) != null;
    }

    public Location[] getPlayerSelection(UUID playerId) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            Location pos1 = getPos1(player);
            Location pos2 = getPos2(player);

            if (pos1 != null && pos2 != null) {
                return new Location[]{pos1, pos2};
            }
        }
        return null;
    }

    public void updatePlayerSelection(UUID playerId, Location pos1, Location pos2) {
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            if (pos1 != null) {
                regionManager.setPos1(player, pos1);
            }
            if (pos2 != null) {
                regionManager.setPos2(player, pos2);
            }
        }
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "null";
        }
        return String.format("X: %d, Y: %d, Z: %d, World: %s",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                location.getWorld().getName());
    }
}