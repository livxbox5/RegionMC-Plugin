package org.KillerYT.regionMC.listeners;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.region.ColorUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class InfoListener implements Listener {

    private final RegionMC plugin;
    private final RegionManager regionManager;

    public InfoListener(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Проверяем, что это палка (STICK)
        if (item == null || item.getType() != Material.STICK) {
            return;
        }

        // Проверяем, что игрок нажал ПКМ по блоку
        if (event.getClickedBlock() == null || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Проверка прав
        if (!player.hasPermission("regionmc.command.info") &&
                !player.hasPermission("regionmc.admin") &&
                !regionManager.isAdminBypass(player)) {
            player.sendMessage(ColorUtil.colorize("&cУ вас нет прав для проверки регионов!"));
            return;
        }

        event.setCancelled(true);

        Location location = event.getClickedBlock().getLocation();
        Region region = regionManager.getRegionAtLocation(location);

        if (region != null) {
            showRegionInfo(player, region);
        } else {
            player.sendMessage(ColorUtil.colorize("&cВ этом месте нет региона!"));
        }
    }

    private void showRegionInfo(Player player, Region region) {
        player.sendMessage(ColorUtil.colorize("&6=========== " + region.getName() + " ============"));
        player.sendMessage(ColorUtil.colorize("&6🎯 Приоритет: &f" + region.getPriority()));
        player.sendMessage(ColorUtil.colorize("&6📦 Объем: &f" + region.getVolume() + " блоков"));

        if (region.getWorld() != null) {
            player.sendMessage(ColorUtil.colorize("&6🌍 Мир: &f" + region.getWorld().getName()));
        }

        player.sendMessage(ColorUtil.colorize("&6👑 Владельцев: &f" + region.getOwners().size()));
        player.sendMessage(ColorUtil.colorize("&6👥 Участников: &f" + region.getMembers().size()));

        Location pos1 = region.getPos1();
        Location pos2 = region.getPos2();
        if (pos1 != null && pos2 != null) {
            player.sendMessage(ColorUtil.colorize("&6📐 Границы: &f" +
                    pos1.getBlockX() + "," + pos1.getBlockY() + "," + pos1.getBlockZ() +
                    " &8→ " +
                    pos2.getBlockX() + "," + pos2.getBlockY() + "," + pos2.getBlockZ()));
        }

        player.sendMessage(ColorUtil.colorize("&7================================="));
    }
}