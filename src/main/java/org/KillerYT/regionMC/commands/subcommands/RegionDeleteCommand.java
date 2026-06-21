package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.utils.JsonMessageUtil;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionDeleteCommand {
    private final RegionMC plugin;

    public RegionDeleteCommand(RegionMC plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.delete")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no-permission");
            return true;
        }

        if (args.length < 1) {
            plugin.getLanguageManager().sendMessage(player, "commands.usage", "/region delete <name>");
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent("§e/region delete <name>",
                            "§7Click to suggest command",
                            "/region delete ")
            );
            return true;
        }

        String regionName = args[0];

        // Проверка валидности имени региона
        if (!isValidRegionName(regionName)) {
            plugin.getLanguageManager().sendMessage(player, "region.create.invalid-name");
            return true;
        }

        // Проверка существования региона
        if (!plugin.getRegionManager().regionExists(regionName)) {
            plugin.getLanguageManager().sendMessage(player, "region.not-found", regionName);

            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§a[📋 List Regions]",
                            "§7View all regions", "/region list"),
                    JsonMessageUtil.createSuggestComponent(" §6[🏷 Create Region]",
                            "§7Create this region", "/region create " + regionName)
            );
            return true;
        }

        // Проверка прав на удаление
        if (!hasPermissionToDelete(player, regionName)) {
            plugin.getLanguageManager().sendMessage(player, "commands.no-permission");
            return true;
        }

        // Удаление региона
        boolean success = plugin.getRegionManager().removeRegion(regionName);

        if (success) {
            plugin.getLanguageManager().sendMessage(player, "region.deleted", regionName);

            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§a[📋 List Regions]",
                            "§7View all regions", "/region list"),
                    JsonMessageUtil.createClaimButton()
            );
        } else {
            plugin.getLanguageManager().sendMessage(player, "errors.internal-error");
        }

        return true;
    }

    private boolean isValidRegionName(String name) {
        return name.matches("[a-zA-Z0-9_]{3,16}");
    }

    private boolean hasPermissionToDelete(Player player, String regionName) {
        // Владелец может удалить свой регион, админы из списка могут удалить любой
        return plugin.getRegionManager().isOwner(player, regionName) ||
                PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.admin");
    }
}