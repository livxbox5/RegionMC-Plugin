// RegionListCommand.java
package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.utils.JsonMessageUtil;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionListCommand {

    private final RegionMC plugin;

    public RegionListCommand(RegionMC plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        // Проверка прав
        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.list")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no-permission");
            return true;
        }

        // Получаем регионы игрока через RegionManager
        java.util.List<org.KillerYT.regionMC.region.Region> playerRegions =
                plugin.getRegionManager().getPlayerRegions(player.getUniqueId());

        if (playerRegions.isEmpty()) {
            player.sendMessage("§7No regions found.");

            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§a[🎯 Set Position 1]",
                            "§7Start creating your first region", "/region pos1"),
                    JsonMessageUtil.createRunComponent(" §6[❓ Help]",
                            "§7Show help", "/region help")
            );
            return true;
        }

        player.sendMessage("§6=== Your Regions (§e" + playerRegions.size() + "§6) ===");

        for (org.KillerYT.regionMC.region.Region region : playerRegions) {
            String regionName = region.getName();
            String status = region.isOwner(player.getUniqueId()) ? "§aOwner" : "§bMember";

            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§e" + regionName + " §7(" + status + "§7)",
                            "§7Click to view info about " + regionName,
                            "/region info " + regionName),
                    JsonMessageUtil.createRunComponent(" §a[ℹ]",
                            "§7View detailed info", "/region info " + regionName),
                    JsonMessageUtil.createRunComponent(" §c[✕]",
                            "§7Delete region", "/region delete " + regionName)
            );
        }

        // Кнопки действий
        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§a[🎯 Create New Region]",
                        "§7Start creating a new region", "/region pos1"),
                JsonMessageUtil.createRunComponent(" §6[❓ Help]",
                        "§7Show help", "/region help")
        );

        return true;
    }
}