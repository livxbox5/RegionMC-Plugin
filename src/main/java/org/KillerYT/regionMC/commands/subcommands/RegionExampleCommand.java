package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.utils.LanguageManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegionExampleCommand {

    private final RegionMC plugin;
    private final LanguageManager languageManager;

    public RegionExampleCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭта команда доступна только игрокам!");
            return true;
        }

        // Отправляем заголовок
        player.sendMessage(languageManager.getMessage("example.header"));
        player.sendMessage("");

        // Базовые команды
        player.sendMessage(languageManager.getMessage("example.basic-commands"));
        player.sendMessage(languageManager.getMessage("example.basic-pos1"));
        player.sendMessage(languageManager.getMessage("example.basic-pos2"));
        player.sendMessage(languageManager.getMessage("example.basic-claim"));
        player.sendMessage("");

        // Пример создания региона
        player.sendMessage(languageManager.getMessage("example.create-example"));
        player.sendMessage(languageManager.getMessage("example.create-step1"));
        player.sendMessage(languageManager.getMessage("example.create-step2"));
        player.sendMessage(languageManager.getMessage("example.create-step3"));
        player.sendMessage("");

        // Расширение региона
        player.sendMessage(languageManager.getMessage("example.expand"));
        player.sendMessage(languageManager.getMessage("example.expand-up"));
        player.sendMessage(languageManager.getMessage("example.expand-down"));
        player.sendMessage(languageManager.getMessage("example.expand-north"));
        player.sendMessage(languageManager.getMessage("example.expand-south"));
        player.sendMessage(languageManager.getMessage("example.expand-east"));
        player.sendMessage(languageManager.getMessage("example.expand-west"));
        player.sendMessage("");

        // Управление участниками
        player.sendMessage(languageManager.getMessage("example.member-management"));
        player.sendMessage(languageManager.getMessage("example.addmember"));
        player.sendMessage(languageManager.getMessage("example.removemember"));
        player.sendMessage(languageManager.getMessage("example.addowner"));
        player.sendMessage(languageManager.getMessage("example.removeowner"));
        player.sendMessage("");

        // Флаги региона
        player.sendMessage(languageManager.getMessage("example.flags"));
        player.sendMessage(languageManager.getMessage("example.flag-day"));
        player.sendMessage(languageManager.getMessage("example.flag-night"));
        player.sendMessage(languageManager.getMessage("example.flag-allow"));
        player.sendMessage(languageManager.getMessage("example.flag-deny"));
        player.sendMessage("");

        // Информация и управление
        player.sendMessage(languageManager.getMessage("example.info-management"));
        player.sendMessage(languageManager.getMessage("example.list"));
        player.sendMessage(languageManager.getMessage("example.info"));
        player.sendMessage(languageManager.getMessage("example.priority"));
        player.sendMessage(languageManager.getMessage("example.delete"));
        player.sendMessage(languageManager.getMessage("example.show"));
        player.sendMessage(languageManager.getMessage("example.hide"));
        player.sendMessage("");

        // Дополнительно
        player.sendMessage(languageManager.getMessage("example.additional"));
        player.sendMessage(languageManager.getMessage("example.wand"));
        player.sendMessage(languageManager.getMessage("example.limit"));
        player.sendMessage(languageManager.getMessage("example.reload"));
        player.sendMessage("");

        // Совет
        player.sendMessage(languageManager.getMessage("example.tip"));

        return true;
    }
}