package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.utils.LanguageManager;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionPriorityCommand {

    private final RegionMC plugin;
    private final LanguageManager lang;

    public RegionPriorityCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        // Проверка прав
        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.priority")) {
            // old: if (!player.hasPermission("regionmc.command.priority")) {
            lang.sendMessage(player, "commands.no-permission");
            return true;
        }

        if (args.length < 2) {
            lang.sendMessage(player, "commands.usage", "/region priority <region> <priority>");
            return true;
        }

        String regionName = args[0];

        try {
            int priority = Integer.parseInt(args[1]);

            // Проверяем существование региона
            if (!plugin.getRegionManager().regionExists(regionName)) {
                lang.sendMessage(player, "region.not-found", regionName);
                return true;
            }

            // Проверяем права (только владельцы и администраторы)
            if (!plugin.getRegionManager().isOwner(player, regionName) &&
                    !PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.admin")) {
                // old: !player.hasPermission("regionmc.admin")
                lang.sendMessage(player, "commands.no-permission");
                return true;
            }

            boolean success = plugin.getRegionManager().setPriority(regionName, priority);

            if (success) {
                lang.sendMessage(player, "region.priority-set", regionName, String.valueOf(priority));
            } else {
                lang.sendMessage(player, "region.priority-error");
            }

        } catch (NumberFormatException e) {
            lang.sendMessage(player, "commands.invalid-number");
        }

        return true;
    }
}