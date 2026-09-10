package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.utils.JsonMessageUtil;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionHelpCommand {

    private final RegionMC plugin;

    public RegionHelpCommand(RegionMC plugin) {
        this.plugin = plugin;
    }

    public boolean execute(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        boolean hasAdmin    = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.admin");
        boolean hasCreate   = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.claim")
                || PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.region.create");
        boolean hasDelete   = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.delete")
                || PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.region.delete");
        boolean hasFlag     = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.flag");
        boolean hasPos      = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.pos");
        boolean hasExpand   = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.expand");
        boolean hasPriority = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.priority");
        boolean hasOwnerManage  = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.addowner");
        boolean hasMemberManage = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.addmember");
        boolean hasShow     = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.show");
        boolean hasInfo     = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.info");
        boolean hasList     = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.list");
        boolean hasWand     = PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.wand");

        player.sendMessage("§6=== RegionMC Commands ===");

        // ==================== ОСНОВНЫЕ КОМАНДЫ ====================
        if (hasPos) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent(
                            "§7• §e/region pos1 §7- Set position 1 at your location",
                            "§7Click to set position 1",
                            "/region pos1"
                    )
            );
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent(
                            "§7• §e/region pos2 §7- Set position 2 at your location",
                            "§7Click to set position 2",
                            "/region pos2"
                    )
            );
        }

        if (hasCreate) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent(
                            "§7• §e/region claim <name> §7- Claim a new region",
                            "§7Click to suggest command",
                            "/region claim "
                    )
            );
        }

        if (hasExpand) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent(
                            "§7• §e/region expand <blocks> <up|down> §7- Expand region",
                            "§7Click to suggest command",
                            "/region expand "
                    )
            );
        }

        if (hasDelete) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent(
                            "§7• §e/region delete <name> §7- Delete a region",
                            "§7Click to suggest command",
                            "/region delete "
                    )
            );
        }

        // ==================== ИНФОРМАЦИОННЫЕ КОМАНДЫ ====================
        if (hasInfo) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent(
                            "§7• §e/region info [name] §7- View region info",
                            "§7Click to suggest command",
                            "/region info "
                    )
            );
        }

        if (hasList) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent(
                            "§7• §e/region list §7- List all regions",
                            "§7Click to list regions",
                            "/region list"
                    )
            );
        }

        if (hasFlag) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent(
                            "§7• §e/region flag <region> <flag> <value> §7- Set region flag",
                            "§7Click to suggest command",
                            "/region flag "
                    )
            );
        }

        // ==================== УПРАВЛЕНИЕ УЧАСТНИКАМИ ====================
        boolean showMemberSection = hasOwnerManage || hasMemberManage;
        if (showMemberSection) {
            player.sendMessage("§6=== Member Management ===");

            if (hasMemberManage) {
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createSuggestComponent(
                                "§7• §e/region addmember <region> <player> §7- Add member to region",
                                "§7Click to suggest command",
                                "/region addmember "
                        )
                );
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createSuggestComponent(
                                "§7• §e/region removemember <region> <player> §7- Remove member from region",
                                "§7Click to suggest command",
                                "/region removemember "
                        )
                );
            }

            if (hasOwnerManage) {
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createSuggestComponent(
                                "§7• §e/region addowner <region> <player> §7- Add owner to region",
                                "§7Click to suggest command",
                                "/region addowner "
                        )
                );
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createSuggestComponent(
                                "§7• §e/region removeowner <region> <player> §7- Remove owner from region",
                                "§7Click to suggest command",
                                "/region removeowner "
                        )
                );
            }
        }

        // ==================== БЫСТРЫЕ ДЕЙСТВИЯ ====================
        if (hasPos || hasCreate) {
            player.sendMessage("§6=== Quick Actions ===");

            if (hasPos) {
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createSetPos1Button(),
                        JsonMessageUtil.createSetPos2Button(),
                        hasList
                                ? JsonMessageUtil.createRunComponent(
                                " §a[📋] List Regions",
                                "§7View all your regions",
                                "/region list"
                        )
                                : JsonMessageUtil.createHelpButton()
                );
            } else if (hasList) {
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createRunComponent(
                                " §a[📋] List Regions",
                                "§7View all your regions",
                                "/region list"
                        )
                );
            }
        }

        // ==================== БЫСТРОЕ СОЗДАНИЕ ====================
        if (hasCreate && hasPos) {
            player.sendMessage("§6=== Quick Creation ===");
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createClaimButton(),
                    JsonMessageUtil.createRunComponent(
                            " §b[⚡] Quick Setup",
                            "§7Open quick creation menu",
                            "/region pos1"
                    )
            );
        }

        // ==================== АДМИН-КОМАНДЫ ====================
        if (hasAdmin) {
            player.sendMessage("§6=== Admin Commands ===");
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent(
                            "§7• §c/region reload §7- Reload plugin configuration",
                            "§7Click to reload plugin",
                            "/region reload"
                    )
            );

            if (hasPriority) {
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createSuggestComponent(
                                "§7• §c/region priority <region> <priority> §7- Set region priority",
                                "§7Click to suggest command",
                                "/region priority "
                        )
                );
            }

            if (hasShow) {
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createRunComponent(
                                "§7• §c/region show §7- Toggle region boundaries",
                                "§7Click to toggle boundaries",
                                "/region show"
                        )
                );
            }
        }

        // ==================== СОВЕТЫ ====================
        player.sendMessage("§6=== Usage Tips ===");
        player.sendMessage("§7• Click on commands to insert them into chat");
        player.sendMessage("§7• Replace text in §e< >§7 with your values");

        if (hasCreate) {
            player.sendMessage("§7• Use §e/region claim §7after setting both positions");
        }

        if (!hasCreate && !hasDelete && !hasFlag) {
            player.sendMessage("§7• §8You have basic view-only permissions");
        } else if (hasCreate && hasDelete) {
            player.sendMessage("§7• §aYou have full region management permissions");
        }

        return true;
    }
}