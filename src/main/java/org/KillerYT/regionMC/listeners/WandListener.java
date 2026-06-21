package org.KillerYT.regionMC.listeners;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.commands.subcommands.RegionWandCommand;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class WandListener implements Listener {

    private final RegionMC plugin;
    private final RegionWandCommand wandCommand;
    private final RegionManager regionManager;

    public WandListener(RegionMC plugin, RegionWandCommand wandCommand) {
        this.plugin = plugin;
        this.wandCommand = wandCommand;
        this.regionManager = plugin.getRegionManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !wandCommand.isWandItem(item)) {
            return;
        }

        event.setCancelled(true);

        // Проверка прав
        if (!player.hasPermission("regionmc.command.wand") &&
                !player.hasPermission("regionmc.admin") &&
                !regionManager.isAdminBypass(player)) {
            player.sendMessage(ColorUtil.colorize("&cУ вас нет прав для использования палочки выделения!"));
            return;
        }

        Location location = getTargetLocation(player, event.getClickedBlock());
        if (location == null) {
            player.sendMessage(ColorUtil.colorize("&cНе удалось определить позицию!"));
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) {
            regionManager.setPos1(player, location);
            // УДАЛЕНО: player.sendMessage(...) - сообщение отправляется внутри regionManager.setPos1()
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (player.isSneaking()) {
                showSelectionInfo(player);
            } else {
                regionManager.setPos2(player, location);
                // УДАЛЕНО: player.sendMessage(...) - сообщение отправляется внутри regionManager.setPos2()
            }
        }
    }

    private Location getTargetLocation(Player player, Block clickedBlock) {
        if (clickedBlock != null && clickedBlock.getType() != Material.AIR) {
            return clickedBlock.getLocation();
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock != null && targetBlock.getType() != Material.AIR) {
            return targetBlock.getLocation();
        }

        return player.getLocation().getBlock().getLocation();
    }

    private void showSelectionInfo(Player player) {
        Location pos1 = regionManager.getPos1(player);
        Location pos2 = regionManager.getPos2(player);

        player.sendMessage(ColorUtil.colorize("&6=== Информация о выделении ==="));

        if (pos1 != null) {
            player.sendMessage(ColorUtil.colorize("&aPos1: &7X: " + pos1.getBlockX() + " Y: " + pos1.getBlockY() + " Z: " + pos1.getBlockZ()));
        } else {
            player.sendMessage(ColorUtil.colorize("&cPos1: &7не установлена"));
        }

        if (pos2 != null) {
            player.sendMessage(ColorUtil.colorize("&aPos2: &7X: " + pos2.getBlockX() + " Y: " + pos2.getBlockY() + " Z: " + pos2.getBlockZ()));
        } else {
            player.sendMessage(ColorUtil.colorize("&cPos2: &7не установлена"));
        }

        if (pos1 != null && pos2 != null) {
            int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
            int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            int length = maxZ - minZ + 1;
            int volume = width * height * length;

            player.sendMessage(ColorUtil.colorize("&7Размеры: &e" + width + "×" + height + "×" + length + " (&6" + volume + " блоков&7)"));
            player.sendMessage(ColorUtil.colorize("&a✓ Готово к созданию региона! Используйте &e/rg create <название>"));
        } else {
            player.sendMessage(ColorUtil.colorize("&7Установите обе позиции для создания региона"));
        }
    }
}