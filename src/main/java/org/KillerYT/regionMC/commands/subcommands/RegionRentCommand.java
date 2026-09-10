package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.AdminManager;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.managers.RentManager;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegionRentCommand {

    private final RegionMC plugin;
    private final RegionManager regionManager;
    private final AdminManager adminManager;
    private final RentManager rentManager;

    public RegionRentCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.adminManager = plugin.getAdminManager();
        this.rentManager = plugin.getRentManager();
    }

    public void execute(CommandSender sender, String[] args) {
        if (rentManager == null) {
            sender.sendMessage("§cСистема аренды не инициализирована.");
            return;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return;
        }

        String sub = args[1].toLowerCase();
        // Убираем первые два аргумента ("rent" и подкоманду)
        String[] rest = Arrays.copyOfRange(args, 2, args.length);

        switch (sub) {
            case "price" -> handlePrice(sender, rest);
            case "info" -> handleInfo(sender, rest);
            case "cancel" -> handleCancel(sender, rest);
            case "extend" -> handleExtend(sender, rest);
            default -> {
                // Если первый аргумент не подкоманда, пробуем интерпретировать как:
                // /rg rent <регион> <количество><единица>
                handleRentDirect(sender, args);
            }
        }
    }

    public List<String> getTabCompletions(Player player, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Подкоманды
            String input = args[1].toLowerCase();
            List<String> subCommands = Arrays.asList("price", "info", "cancel", "extend");
            for (String cmd : subCommands) {
                if (cmd.startsWith(input)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 3) {
            String sub = args[1].toLowerCase();
            // Для всех подкоманд нужен регион
            String regionInput = args[2].toLowerCase();
            // Если игрок может видеть регионы (владелец, участник или админ)
            for (Region region : regionManager.getAllRegions()) {
                if (region.getName().toLowerCase().startsWith(regionInput)) {
                    // Фильтруем по правам (для команд, где нужны права)
                    if (sub.equals("price") || sub.equals("cancel")) {
                        // Для установки цены и отмены нужны права владельца/админа
                        if (region.isOwner(player.getUniqueId()) || adminManager.isAdmin(player)) {
                            completions.add(region.getName());
                        }
                    } else {
                        completions.add(region.getName());
                    }
                }
            }
        } else if (args.length == 4) {
            String sub = args[1].toLowerCase();
            if (sub.equals("price")) {
                // Третий аргумент - цена, четвёртый - единица
                String unitInput = args[3].toLowerCase();
                for (String unit : Arrays.asList("s", "m", "h", "d", "mo", "y")) {
                    if (unit.startsWith(unitInput)) {
                        completions.add(unit);
                    }
                }
            } else if (sub.equals("extend") || args.length == 4 && !sub.equals("price")) {
                // Для extend и прямой аренды: четвёртый аргумент - длительность (число+единица)
                // Можно предложить примеры
                completions.add("1d");
                completions.add("7d");
                completions.add("30d");
                completions.add("1h");
                completions.add("1mo");
                completions.add("1y");
            }
        }

        return completions;
    }

    // ==================== ОБРАБОТЧИКИ ====================

    private void handlePrice(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков.");
            return;
        }
        if (!hasPermission(player, "regionmc.rent.price")) {
            player.sendMessage("§cУ вас нет права устанавливать цену аренды.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage("§cИспользование: §f/rg rent price <регион> <цена> <единица>");
            return;
        }
        String regionName = args[0];
        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cЦена должна быть числом.");
            return;
        }
        String unit = args[2].toLowerCase();
        rentManager.setPrice(player, regionName, price, unit);
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cИспользование: §f/rg rent info <регион>");
            return;
        }
        String regionName = args[0];
        rentManager.getInfo(sender, regionName);
    }

    private void handleCancel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков.");
            return;
        }
        if (!hasPermission(player, "regionmc.rent.cancel")) {
            player.sendMessage("§cУ вас нет права отменять аренду.");
            return;
        }
        if (args.length < 1) {
            player.sendMessage("§cИспользование: §f/rg rent cancel <регион>");
            return;
        }
        String regionName = args[0];
        rentManager.cancelRent(player, regionName);
    }

    private void handleExtend(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков.");
            return;
        }
        if (!hasPermission(player, "regionmc.rent.extend")) {
            player.sendMessage("§cУ вас нет права продлевать аренду.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cИспользование: §f/rg rent extend <регион> <количество><единица>");
            player.sendMessage("§7Пример: §f/rg rent extend MyRegion 7d");
            return;
        }
        String regionName = args[0];
        String duration = args[1];
        int amount;
        String unit;
        try {
            Pattern pattern = Pattern.compile("(\\d+)([smhdmoy])");
            Matcher matcher = pattern.matcher(duration);
            if (!matcher.matches()) {
                player.sendMessage("§cНеверный формат. Используйте число и букву: 7d, 3h и т.д.");
                return;
            }
            amount = Integer.parseInt(matcher.group(1));
            unit = matcher.group(2).toLowerCase();
        } catch (Exception e) {
            player.sendMessage("§cОшибка в формате. Пример: 7d, 3h.");
            return;
        }
        rentManager.extendRent(player, regionName, amount, unit);
    }

    private void handleRentDirect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТолько для игроков.");
            return;
        }
        if (!hasPermission(player, "regionmc.rent.use")) {
            player.sendMessage("§cУ вас нет права арендовать регионы.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage("§cИспользование: §f/rg rent <регион> <количество><единица>");
            player.sendMessage("§7Пример: §f/rg rent MyRegion 7d");
            return;
        }
        String regionName = args[1];
        String duration = args[2];
        int amount;
        String unit;
        try {
            Pattern pattern = Pattern.compile("(\\d+)([smhdmoy])");
            Matcher matcher = pattern.matcher(duration);
            if (!matcher.matches()) {
                player.sendMessage("§cНеверный формат. Используйте число и букву: 7d, 3h и т.д.");
                return;
            }
            amount = Integer.parseInt(matcher.group(1));
            unit = matcher.group(2).toLowerCase();
        } catch (Exception e) {
            player.sendMessage("§cОшибка в формате. Пример: 7d, 3h.");
            return;
        }
        rentManager.rentRegion(player, regionName, amount, unit);
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6=== Аренда регионов ===");
        sender.sendMessage("§7/rg rent price <регион> <цена> <ед>  §f- установить цену");
        sender.sendMessage("§7/rg rent <регион> <кол-во><ед>   §f- арендовать на период");
        sender.sendMessage("§7/rg rent info <регион>           §f- информация об аренде");
        sender.sendMessage("§7/rg rent cancel <регион>        §f- отменить аренду");
        sender.sendMessage("§7/rg rent extend <регион> <период> §f- продлить аренду");
        sender.sendMessage("§7Единицы: s(сек), m(мин), h(час), d(день), mo(месяц), y(год)");
    }

    private boolean hasPermission(Player player, String permission) {
        return PermissionUtil.hasPermission(player, adminManager, permission);
    }
}