package org.KillerYT.regionMC.listeners;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RentManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class RentSignListener implements Listener {

    private final RegionMC plugin;

    public RentSignListener(RegionMC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        // Проверяем, что это клик правой кнопкой по блоку
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // Игнорируем, если игрок держит предмет (чтобы не мешать)
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Sign sign)) return;

        // Проверяем, что табличка имеет формат [Rent]
        String[] lines = sign.getLines();
        if (lines.length < 4) return;
        if (!lines[0].equalsIgnoreCase("[Rent]")) return;

        // Получаем RentManager
        RentManager rentManager = plugin.getRentManager();
        if (rentManager == null) {
            event.getPlayer().sendMessage("§cСистема аренды не инициализирована.");
            return;
        }

        // Обрабатываем клик
        rentManager.handleSignClick(event.getPlayer(), sign);
        event.setCancelled(true); // предотвращаем открытие редактора табличек
    }
}