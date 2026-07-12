package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionExpandCommand {
    private final RegionManager regionManager;
    private final RegionMC plugin;

    public RegionExpandCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭта команда только для игроков!");
            return true;
        }

        // Команде нужны РОВНО 2 аргумента: <блоки> <направление>
        if (args.length != 2) {
            showUsage(player);
            return true;
        }

        // Используем стандартные методы для получения позиций
        Location pos1 = regionManager.getPos1(player);
        Location pos2 = regionManager.getPos2(player);

        if (pos1 == null || pos2 == null) {
            player.sendMessage("§cУ вас не установлены обе позиции выделения!");
            player.sendMessage("§7Используйте §f/region pos1 §7и §f/region pos2 §7или палочку выделения");
            return true;
        }

        int blocks;
        String direction = args[1].toLowerCase();  // Второй аргумент - направление

        try {
            blocks = Integer.parseInt(args[0]);  // Первый аргумент - блоки
            if (blocks <= 0) {
                player.sendMessage("§cКоличество блоков должно быть положительным числом!");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cКоличество блоков должно быть числом!");
            return true;
        }

        return expandSelection(player, pos1, pos2, direction, blocks);
    }

    private void showUsage(Player player) {
        player.sendMessage("§cИспользование: §f/region expand <блоки> <направление>");
        player.sendMessage("§7Направления: up, down, north, south, east, west");
        player.sendMessage("§7Пример: §f/region expand 10 up");
    }

    private boolean expandSelection(Player player, Location pos1, Location pos2, String direction, int blocks) {
        // Создаем копии локаций для изменения
        Location newPos1 = pos1.clone();
        Location newPos2 = pos2.clone();

        // Расширяем границы в указанном направлении
        switch (direction.toLowerCase()) {
            case "up":
                newPos2.setY(Math.max(pos1.getY(), pos2.getY()) + blocks);
                break;
            case "down":
                newPos1.setY(Math.min(pos1.getY(), pos2.getY()) - blocks);
                break;
            case "north":
                newPos1.setZ(Math.min(pos1.getZ(), pos2.getZ()) - blocks);
                break;
            case "south":
                newPos2.setZ(Math.max(pos1.getZ(), pos2.getZ()) + blocks);
                break;
            case "east":
                newPos2.setX(Math.max(pos1.getX(), pos2.getX()) + blocks);
                break;
            case "west":
                newPos1.setX(Math.min(pos1.getX(), pos2.getX()) - blocks);
                break;
            default:
                player.sendMessage("§cНеизвестное направление: " + direction);
                player.sendMessage("§cДоступные направления: up, down, north, south, east, west");
                return true;  // ИЗМЕНИТЬ С false НА true!
        }

        // Проверяем размер выделения после расширения с учетом лимитов игрока
        int volume = calculateVolume(newPos1, newPos2);

        // Проверяем лимиты, если они включены
        if (regionManager.areLimitsEnabled()) {
            int maxSize = regionManager.getMaxRegionSizeForPlayer(player);

            // Проверяем только если есть лимит (не -1) и лимиты включены
            if (maxSize != -1 && volume > maxSize) {
                player.sendMessage("§cРазмер выделения слишком большой после расширения!");
                player.sendMessage("§cВаш лимит: §e" + maxSize + " §cблоков, новый размер: §e" + volume);
                return true;  // ИЗМЕНИТЬ С false НА true!
            }
        }

        // Обновляем позиции выделения игрока БЕЗ отправки сообщений
        regionManager.setPos1Silently(player, newPos1);
        regionManager.setPos2Silently(player, newPos2);

        // Простое сообщение об успехе
        player.sendMessage("§aВыделение расширено на " + blocks + " блоков в направлении " + direction + "!");

        return true;
    }

    /**
     * Вычисляет объем выделения
     */
    private int calculateVolume(Location pos1, Location pos2) {
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }
}