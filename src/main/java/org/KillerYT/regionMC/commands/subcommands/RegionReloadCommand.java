package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionReloadCommand {

    private final RegionMC plugin;

    public RegionReloadCommand(RegionMC plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭта команда только для игроков!");
            return true;
        }

        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.reload")) {
            // old: if (!player.hasPermission("regionmc.command.reload")) {
            if (plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().sendMessage(player, "commands.no-permission");
            } else {
                player.sendMessage("§cУ вас нет прав!");
            }
            return true;
        }

        try {
            if (plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().sendMessage(player, "commands.reload.start");
            } else {
                player.sendMessage("§eПерезагрузка RegionMC...");
            }

            plugin.reloadManagers();

            if (plugin.getAdminManager() != null) {
                plugin.getAdminManager().reload();
            }

            if (plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().sendMessage(player, "commands.reload.success");
            } else {
                player.sendMessage("§aRegionMC успешно перезагружен!");
            }

            plugin.getLogger().info("RegionMC reloaded by " + sender.getName());

        } catch (Exception e) {
            if (plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().sendMessage(player, "commands.reload.error");
            } else {
                player.sendMessage("§cОшибка при перезагрузке RegionMC!");
            }
            plugin.getLogger().severe("Failed to reload RegionMC: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }
}