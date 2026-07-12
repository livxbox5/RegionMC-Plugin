package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.utils.LanguageManager;
import org.KillerYT.regionMC.utils.JsonMessageUtil;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionMemberCommand {
    private final RegionMC plugin;
    private final LanguageManager lang;

    public RegionMemberCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        // Проверка прав на выполнение (addmember или removemember)
        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.addmember") &&
                !PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.removemember")) {
            // old: if (!player.hasPermission("regionmc.command.addmember") && !player.hasPermission("regionmc.command.removemember")) {
            lang.sendMessage(player, "commands.no-permission");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6=== Member Management ===");
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent("§a[➕ Add Member]",
                            "§7Add member to region", "/region addmember "),
                    JsonMessageUtil.createSuggestComponent(" §c[➖ Remove Member]",
                            "§7Remove member from region", "/region removemember ")
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

        if (!hasPermissionToManageMembers(player, regionName)) {
            lang.sendMessage(player, "commands.no-permission");
            return true;
        }

        boolean success = false;
        switch (action) {
            case "add":
                if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.addmember")) {
                    // old: if (!player.hasPermission("regionmc.command.addmember")) {
                    lang.sendMessage(player, "commands.no-permission");
                    return true;
                }
                success = plugin.getRegionManager().addMember(regionName, targetPlayerName);
                break;
            case "remove":
                if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.removemember")) {
                    // old: if (!player.hasPermission("regionmc.command.removemember")) {
                    lang.sendMessage(player, "commands.no-permission");
                    return true;
                }
                success = plugin.getRegionManager().removeMember(regionName, targetPlayerName);
                break;
            default:
                sendUsage(player);
                return true;
        }

        if (success) {
            lang.sendMessage(player, "region.member." + action + ".success", targetPlayerName, regionName);
        } else {
            lang.sendMessage(player, "region.member." + action + ".failed", targetPlayerName, regionName);
        }

        return true;
    }

    private void sendUsage(Player player) {
        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createSuggestComponent("§cUsage: §e/region member <add|remove> <region> <player>",
                        "§7Click to suggest command", "/region member "),
                JsonMessageUtil.createSuggestComponent(" §a[➕ Add Member]",
                        "§7Quickly add a member", "/region addmember "),
                JsonMessageUtil.createSuggestComponent(" §c[➖ Remove Member]",
                        "§7Quickly remove a member", "/region removemember ")
        );
    }

    private boolean hasPermissionToManageMembers(Player player, String regionName) {
        // Владелец может управлять участниками, админы из списка могут управлять любыми регионами
        return plugin.getRegionManager().isOwner(player, regionName) ||
                PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.admin");
        // old: player.hasPermission("regionmc.admin")
    }
}