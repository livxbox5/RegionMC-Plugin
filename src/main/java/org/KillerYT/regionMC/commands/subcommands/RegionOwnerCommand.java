package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.utils.LanguageManager;
import org.KillerYT.regionMC.utils.JsonMessageUtil;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionOwnerCommand {
    private final RegionMC plugin;
    private final LanguageManager lang;

    public RegionOwnerCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        // Проверка прав на выполнение (addowner или removeowner)
        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.addowner") &&
                !PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.removeowner")) {
            // old: if (!player.hasPermission("regionmc.command.addowner") && !player.hasPermission("regionmc.command.removeowner")) {
            lang.sendMessage(player, "commands.no-permission");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6=== Owner Management ===");
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent("§a[➕ Add Owner]",
                            "§7Add owner to region", "/region addowner "),
                    JsonMessageUtil.createSuggestComponent(" §c[➖ Remove Owner]",
                            "§7Remove owner from region", "/region removeowner ")
            );
            return true;
        }

        if (args.length < 3) {
            sendUsage(player);
            return true;
        }

        String action = args[0].toLowerCase();
        String regionName = args[1];
        String targetPlayerName = args[2];

        if (!plugin.getRegionManager().regionExists(regionName)) {
            lang.sendMessage(player, "region.not-found", regionName);
            return true;
        }

        if (!hasPermissionToManageOwners(player, regionName)) {
            lang.sendMessage(player, "commands.no-permission");
            return true;
        }

        boolean success = false;
        switch (action) {
            case "add":
                if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.addowner")) {
                    // old: if (!player.hasPermission("regionmc.command.addowner")) {
                    lang.sendMessage(player, "commands.no-permission");
                    return true;
                }
                success = plugin.getRegionManager().addOwner(regionName, targetPlayerName);
                break;
            case "remove":
                if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.removeowner")) {
                    // old: if (!player.hasPermission("regionmc.command.removeowner")) {
                    lang.sendMessage(player, "commands.no-permission");
                    return true;
                }
                success = plugin.getRegionManager().removeOwner(regionName, targetPlayerName);
                break;
            default:
                sendUsage(player);
                return true;
        }

        if (success) {
            lang.sendMessage(player, "region.owner." + action + ".success", targetPlayerName, regionName);
        } else {
            lang.sendMessage(player, "region.owner." + action + ".failed", targetPlayerName, regionName);
        }

        return true;
    }

    private void sendUsage(Player player) {
        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createSuggestComponent("§cUsage: §e/region owner <add|remove> <region> <player>",
                        "§7Click to suggest command", "/region owner "),
                JsonMessageUtil.createSuggestComponent(" §a[➕ Add Owner]",
                        "§7Quickly add an owner", "/region addowner "),
                JsonMessageUtil.createSuggestComponent(" §c[➖ Remove Owner]",
                        "§7Quickly remove an owner", "/region removeowner ")
        );
    }

    private boolean hasPermissionToManageOwners(Player player, String regionName) {
        // Только админы из списка могут управлять владельцами
        return PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.admin");
        // old: player.hasPermission("regionmc.admin");
    }
}