package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.utils.JsonMessageUtil;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionClaimCommand {

    private final RegionMC plugin;
    private final RegionPosCommand posCommand;

    public RegionClaimCommand(RegionMC plugin, RegionPosCommand posCommand) {
        this.plugin = plugin;
        this.posCommand = posCommand;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        // Проверка прав через PermissionUtil
        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.claim")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no-permission");
            return true;
        }

        // Если нет аргументов - показываем меню с кнопкой
        if (args.length == 0) {
            JsonMessageUtil.sendClaimMenu(player);
            return true;
        }

        String regionName = args[0];

        // Проверка валидности имени региона
        if (!isValidRegionName(regionName)) {
            plugin.getLanguageManager().sendMessage(player, "region.create.invalid-name");

            // Показываем кнопку для повторной попытки
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createClaimButton()
            );
            return true;
        }

        // Получаем позиции из RegionManager
        Location pos1 = posCommand.getPos1(player);
        Location pos2 = posCommand.getPos2(player);

        // ОТЛАДОЧНАЯ ИНФОРМАЦИЯ
        plugin.getLogger().info("=== DEBUG REGION CREATION ===");
        plugin.getLogger().info("Player: " + player.getName());
        plugin.getLogger().info("Region name: " + regionName);
        plugin.getLogger().info("Pos1: " + (pos1 != null ? formatLocation(pos1) : "null"));
        plugin.getLogger().info("Pos2: " + (pos2 != null ? formatLocation(pos2) : "null"));
        plugin.getLogger().info("=== END DEBUG ===");

        if (pos1 == null || pos2 == null) {
            player.sendMessage("§cПозиции не установлены! Используйте:");
            player.sendMessage("§7/region pos1 - установить первую позицию");
            player.sendMessage("§7/region pos2 - установить вторую позицию");

            // Показываем меню управления позициями
            boolean hasPos1 = pos1 != null;
            boolean hasPos2 = pos2 != null;
            JsonMessageUtil.sendPositionMenu(player, hasPos1, hasPos2);
            return true;
        }

        // Проверяем, что позиции в одном мире
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            plugin.getLanguageManager().sendMessage(player, "region.different-worlds");
            return true;
        }

        RegionManager regionManager = plugin.getRegionManager();

        // Проверяем, существует ли регион с таким именем
        if (regionManager.regionExists(regionName)) {
            plugin.getLanguageManager().sendMessage(player, "region.already-exists", regionName);

            // Предложить посмотреть информацию о регионе
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRegionInfoButton(regionName)
            );
            return true;
        }

        // Создаем регион - используем правильные позиции
        boolean success = regionManager.createRegion(regionName, player, pos1, pos2);

        if (success) {
            player.sendMessage("§aРегион '" + regionName + "' успешно создан!");
            player.sendMessage("§7Границы: " + formatLocation(pos1) + " >> " + formatLocation(pos2));

            // Показываем меню успеха
            JsonMessageUtil.sendSuccessMenu(player, regionName);

            // Очищаем позиции после успешного создания
            posCommand.clearSelection(player);
        } else {
            player.sendMessage("§cНе удалось создать регион '" + regionName + "'!");
        }

        return true;
    }

    private String formatLocation(Location location) {
        if (location == null) return "null";
        return String.format("X: %d, Y: %d, Z: %d, World: %s",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                location.getWorld().getName());
    }

    private boolean isValidRegionName(String name) {
        return name.matches("[a-zA-Z0-9_]{3,16}");
    }
}