package org.KillerYT.regionMC.utils;

import org.KillerYT.regionMC.RegionMC;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class RegionNotifierManager {
    private final RegionMC plugin;

    // Добавляем конструктор
    public RegionNotifierManager(RegionMC plugin) {
        this.plugin = plugin;
    }

    // Существующий метод
    public void sendActionBarMessage(Player player, String message, int duration) {
        // Отправляем сообщение в action bar
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(message));

        // Удаляем сообщение через указанное время
        if (duration > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(""));
                }
            }.runTaskLater(plugin, duration); // Теперь plugin доступен
        }
    }

    // Добавляем недостающие методы
    public void notifyRegionEnter(Player player, String regionName) {
        String message = "§aВход в регион: §e" + regionName;
        sendActionBarMessage(player, message, 60);
    }

    public void testNotifications(Player player) {
        // Тестовые уведомления
        sendActionBarMessage(player, "§aТестовое уведомление 1", 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                sendActionBarMessage(player, "§bТестовое уведомление 2", 20);
            }
        }.runTaskLater(plugin, 40);

        new BukkitRunnable() {
            @Override
            public void run() {
                sendActionBarMessage(player, "§cТестовое уведомление 3", 20);
            }
        }.runTaskLater(plugin, 80);
    }
}