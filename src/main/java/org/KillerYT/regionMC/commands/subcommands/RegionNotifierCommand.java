package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.utils.RegionNotifierManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.Arrays;

public class RegionNotifierCommand {
    private final RegionMC plugin;
    private final RegionNotifierManager notifier;

    public RegionNotifierCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.notifier = new RegionNotifierManager(plugin); // Передаем plugin
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (args.length == 0) {
            showUsage(player);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "test":
                testNotification(player);
                break;
            case "message":
                if (args.length >= 2) {
                    String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    // Теперь передаем три параметра: игрок, сообщение и длительность (60 тиков = 3 секунды)
                    notifier.sendActionBarMessage(player, message, 60);

                    // Используем LanguageManager для сообщения об успехе - FIXED: use player.getName()
                    String successMessage = plugin.getLanguageManager().getMessage(player.getName(), "notifier.message-sent");
                    player.sendMessage(colorize(successMessage));
                } else {
                    // Используем LanguageManager для сообщения об использовании - FIXED: use player.getName()
                    String usageMessage = plugin.getLanguageManager().getMessage(player.getName(), "notifier.usage");
                    player.sendMessage(colorize(usageMessage));
                }
                break;
            default:
                showUsage(player);
                break;
        }

        return true;
    }

    private void showUsage(Player player) {
        // Используем LanguageManager для заголовка помощи - FIXED: use player.getName()
        String header = plugin.getLanguageManager().getMessage(player.getName(), "help.header");
        player.sendMessage(colorize(header));

        player.sendMessage(colorize("&e/region notifier test &7- " +
                plugin.getLanguageManager().getMessage(player.getName(), "help.notifier-test"))); // FIXED
        player.sendMessage(colorize("&e/region notifier message <текст> &7- " +
                plugin.getLanguageManager().getMessage(player.getName(), "help.notifier-message"))); // FIXED
    }

    private void testNotification(Player player) {
        String currentRegion = plugin.getRegionManager().findRegionAtLocation(player.getLocation());

        if (currentRegion != null) {
            notifier.notifyRegionEnter(player, currentRegion);
            // FIXED: use player.getName()
            String message = plugin.getLanguageManager().getMessage(player.getName(), "notifier.test-started");
            player.sendMessage(colorize(message));
        } else {
            // Тестовые уведомления
            notifier.testNotifications(player);
            // FIXED: use player.getName()
            String message = plugin.getLanguageManager().getMessage(player.getName(), "notifier.test-completed");
            player.sendMessage(colorize(message));
        }
    }

    /**
     * Вспомогательный метод для цветов (альтернатива deprecated ChatColor)
     */
    private String colorize(String message) {
        return message.replace('&', '§');
    }
}