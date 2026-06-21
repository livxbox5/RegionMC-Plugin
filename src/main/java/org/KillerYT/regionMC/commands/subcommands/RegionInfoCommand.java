package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.utils.JsonMessageUtil;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RegionInfoCommand {

    private final RegionMC plugin;
    private final RegionManager regionManager;

    public RegionInfoCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
    }

    public RegionInfoCommand(RegionManager regionManager, RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.info")) {
            // old: if (!player.hasPermission("regionmc.command.info")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no-permission");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /region info <region-name>");
            return true;
        }

        String regionName = args[0];
        Region region = regionManager.getRegion(regionName);

        if (region == null) {
            player.sendMessage("§cRegion '" + regionName + "' not found!");
            return true;
        }

        if (!region.isOwner(player.getUniqueId()) && !region.isMember(player.getUniqueId())
                && !PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.admin")) {
            // old: && !player.hasPermission("regionmc.admin")
            player.sendMessage("§cYou don't have access to this region!");
            return true;
        }

        displayRegionInfo(player, region);
        return true;
    }

    public void showRegionInfo(Player player, Region region) {
        displayRegionInfo(player, region);
    }

    private void displayRegionInfo(Player player, Region region) {
        String regionName = region.getName();

        int[] flagCounts = countRegionFlags(region, player);
        int allowCount = flagCounts[0];
        int denyCount = flagCounts[1];

        player.sendMessage("§6=========== " + regionName + " ============");

        String flagsCounter = String.format("§a%d§7/§c%d", allowCount, denyCount);

        JsonMessageUtil.sendCompositeMessage(player,
                Component.text("§6Флаги: " + flagsCounter + " "),
                JsonMessageUtil.createRunComponent("§e[edit]",
                        "§7Изменить флаги", "/region flag " + regionName),
                Component.text(" §6priority §e" + region.getPriority() + " "),
                JsonMessageUtil.createSuggestComponent("§e[edit]",
                        "§7Изменить приоритет", "/region priority " + regionName + " ")
        );

        player.sendMessage("§6Владелец:");
        List<UUID> owners = getOwners(region);

        if (owners.isEmpty()) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent("§a[+]",
                            "§7Добавить владельца", "/region addowner " + regionName + " ")
            );
        } else {
            List<Component> ownerComponents = new ArrayList<>();
            for (UUID ownerId : owners) {
                String ownerName = getPlayerName(ownerId);
                if (ownerName != null && !ownerName.equals("Unknown")) {
                    ownerComponents.add(Component.text("§7" + ownerName + " "));
                }
            }
            ownerComponents.add(JsonMessageUtil.createSuggestComponent("§a[+]",
                    "§7Добавить еще владельца", "/region addowner " + regionName + " "));

            JsonMessageUtil.sendCompositeMessage(player, ownerComponents.toArray(new Component[0]));
        }

        player.sendMessage("§6Участники:");
        List<UUID> members = getMembers(region);

        if (members.isEmpty()) {
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent("§a[+]",
                            "§7Добавить участника", "/region addmember " + regionName + " ")
            );
        } else {
            List<Component> memberComponents = new ArrayList<>();
            int count = 0;

            for (UUID memberId : members) {
                String memberName = getPlayerName(memberId);
                if (memberName != null && !memberName.equals("Unknown")) {
                    memberComponents.add(Component.text("§7" + memberName + " "));
                    memberComponents.add(JsonMessageUtil.createRunComponent("§c[-]",
                            "§7Удалить участника", "/region removemember " + regionName + " " + memberName));
                    memberComponents.add(Component.text("  "));

                    count++;
                    if (count >= 10) {
                        memberComponents.add(Component.text("§7..."));
                        break;
                    }
                }
            }

            memberComponents.add(JsonMessageUtil.createSuggestComponent("§a[+]",
                    "§7Добавить еще участника", "/region addmember " + regionName + " "));

            JsonMessageUtil.sendCompositeMessage(player, memberComponents.toArray(new Component[0]));
        }

        displayOptionalBoundsInfo(player, region);
    }

    private int[] countRegionFlags(Region region, Player player) {
        int allowCount = 0;
        int denyCount = 0;

        try {
            org.KillerYT.regionMC.flags.main.FlagManager flagManager = plugin.getFlagManager();
            if (flagManager == null) {
                return new int[]{allowCount, denyCount};
            }

            List<org.KillerYT.regionMC.flags.main.AbstractRegionFlag<?>> availableFlags =
                    flagManager.getFlagsWithPermission(player);

            for (org.KillerYT.regionMC.flags.main.AbstractRegionFlag<?> flag : availableFlags) {
                String flagName = flag.getName();
                if (flagName == null) continue;

                Object currentValue = region.getFlag(flagName);
                boolean isAllowed;

                if (currentValue != null) {
                    isAllowed = isFlagAllowed(currentValue);
                } else {
                    Object defaultValue = flag.getDefaultValue();
                    isAllowed = isFlagAllowed(defaultValue);
                }

                if (isAllowed) {
                    allowCount++;
                } else {
                    denyCount++;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при подсчете флагов: " + e.getMessage());
        }

        return new int[]{allowCount, denyCount};
    }

    private boolean isFlagAllowed(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        } else if (value instanceof String strValue) {
            String lowerValue = strValue.toLowerCase();
            return lowerValue.equals("true") || lowerValue.equals("allow") ||
                    lowerValue.equals("on") || lowerValue.equals("yes") ||
                    lowerValue.equals("разрешить") || lowerValue.equals("да") ||
                    lowerValue.equals("включить") || lowerValue.equals("1");
        }
        return false;
    }

    private void displayOptionalBoundsInfo(Player player, Region region) {
        org.bukkit.Location pos1 = getPos1(region);
        org.bukkit.Location pos2 = getPos2(region);

        if (pos1 != null && pos2 != null) {
            player.sendMessage("§6Границы:");
            player.sendMessage("§7  Pos1: X: " + pos1.getBlockX() + " Y: " + pos1.getBlockY() + " Z: " + pos1.getBlockZ());
            player.sendMessage("§7  Pos2: X: " + pos2.getBlockX() + " Y: " + pos2.getBlockY() + " Z: " + pos2.getBlockZ());

            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§a[🎯 Wand]",
                            "§7Получить волшебную палочку", "/region wand"),
                    JsonMessageUtil.createSuggestComponent(" §6[📐 Resize]",
                            "§7Изменить размер", "/region expand " + region.getName() + " ")
            );
        }
    }

    private String formatLocation(org.bukkit.Location location) {
        if (location == null) return "§cНе установлено";
        return "X: " + location.getBlockX() + " Y: " + location.getBlockY() + " Z: " + location.getBlockZ();
    }

    private String getPlayerName(UUID uuid) {
        try {
            String name = plugin.getServer().getOfflinePlayer(uuid).getName();
            return name != null ? name : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private List<UUID> getOwners(Region region) {
        try {
            Set<UUID> ownersSet = region.getOwners();
            return new ArrayList<>(ownersSet);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<UUID> getMembers(Region region) {
        try {
            Set<UUID> membersSet = region.getMembers();
            return new ArrayList<>(membersSet);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private org.bukkit.Location getPos1(Region region) {
        try {
            return region.getPos1();
        } catch (Exception e) {
            return null;
        }
    }

    private org.bukkit.Location getPos2(Region region) {
        try {
            return region.getPos2();
        } catch (Exception e) {
            return null;
        }
    }

    private void showQuickActions(Player player, Region region) {
        player.sendMessage("§6Быстрые действия:");

        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§a[📋 Обновить]",
                        "§7Обновить информацию", "/region info " + region.getName()),
                JsonMessageUtil.createRunComponent(" §e[🎯 Палочка]",
                        "§7Получить волшебную палочку", "/region wand"),
                JsonMessageUtil.createSuggestComponent(" §6[➕ Добавить]",
                        "§7Добавить участника", "/region addmember " + region.getName() + " ")
        );

        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§c[🗑 Удалить]",
                        "§7Удалить регион", "/region delete " + region.getName()),
                JsonMessageUtil.createRunComponent(" §b[📜 Список]",
                        "§7Показать все регионы", "/region list")
        );
    }
}