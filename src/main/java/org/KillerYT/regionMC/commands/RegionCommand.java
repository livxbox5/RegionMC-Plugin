package org.KillerYT.regionMC.commands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.commands.subcommands.*;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.managers.AdminManager;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.utils.DebugUtils;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

public class RegionCommand implements CommandExecutor, TabCompleter {
    private final RegionMC plugin;
    private final RegionManager regionManager;
    private final RegionShowHideCommand showHideCommand;
    private final RegionInfoCommand infoCommand;
    private final RegionFlagCommand flagCommand;
    private final RegionWandCommand wandCommand;
    private final RegionExpandCommand expandCommand;
    private final TestPlaceholderCommand testPlaceholderCommand;
    private final RegionExampleCommand exampleCommand;
    private final RegionRentCommand rentCommand;
    private final RegionHelpCommand helpCommand;
    private final AdminManager adminManager;
    private final DebugUtils debug = DebugUtils.getInstance();

    public RegionCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.adminManager = plugin.getAdminManager();
        this.showHideCommand = new RegionShowHideCommand(plugin);
        this.infoCommand = new RegionInfoCommand(regionManager, plugin);
        this.flagCommand = new RegionFlagCommand(plugin);
        this.wandCommand = new RegionWandCommand(plugin);
        this.expandCommand = new RegionExpandCommand(plugin);
        this.testPlaceholderCommand = new TestPlaceholderCommand(plugin);
        this.exampleCommand = new RegionExampleCommand(plugin);
        this.rentCommand = new RegionRentCommand(plugin);
        this.helpCommand = new RegionHelpCommand(plugin);
        debug.debug(DebugUtils.DebugCategory.COMMANDS, "RegionCommand инициализирован");
    }

    public void registerAllCommands() {
        try {
            registerCommandWithReflection(Arrays.asList("region", "rg"));
            plugin.getLogger().info("✓ All region commands registered successfully");
            debug.debug(DebugUtils.DebugCategory.COMMANDS, "Команды зарегистрированы с алиасами: region, rg");
        } catch (Exception e) {
            plugin.getLogger().warning("✗ Failed to register commands: " + e.getMessage());
            debug.error("Ошибка регистрации команд", e);
        }
    }

    private void registerCommandWithReflection(List<String> aliases) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand command = constructor.newInstance("regionmc", plugin);

            command.setDescription("Region management command");
            command.setUsage("/regionmc <subcommand>");
            command.setAliases(aliases);
            command.setExecutor(this);
            command.setTabCompleter(this);
            command.setPermission("regionmc.command.region");
            command.setPermissionMessage("§cУ вас нет прав на использование команд RegionMC!");

            commandMap.register(plugin.getName().toLowerCase(), command);
            plugin.getLogger().info("✓ Command 'regionmc' registered with aliases: " + aliases);

        } catch (Exception e) {
            plugin.getLogger().warning("✗ Failed to register command 'regionmc': " + e.getMessage());
        }
    }

    private boolean hasBasicAccess(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;

        if (adminManager != null && adminManager.isAdmin(player)) return true;

        if (player.isOp()) return true;
        if (player.hasPermission("*")) return true;
        if (player.hasPermission("regionmc.*")) return true;
        if (player.hasPermission("regionmc.admin")) return true;

        if (player.hasPermission("regionmc.command.*")) return true;
        if (player.hasPermission("regionmc.command.region")) return true;

        return false;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        if (!(sender instanceof Player player)) return true;
        return PermissionUtil.hasPermission(player, adminManager, permission);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasBasicAccess(sender)) {
            sender.sendMessage("§cУ вас нет прав на использование команд RegionMC!");
            return true;
        }

        if (args.length == 0) {
            if (!hasPermission(sender, "regionmc.command.help")) {
                sender.sendMessage("§cУ вас нет прав для просмотра помощи!");
                return true;
            }
            helpCommand.execute(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        try {
            switch (subCommand) {
                case "help" -> {
                    if (!hasPermission(sender, "regionmc.command.help")) {
                        sender.sendMessage("§cУ вас нет прав для просмотра помощи!");
                        return true;
                    }
                    helpCommand.execute(sender);
                }
                case "create", "claim" -> {
                    if (!hasPermission(sender, "regionmc.command.claim") && !hasPermission(sender, "regionmc.region.create")) {
                        sender.sendMessage("§cУ вас нет прав для создания регионов!");
                        return true;
                    }
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cЭта команда доступна только игрокам!");
                        return true;
                    }
                    handleCreate(player, args);
                }
                case "pos1", "pos2" -> {
                    if (!hasPermission(sender, "regionmc.command.pos")) {
                        sender.sendMessage("§cУ вас нет прав для установки позиций!");
                        return true;
                    }
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cЭта команда доступна только игрокам!");
                        return true;
                    }
                    if (subCommand.equals("pos1")) {
                        handlePos1(player);
                    } else {
                        handlePos2(player);
                    }
                }
                case "info" -> {
                    if (!hasPermission(sender, "regionmc.command.info")) {
                        sender.sendMessage("§cУ вас нет прав для просмотра информации!");
                        return true;
                    }
                    handleInfo(sender, args);
                }
                case "list" -> {
                    if (!hasPermission(sender, "regionmc.command.list")) {
                        sender.sendMessage("§cУ вас нет прав для просмотра списка регионов!");
                        return true;
                    }
                    handleList(sender);
                }
                case "flag" -> {
                    if (!hasPermission(sender, "regionmc.command.flag") && !hasPermission(sender, "regionmc.flags.*")) {
                        sender.sendMessage("§cУ вас нет прав для управления флагами!");
                        return true;
                    }
                    if (args.length >= 3 && args[2].equalsIgnoreCase("time-lock")) {
                        if (!(sender instanceof Player player)) {
                            sender.sendMessage("§cЭта команда доступна только игрокам!");
                            return true;
                        }
                        handleTimeLockFlag(player, args);
                        return true;
                    }
                    String[] flagArgs = Arrays.copyOfRange(args, 1, args.length);
                    flagCommand.execute(sender, flagArgs);
                }
                case "addmember" -> {
                    if (!hasPermission(sender, "regionmc.command.addmember")) {
                        sender.sendMessage("§cУ вас нет прав для добавления участников!");
                        return true;
                    }
                    handleAddMember(sender, args);
                }
                case "addowner" -> {
                    if (!hasPermission(sender, "regionmc.command.addowner")) {
                        sender.sendMessage("§cУ вас нет прав для добавления владельцев!");
                        return true;
                    }
                    handleAddOwner(sender, args);
                }
                case "removemember" -> {
                    if (!hasPermission(sender, "regionmc.command.removemember")) {
                        sender.sendMessage("§cУ вас нет прав для удаления участников!");
                        return true;
                    }
                    handleRemoveMember(sender, args);
                }
                case "removeowner" -> {
                    if (!hasPermission(sender, "regionmc.command.removeowner")) {
                        sender.sendMessage("§cУ вас нет прав для удаления владельцев!");
                        return true;
                    }
                    handleRemoveOwner(sender, args);
                }
                case "priority" -> {
                    if (!hasPermission(sender, "regionmc.command.priority")) {
                        sender.sendMessage("§cУ вас нет прав для изменения приоритета!");
                        return true;
                    }
                    handlePriority(sender, args);
                }
                case "delete", "remove" -> {
                    if (!hasPermission(sender, "regionmc.command.delete") && !hasPermission(sender, "regionmc.region.delete")) {
                        sender.sendMessage("§cУ вас нет прав для удаления регионов!");
                        return true;
                    }
                    handleDelete(sender, args);
                }
                case "wand" -> {
                    if (!hasPermission(sender, "regionmc.command.wand")) {
                        sender.sendMessage("§cУ вас нет прав для получения палочки выделения!");
                        return true;
                    }
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cЭта команда доступна только игрокам!");
                        return true;
                    }
                    handleWand(player);
                }
                case "expand" -> {
                    if (!hasPermission(sender, "regionmc.command.expand")) {
                        sender.sendMessage("§cУ вас нет прав для расширения регионов!");
                        return true;
                    }
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cЭта команда доступна только игрокам!");
                        return true;
                    }
                    if (args.length >= 3) {
                        expandCommand.execute(sender, new String[]{args[1], args[2]});
                    } else {
                        sender.sendMessage("§cИспользование: §f/rg expand <блоки> <направление>");
                        sender.sendMessage("§7Направления: up, down, north, south, east, west");
                        sender.sendMessage("§7Пример: §f/rg expand 10 up");
                    }
                }
                case "show", "hide" -> {
                    if (!hasPermission(sender, "regionmc.command.show")) {
                        sender.sendMessage("§cУ вас нет прав для показа/скрытия границ регионов!");
                        return true;
                    }
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cЭта команда доступна только игрокам!");
                        return true;
                    }
                    showHideCommand.execute(player, args);
                }
                case "testplaceholders", "testpapi" -> {
                    if (!hasPermission(sender, "regionmc.command.testplaceholders")) {
                        sender.sendMessage("§cУ вас нет прав для тестирования плейсхолдеров!");
                        return true;
                    }
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("§cЭта команда доступна только игрокам!");
                        return true;
                    }
                    testPlaceholderCommand.onCommand(sender, command, label, new String[0]);
                }
                case "example", "examples" -> {
                    if (!hasPermission(sender, "regionmc.command.example")) {
                        sender.sendMessage("§cУ вас нет прав для просмотра примеров!");
                        return true;
                    }
                    exampleCommand.execute(sender, args);
                }
                case "reload" -> {
                    if (!hasPermission(sender, "regionmc.command.reload")) {
                        sender.sendMessage("§cУ вас нет прав для перезагрузки плагина!");
                        return true;
                    }
                    handleReload(sender);
                }
                case "limit" -> {
                    if (!hasPermission(sender, "regionmc.command.limit")) {
                        sender.sendMessage("§cУ вас нет прав для просмотра лимитов регионов!");
                        return true;
                    }
                    handleLimit(sender);
                }
                case "rent" -> {
                    if (!hasPermission(sender, "regionmc.command.rent")) {
                        sender.sendMessage("§cУ вас нет прав на использование аренды!");
                        return true;
                    }
                    rentCommand.execute(sender, args);
                }
                default -> sender.sendMessage("§cНеизвестная подкоманда! Используйте §f/" + label + " help");
            }
        } catch (Exception e) {
            sender.sendMessage("§cПроизошла ошибка при выполнении команды: " + e.getMessage());
            plugin.getLogger().warning("Ошибка в команде " + subCommand + ": " + e.getMessage());
            plugin.getLogger().warning("Stack trace: " + Arrays.toString(e.getStackTrace()));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (!hasBasicAccess(sender)) {
            return Collections.emptyList();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> availableCommands = new ArrayList<>();

            if (hasPermission(sender, "regionmc.command.help"))
                availableCommands.add("help");
            if (hasPermission(sender, "regionmc.command.claim") || hasPermission(sender, "regionmc.region.create"))
                availableCommands.addAll(Arrays.asList("create", "claim"));
            if (hasPermission(sender, "regionmc.command.pos"))
                availableCommands.addAll(Arrays.asList("pos1", "pos2"));
            if (hasPermission(sender, "regionmc.command.info"))
                availableCommands.add("info");
            if (hasPermission(sender, "regionmc.command.list"))
                availableCommands.add("list");
            if (hasPermission(sender, "regionmc.command.flag") || hasPermission(sender, "regionmc.flags.*"))
                availableCommands.add("flag");
            if (hasPermission(sender, "regionmc.command.addmember"))
                availableCommands.add("addmember");
            if (hasPermission(sender, "regionmc.command.addowner"))
                availableCommands.add("addowner");
            if (hasPermission(sender, "regionmc.command.removemember"))
                availableCommands.add("removemember");
            if (hasPermission(sender, "regionmc.command.removeowner"))
                availableCommands.add("removeowner");
            if (hasPermission(sender, "regionmc.command.priority"))
                availableCommands.add("priority");
            if (hasPermission(sender, "regionmc.command.delete") || hasPermission(sender, "regionmc.region.delete"))
                availableCommands.addAll(Arrays.asList("delete", "remove"));
            if (hasPermission(sender, "regionmc.command.wand"))
                availableCommands.add("wand");
            if (hasPermission(sender, "regionmc.command.expand"))
                availableCommands.add("expand");
            if (hasPermission(sender, "regionmc.command.show"))
                availableCommands.addAll(Arrays.asList("show", "hide"));
            if (hasPermission(sender, "regionmc.command.reload"))
                availableCommands.add("reload");
            if (hasPermission(sender, "regionmc.command.testplaceholders"))
                availableCommands.addAll(Arrays.asList("testplaceholders", "testpapi"));
            if (hasPermission(sender, "regionmc.command.limit"))
                availableCommands.add("limit");
            // ИСПРАВЛЕНО: отдельное право для example, и убран examples
            if (hasPermission(sender, "regionmc.command.example"))
                availableCommands.add("example");
            if (hasPermission(sender, "regionmc.command.rent"))
                availableCommands.add("rent");

            String input = args[0].toLowerCase();
            for (String cmd : availableCommands) {
                if (cmd.toLowerCase().startsWith(input)) {
                    completions.add(cmd);
                }
            }

        } else if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();

            if ("flag".equals(subCommand)) {
                if (!hasPermission(sender, "regionmc.command.flag") && !hasPermission(sender, "regionmc.flags.*")) {
                    return Collections.emptyList();
                }
                if (args.length == 3 && args[2].toLowerCase().startsWith("time")) {
                    completions.add("time-lock");
                } else if (args.length == 4 && args[2].equalsIgnoreCase("time-lock")) {
                    String valueInput = args[3].toLowerCase();
                    for (String value : Arrays.asList("day", "night", "allow", "deny")) {
                        if (value.startsWith(valueInput)) {
                            completions.add(value);
                        }
                    }
                } else {
                    String[] flagArgs = Arrays.copyOfRange(args, 1, args.length);
                    return flagCommand.getTabCompleteSuggestions(player, flagArgs);
                }
            }

            if ("rent".equals(subCommand)) {
                if (hasPermission(sender, "regionmc.command.rent")) {
                    return rentCommand.getTabCompletions(player, args);
                }
                return Collections.emptyList();
            }

            switch (subCommand) {
                case "info":
                    if (!hasPermission(sender, "regionmc.command.info")) return Collections.emptyList();
                case "addmember":
                    if (!hasPermission(sender, "regionmc.command.addmember")) return Collections.emptyList();
                case "addowner":
                    if (!hasPermission(sender, "regionmc.command.addowner")) return Collections.emptyList();
                case "removemember":
                    if (!hasPermission(sender, "regionmc.command.removemember")) return Collections.emptyList();
                case "removeowner":
                    if (!hasPermission(sender, "regionmc.command.removeowner")) return Collections.emptyList();
                case "priority":
                    if (!hasPermission(sender, "regionmc.command.priority")) return Collections.emptyList();
                case "delete", "remove":
                    if (!hasPermission(sender, "regionmc.command.delete") && !hasPermission(sender, "regionmc.region.delete"))
                        return Collections.emptyList();
                case "show", "hide":
                    if (!hasPermission(sender, "regionmc.command.show")) return Collections.emptyList();
                case "limit":
                    if (!hasPermission(sender, "regionmc.command.limit")) return Collections.emptyList();

                    if (args.length == 2) {
                        List<String> playerRegions = regionManager.getPlayerRegionNames(player.getUniqueId());
                        String regionInput = args[1].toLowerCase();
                        for (String region : playerRegions) {
                            if (region.toLowerCase().startsWith(regionInput)) {
                                completions.add(region);
                            }
                        }
                        if ("hide".equals(subCommand)) {
                            completions.add("all");
                        }
                    } else if (args.length == 3) {
                        if (subCommand.matches("addmember|addowner|removemember|removeowner")) {
                            String playerInput = args[2].toLowerCase();
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                if (onlinePlayer.getName().toLowerCase().startsWith(playerInput)) {
                                    completions.add(onlinePlayer.getName());
                                }
                            }
                        }
                    }
                    break;
                case "expand":
                    if (!hasPermission(sender, "regionmc.command.expand")) return Collections.emptyList();
                    if (args.length == 2) {
                        String blocksInput = args[1].toLowerCase();
                        for (String blockCount : Arrays.asList("1", "5", "10", "16", "32", "64")) {
                            if (blockCount.startsWith(blocksInput)) {
                                completions.add(blockCount);
                            }
                        }
                    } else if (args.length == 3) {
                        String[] directions = {"up", "down", "north", "south", "east", "west"};
                        String dirInput = args[2].toLowerCase();
                        for (String dir : directions) {
                            if (dir.startsWith(dirInput)) {
                                completions.add(dir);
                            }
                        }
                    }
                    break;
            }
        }

        return completions;
    }

    // ==================== ОБРАБОТЧИКИ КОМАНД ====================

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cИспользование: §f/regionmc create <имя>");
            return;
        }
        String regionName = args[1];
        boolean success = regionManager.createRegion(regionName, player);
        if (success) {
            player.sendMessage("§aРегион '" + regionName + "' успешно создан!");
        } else {
            player.sendMessage("§cНе удалось создать регион! Возможно, позиции не установлены или регион с таким именем уже существует.");
        }
    }

    private void handlePos1(Player player) {
        regionManager.setPos1(player, player.getLocation());
        player.sendMessage("§aПозиция 1 установлена!");
    }

    private void handlePos2(Player player) {
        regionManager.setPos2(player, player.getLocation());
        player.sendMessage("§aПозиция 2 установлена!");
    }

    private void handleWand(Player player) {
        wandCommand.execute(player);
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: §f/regionmc info <имя региона>");
            return;
        }
        String regionName = args[1];
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            sender.sendMessage("§cРегион '" + regionName + "' не найден!");
            return;
        }
        if (sender instanceof Player player) {
            if (!regionManager.isOwner(player, regionName) &&
                    !regionManager.isMember(player, regionName) &&
                    !hasPermission(sender, "regionmc.admin")) {
                sender.sendMessage("§cУ вас нет прав для просмотра информации об этом регионе!");
                return;
            }
        }
        sender.sendMessage("§6=== Информация о регионе " + regionName + " ===");
        sender.sendMessage("§7Владельцы: " + region.getOwners().size());
        sender.sendMessage("§7Участники: " + region.getMembers().size());
        sender.sendMessage("§7Приоритет: " + region.getPriority());
        sender.sendMessage("§7Размер: " + region.getVolume() + " блоков");
        if (region.getWorld() != null) {
            sender.sendMessage("§7Мир: " + region.getWorld().getName());
        }
        sender.sendMessage("§7Флаги: " + region.getFlags().size() + " установлено");
    }

    private void handleList(CommandSender sender) {
        Collection<Region> allRegions = regionManager.getAllRegions();
        if (allRegions.isEmpty()) {
            sender.sendMessage("§eРегионов нет.");
            return;
        }
        sender.sendMessage("§6=== Список всех регионов (" + allRegions.size() + ") ===");
        for (Region region : allRegions) {
            sender.sendMessage("§e- " + region.getName() + " §7(приоритет: " + region.getPriority() + ")");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cИспользование: §f/regionmc delete <имя региона>");
            return;
        }
        String regionName = args[1];
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            sender.sendMessage("§cРегион '" + regionName + "' не найден!");
            return;
        }
        if (sender instanceof Player player) {
            if (!region.isOwner(player.getUniqueId()) && !hasPermission(sender, "regionmc.admin")) {
                sender.sendMessage("§cВы не являетесь владельцем этого региона!");
                return;
            }
        }
        boolean success = regionManager.removeRegion(regionName);
        if (success) {
            sender.sendMessage("§aРегион '" + regionName + "' успешно удален!");
        } else {
            sender.sendMessage("§cНе удалось удалить регион '" + regionName + "'!");
        }
    }

    private void handleAddMember(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользование: §f/regionmc addmember <регион> <игрок>");
            return;
        }
        String regionName = args[1];
        String targetPlayer = args[2];
        if (sender instanceof Player player) {
            if (!regionManager.canModifyRegion(player, regionName)) {
                sender.sendMessage("§cУ вас нет прав для изменения этого региона!");
                return;
            }
        }
        boolean success = regionManager.addMember(regionName, targetPlayer);
        if (success) {
            sender.sendMessage("§aИгрок " + targetPlayer + " добавлен как участник региона " + regionName);
        } else {
            sender.sendMessage("§cНе удалось добавить участника! Проверьте название региона и ваши права.");
        }
    }

    private void handleAddOwner(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользование: §f/regionmc addowner <регион> <игрок>");
            return;
        }
        String regionName = args[1];
        String targetPlayer = args[2];
        if (sender instanceof Player player) {
            if (!regionManager.canModifyRegion(player, regionName)) {
                sender.sendMessage("§cУ вас нет прав для изменения этого региона!");
                return;
            }
        }
        boolean success = regionManager.addOwner(regionName, targetPlayer);
        if (success) {
            sender.sendMessage("§aИгрок " + targetPlayer + " добавлен как владелец региона " + regionName);
        } else {
            sender.sendMessage("§cНе удалось добавить владельца! Проверьте название региона и ваши права.");
        }
    }

    private void handleRemoveMember(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользование: §f/regionmc removemember <регион> <игрок>");
            return;
        }
        String regionName = args[1];
        String targetPlayer = args[2];
        if (sender instanceof Player player) {
            if (!regionManager.canModifyRegion(player, regionName)) {
                sender.sendMessage("§cУ вас нет прав для изменения этого региона!");
                return;
            }
        }
        boolean success = regionManager.removeMember(regionName, targetPlayer);
        if (success) {
            sender.sendMessage("§aИгрок " + targetPlayer + " удален из участников региона " + regionName);
        } else {
            sender.sendMessage("§cНе удалось удалить участника! Проверьте название региона и ваши права.");
        }
    }

    private void handleRemoveOwner(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользование: §f/regionmc removeowner <регион> <игрок>");
            return;
        }
        String regionName = args[1];
        String targetPlayer = args[2];
        if (sender instanceof Player player) {
            if (!regionManager.canModifyRegion(player, regionName)) {
                sender.sendMessage("§cУ вас нет прав для изменения этого региона!");
                return;
            }
        }
        boolean success = regionManager.removeOwner(regionName, targetPlayer);
        if (success) {
            sender.sendMessage("§aИгрок " + targetPlayer + " удален из владельцев региона " + regionName);
        } else {
            sender.sendMessage("§cНе удалось удалить владельца! Проверьте название региона и ваши права.");
        }
    }

    private void handlePriority(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cИспользование: §f/regionmc priority <регион> <приоритет>");
            return;
        }
        String regionName = args[1];
        int priority;
        try {
            priority = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cПриоритет должен быть числом!");
            return;
        }
        if (sender instanceof Player player) {
            if (!regionManager.canModifyRegion(player, regionName)) {
                sender.sendMessage("§cУ вас нет прав для изменения этого региона!");
                return;
            }
        }
        boolean success = regionManager.setPriority(regionName, priority);
        if (success) {
            sender.sendMessage("§aПриоритет региона " + regionName + " установлен в: " + priority);
        } else {
            sender.sendMessage("§cНе удалось установить приоритет! Проверьте название региона и ваши права.");
        }
    }

    private void handleLimit(CommandSender sender) {
        if (sender instanceof Player player) {
            regionManager.showLimitInfo(player);
        } else {
            sender.sendMessage("§6=== Информация о лимитах ===");
            sender.sendMessage("§7Лимиты включены: " + (regionManager.areLimitsEnabled() ? "§aДа" : "§cНет"));
            if (regionManager.areLimitsEnabled()) {
                sender.sendMessage("§7Минимальный размер региона: " + regionManager.getMinRegionSize());
                int maxHeight = regionManager.getMaxRegionHeight();
                sender.sendMessage("§7Максимальная высота: " + (maxHeight == Integer.MAX_VALUE ? "безлимитно" : maxHeight));
                sender.sendMessage("§7Максимальный размер по умолчанию: " + regionManager.getDefaultLimit());
            }
        }
    }

    private void handleReload(CommandSender sender) {
        RegionReloadCommand reloadCommand = new RegionReloadCommand(plugin);
        reloadCommand.execute(sender, new String[0]);
    }

    private void handleTimeLockFlag(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§cИспользование: /rg flag <регион> time-lock <day|night|allow|deny>");
            return;
        }

        String regionName = args[1];
        String value = args[3].toLowerCase();

        if (!value.equals("day") && !value.equals("night") && !value.equals("allow") && !value.equals("deny")) {
            player.sendMessage("§cНедопустимое значение! Используйте: day, night, allow или deny");
            player.sendMessage("§7day - всегда день в регионе (визуально)");
            player.sendMessage("§7night - всегда ночь в регионе (визуально)");
            player.sendMessage("§7allow - разрешить изменение времени (стандартное поведение)");
            player.sendMessage("§7deny - заморозить текущее время (визуально)");
            return;
        }

        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            player.sendMessage("§cРегион '" + regionName + "' не найден!");
            return;
        }

        if (!regionManager.canModifyRegion(player, regionName)) {
            player.sendMessage("§cУ вас нет прав для изменения флагов этого региона!");
            return;
        }

        region.setFlag("time-lock", value);
        regionManager.saveRegion(region);

        if (plugin.getPlayerTimeManager() != null) {
            plugin.getPlayerTimeManager().handleRegionTimeLockChange(region);
        }

        switch (value) {
            case "day" -> {
                player.sendMessage("§aВремя установлено на §eДЕНЬ§a в регионе §6" + region.getName());
                player.sendMessage("§7Теперь в этом регионе игроки будут видеть день (6000 тиков)");
                player.sendMessage("§7⚠ Это только визуальный эффект для игроков в регионе!");
            }
            case "night" -> {
                player.sendMessage("§aВремя установлено на §9НОЧЬ§a в регионе §6" + region.getName());
                player.sendMessage("§7Теперь в этом регионе игроки будут видеть ночь (18000 тиков)");
                player.sendMessage("§7⚠ Это только визуальный эффект для игроков в регионе!");
            }
            case "allow" -> {
                player.sendMessage("§aИзменение времени §2РАЗРЕШЕНО§a в регионе §6" + region.getName());
                player.sendMessage("§7Теперь время отображается стандартным образом");
            }
            case "deny" -> {
                player.sendMessage("§aВремя §6ЗАМОРОЖЕНО§a в регионе §6" + region.getName());
                player.sendMessage("§7Теперь время будет заморожено на текущем значении");
                player.sendMessage("§7⚠ Это только визуальный эффект для игроков в регионе!");
            }
        }
    }
}