package org.KillerYT.regionMC.commands.subcommands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.Placeholders.PlaceholderAPIUtils;

public class TestPlaceholderCommand implements CommandExecutor {

    private final RegionMC plugin;

    public TestPlaceholderCommand(RegionMC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (!PlaceholderAPIUtils.isPlaceholderApiEnabled()) {
            player.sendMessage(ChatColor.RED + "❌ PlaceholderAPI не найден!");
            player.sendMessage(ChatColor.GRAY + "Установите PlaceholderAPI для работы плейсхолдеров");
            return true;
        }

        player.sendMessage(ChatColor.GOLD + "=== 🧪 Тест плейсхолдеров RegionMC ===");

        // Тестируем основные плейсхолдеры
        testPlaceholder(player, "current_name", "📍 Название региона");
        testPlaceholder(player, "current_owner", "👑 Владелец региона");
        testPlaceholder(player, "current_members", "👥 Участники региона");
        testPlaceholder(player, "current_priority", "⚡ Приоритет региона");
        testPlaceholder(player, "current_size", "📏 Размер региона");
        testPlaceholder(player, "current_world", "🌍 Мир региона");
        testPlaceholder(player, "player_can_build", "🛠️ Можно строить");
        testPlaceholder(player, "player_is_owner", "👑 Является владельцем");
        testPlaceholder(player, "player_is_member", "👥 Является участником");
        testPlaceholder(player, "player_regions_count", "📊 Количество регионов");
        testPlaceholder(player, "player_max_regions", "📈 Максимум регионов");
        testPlaceholder(player, "player_regions_remaining", "🎯 Осталось регионов");

        player.sendMessage(ChatColor.GREEN + "✅ Все плейсхолдеры протестированы!");
        player.sendMessage(ChatColor.GRAY + "Используйте: " + ChatColor.WHITE + "papi parse <игрок> %regionmc_placeholder%");

        return true;
    }

    private void testPlaceholder(Player player, String placeholder, String description) {
        try {
            String result = PlaceholderAPIUtils.setPlaceholders(player, "%regionmc_" + placeholder + "%");
            String status = result != null && !result.equals("null") ? "✅" : "❌";
            player.sendMessage(ChatColor.YELLOW + status + " " + description + ": " +
                    ChatColor.WHITE + (result != null ? result : "null"));
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "❌ " + description + ": ОШИБКА - " + e.getMessage());
        }
    }
}