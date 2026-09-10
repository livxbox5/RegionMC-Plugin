package org.KillerYT.regionMC.commands.subcommands;

import net.kyori.adventure.text.Component;
import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.*;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
//import org.KillerYT.regionMC.utils.JsonMessageUtil;
import org.killeryt.killerCoreAPI.utils.message.JsonMessageUtil;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

public class RegionFlagCommand {
    private final FlagManager flagManager;
    private final RegionManager regionManager;
    private final RegionMC plugin;

    public RegionFlagCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.flagManager = plugin.getFlagManager();
        this.regionManager = plugin.getRegionManager();
    }

    /**
     * Получить список предложений для автодополнения
     */
    public List<String> getTabCompleteSuggestions(Player player, String[] args) {
        List<String> suggestions = new ArrayList<>();

        // /rg flag [TAB] - пустой ввод
        if (args.length == 0) {
            // Предлагаем команду list
            suggestions.add("list");

            // Предлагаем регионы игрока где он owner
            List<String> playerRegions = regionManager.getPlayerRegionNames(player.getUniqueId());
            suggestions.addAll(playerRegions);
            return suggestions;
        }

        // /rg flag <первый_аргумент> [TAB]
        if (args.length == 1) {
            String firstArg = args[0].toLowerCase();

            // Если введен "list" - не предлагаем ничего
            if ("list".equalsIgnoreCase(firstArg)) {
                return Collections.emptyList();
            }

            // Фильтруем регионы игрока по введенному тексту
            List<String> playerRegions = regionManager.getPlayerRegionNames(player.getUniqueId());
            for (String regionName : playerRegions) {
                if (regionName.toLowerCase().startsWith(firstArg)) {
                    suggestions.add(regionName);
                }
            }
            return suggestions;
        }

        // /rg flag <регион> [TAB]
        if (args.length == 2) {
            String regionName = args[0];
            String currentInput = args[1].toLowerCase();

            // Проверяем существование региона
            Region region = regionManager.getRegion(regionName);
            if (region == null) {
                // Регион не найден - предлагаем другие регионы игрока
                List<String> playerRegions = regionManager.getPlayerRegionNames(player.getUniqueId());
                for (String r : playerRegions) {
                    if (r.toLowerCase().startsWith(currentInput)) {
                        suggestions.add(r);
                    }
                }
                return suggestions;
            }

            // Проверяем права игрока на регион
            if (!region.isOwner(player.getUniqueId())) {
                return suggestions; // Нет прав - не предлагаем флаги
            }

            // Получаем все доступные флаги с правами
            List<String> availableFlags = flagManager.getFlagNamesWithPermission(player);

            // Фильтруем по уже введенному тексту
            for (String flagName : availableFlags) {
                if (flagName.toLowerCase().startsWith(currentInput)) {
                    suggestions.add(flagName);
                }
            }

            // Добавляем специальные команды если они начинаются с введенного текста
            if ("allowall".startsWith(currentInput)) {
                suggestions.add("allowall");
            }
            if ("denyall".startsWith(currentInput)) {
                suggestions.add("denyall");
            }

            // Добавляем номера страниц для пагинации
            for (int i = 1; i <= 5; i++) {
                if (String.valueOf(i).startsWith(currentInput)) {
                    suggestions.add(String.valueOf(i));
                }
            }
            return suggestions;
        }

        // /rg flag <регион> <флаг> [TAB]
        if (args.length == 3) {
            String regionName = args[0];
            String flagName = args[1];
            String currentInput = args[2].toLowerCase();

            // Проверяем регион
            Region region = regionManager.getRegion(regionName);
            if (region == null) {
                return suggestions;
            }

            // Проверяем права
            if (!region.isOwner(player.getUniqueId())) {
                return suggestions;
            }

            // Проверяем, является ли это allowall/denyall
            if ("allowall".equalsIgnoreCase(flagName) || "denyall".equalsIgnoreCase(flagName)) {
                return suggestions; // Эти команды не требуют дополнительных аргументов
            }

            // Проверяем, является ли flagName номером страницы
            try {
                Integer.parseInt(flagName);
                return suggestions; // Номер страницы - не предлагаем значения
            } catch (NumberFormatException e) {
                // Продолжаем обработку
            }

            // Проверяем существование флага
            if (!flagManager.flagExists(flagName)) {
                // Предлагаем доступные флаги
                List<String> availableFlags = flagManager.getFlagNamesWithPermission(player);
                for (String flag : availableFlags) {
                    if (flag.toLowerCase().startsWith(flagName.toLowerCase())) {
                        suggestions.add(flag);
                    }
                }
                return suggestions;
            }

            // Получаем флаг
            AbstractRegionFlag<?> flag = flagManager.getFlag(flagName);
            if (flag == null) {
                return suggestions;
            }

            // Предлагаем базовые значения для флага
            List<String> flagValues = getFlagValueSuggestions(flag);
            for (String value : flagValues) {
                if (value.toLowerCase().startsWith(currentInput)) {
                    suggestions.add(value);
                }
            }
            return suggestions;
        }

        // /rg flag <регион> <флаг> <часть_значения> [TAB] - продолжение для строковых значений
        // args.length >= 4
        String regionName = args[0];
        String flagName = args[1];
        String currentInput = args[args.length - 1].toLowerCase();

        // Проверяем регион
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            return suggestions;
        }

        // Проверяем права
        if (!region.isOwner(player.getUniqueId())) {
            return suggestions;
        }

        // Проверяем существование флага
        if (!flagManager.flagExists(flagName)) {
            return suggestions;
        }

        // Для строковых флагов предлагаем продолжения
        AbstractRegionFlag<?> flag = flagManager.getFlag(flagName);
        if (flag != null) {
            List<String> additionalValues = getAdditionalValueSuggestions(flag);
            for (String value : additionalValues) {
                if (value.toLowerCase().startsWith(currentInput)) {
                    suggestions.add(value);
                }
            }
        }

        return suggestions;
    }

    /**
     * Получить предложения значений для конкретного флага
     */
    private List<String> getFlagValueSuggestions(AbstractRegionFlag<?> flag) {
        List<String> suggestions = new ArrayList<>();

        String flagName = flag.getName().toLowerCase();

        // Специальная обработка для флага time-lock
        if (flagName.contains("time") && flagName.contains("lock")) {
            suggestions.add("deny");
            suggestions.add("day");
            suggestions.add("night");
            suggestions.add("allow");
            suggestions.add("true");
            suggestions.add("false");
            return suggestions;
        }

        // Проверяем, является ли флаг BooleanFlag
        boolean isBooleanFlag = false;
        try {
            // Проверяем, наследуется ли флаг от BooleanFlag
            isBooleanFlag = flag instanceof BooleanFlag;
        } catch (Exception e) {
            // Игнорируем ошибки
        }

        if (isBooleanFlag) {
            // Для BooleanFlag предлагаем все возможные значения
            suggestions.add("allow");
            suggestions.add("deny");
            suggestions.add("true");
            suggestions.add("false");
            suggestions.add("on");
            suggestions.add("off");
            suggestions.add("yes");
            suggestions.add("no");
            suggestions.add("1");
            suggestions.add("0");

            // Также добавляем русские варианты
            suggestions.add("разрешить");
            suggestions.add("запретить");
            suggestions.add("да");
            suggestions.add("нет");
            suggestions.add("включить");
            suggestions.add("выключить");
        }
        // Для числовых флагов (когда defaultValue является Integer)
        else if (flag.getDefaultValue() instanceof Integer) {
            suggestions.add("0");
            suggestions.add("1");
            suggestions.add("5");
            suggestions.add("10");
            suggestions.add("50");
            suggestions.add("100");
            suggestions.add("-1");
            suggestions.add("-5");
            suggestions.add("-10");
        }
        // Для строковых флагов (когда defaultValue является String)
        else if (flag.getDefaultValue() instanceof String) {
            // Для флагов приоритета
            if (flagName.contains("priority")) {
                suggestions.add("0");
                suggestions.add("1");
                suggestions.add("10");
                suggestions.add("100");
                suggestions.add("-1");
                suggestions.add("-10");
            }
            // Для флагов мобов
            else if (flagName.contains("mob") || flagName.contains("spawn")) {
                suggestions.add("all");
                suggestions.add("hostile");
                suggestions.add("passive");
                suggestions.add("none");
                suggestions.add("monster");
                suggestions.add("animal");
            }
            // Для других строковых флагов
            else {
                // Предлагаем булевые значения, так как они могут быть преобразованы
                suggestions.add("allow");
                suggestions.add("deny");
                suggestions.add("true");
                suggestions.add("false");
                suggestions.add("on");
                suggestions.add("off");
            }
        }
        // Для остальных типов флагов
        else {
            // По умолчанию предлагаем булевые значения
            suggestions.add("allow");
            suggestions.add("deny");
            suggestions.add("true");
            suggestions.add("false");
            suggestions.add("on");
            suggestions.add("off");
        }

        return suggestions;
    }

    /**
     * Получить дополнительные предложения значений для сложных флагов
     */
    private List<String> getAdditionalValueSuggestions(AbstractRegionFlag<?> flag) {
        List<String> suggestions = new ArrayList<>();
        String flagName = flag.getName().toLowerCase();

        // Для флагов сообщений (строковые значения)
        if (flagName.contains("message") || flagName.contains("msg") ||
                flagName.contains("greeting") || flagName.contains("farewell")) {
            suggestions.add("Приветствую!");
            suggestions.add("Добро пожаловать!");
            suggestions.add("Вход запрещен!");
            suggestions.add("У вас нет прав!");
            suggestions.add("Внимание!");
            suggestions.add("До свидания!");
            suggestions.add("Welcome!");
            suggestions.add("Access Denied!");
            suggestions.add("No Permission!");
        }

        // Для флагов групп и разрешений
        if (flagName.contains("group") || flagName.contains("member") ||
                flagName.contains("role") || flagName.contains("trust")) {
            suggestions.add("owner");
            suggestions.add("member");
            suggestions.add("trusted");
            suggestions.add("guest");
            suggestions.add("admin");
            suggestions.add("moderator");
            suggestions.add("builder");
            suggestions.add("владелец");
            suggestions.add("участник");
            suggestions.add("доверенный");
            suggestions.add("гость");
        }

        // Для флагов телепортации
        if (flagName.contains("teleport") || flagName.contains("warp")) {
            suggestions.add("spawn");
            suggestions.add("home");
            suggestions.add("center");
            suggestions.add("entrance");
            suggestions.add("спавн");
            suggestions.add("дом");
            suggestions.add("центр");
            suggestions.add("вход");
        }

        return suggestions;
    }

    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭта команда доступна только игрокам!");
            return;
        }

        // Команда /region flag list
        if (args.length > 0 && "list".equalsIgnoreCase(args[0])) {
            listFlags(sender); // listFlags уже принимает CommandSender
            return;
        }

        // Команда /region flag allowall/denyall
        if (args.length == 2 && ("allowall".equalsIgnoreCase(args[1]) || "denyall".equalsIgnoreCase(args[1]))) {
            handleBulkFlagOperation(player, args[0], args[1]);
            return;
        }

        // Если нет аргументов - меню выбора региона
        if (args.length == 0) {
            showRegionSelectionMenu(player);
            return;
        }

        // Если 1 аргумент - меню флагов для региона
        if (args.length == 1) {
            showFlagMenu(player, args[0], 1);
            return;
        }

        // Если 2 аргумента - проверяем, является ли второй аргумент числом (страницей)
        if (args.length == 2) {
            try {
                int page = Integer.parseInt(args[1]);
                showFlagMenu(player, args[0], page);
                return;
            } catch (NumberFormatException e) {
                // Если не число, значит это флаг - показываем меню значений
                showFlagValueMenu(player, args[0], args[1]);
                return;
            }
        }

        // 3+ аргументов - установка флага
        String regionName = args[0];
        String flagName = args[1].toLowerCase();
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (!valueBuilder.isEmpty()) valueBuilder.append(" ");
            valueBuilder.append(args[i]);
        }
        String flagValue = valueBuilder.toString();

        setFlagDirectly(sender, regionName, flagName, flagValue); // setFlagDirectly уже принимает CommandSender
    }

    /**
     * Метод для прямой установки флага с поддержкой специальных флагов
     */
    private void setFlagDirectly(CommandSender sender, String regionName, String flagName, String flagValue) {
        // Проверяем входные параметры
        if (regionName == null || regionName.isEmpty()) {
            sender.sendMessage("§cОшибка: имя региона не может быть пустым!");
            return;
        }

        if (flagName == null || flagName.isEmpty()) {
            sender.sendMessage("§cОшибка: имя флага не может быть пустым!");
            return;
        }

        // Проверяем существование региона через RegionManager
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            sender.sendMessage("§cРегион '" + regionName + "' не найден!");
            return;
        }

        // Проверка существования флага через FlagManager
        if (!flagManager.flagExists(flagName)) {
            sender.sendMessage("§cФлаг '" + flagName + "' не найден!");
            return;
        }

        // Проверка прав через FlagManager
        if (sender instanceof Player player && !flagManager.hasFlagPermission(player, flagName)) {
            player.sendMessage("§cУ вас нет прав для изменения флага '" + flagName + "'!");
            return;
        }

        // Получаем флаг из FlagManager
        AbstractRegionFlag<?> regionFlag = flagManager.getFlag(flagName);
        if (regionFlag == null) {
            sender.sendMessage("§cФлаг '" + flagName + "' не найден!");
            return;
        }

        // Парсим значение через метод флага
        Object finalValue;
        try {
            finalValue = regionFlag.parseInput(flagValue);
        } catch (Exception e) {
            sender.sendMessage("§cНеверное значение флага: " + flagValue);
            sender.sendMessage("§cДопустимые значения: allow, deny, true, false, on, off");
            return;
        }

        // Обработка специальных флагов (например, time-lock)
        boolean success = handleSpecialFlag(sender, region, flagName, flagValue);

        if (!success) {
            // Для обычных флагов используем стандартную установку
            success = setRegionFlagDirect(region, flagName, finalValue);
        }

        if (success) {
            String valueDisplay = getDisplayValue(finalValue);
            sender.sendMessage("§aФлаг §e" + flagName + "§a установлен в " + valueDisplay + "§a для региона §e" + regionName);

            // Кнопки для быстрых действий (только для игроков)
            if (sender instanceof Player player) {
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createRunComponent("§6[📋 Инфо региона]",
                                "§7Просмотр информации о регионе", "/region info " + regionName),
                        JsonMessageUtil.createRunComponent(" §a[🔄 Переключить]",
                                "§7Переключить этот флаг", "/region flag " + regionName + " " + flagName + " " + getToggleValue(flagValue)),
                        JsonMessageUtil.createRunComponent(" §e[⚙ Другие флаги]",
                                "§7Установить другие флаги", "/region flag " + regionName)
                );
            }
        } else {
            sender.sendMessage("§cНе удалось установить флаг '" + flagName + "' для региона '" + regionName + "'!");
        }
    }

    /**
     * Обработка специальных флагов с дополнительной логикой
     */
    private boolean handleSpecialFlag(CommandSender sender, Region region, String flagName, String rawValue) {
        // Обработка флага time-lock
        if (flagName.equalsIgnoreCase("time-lock")) {
            return handleTimeLockFlag(sender, region, rawValue);
        }

        return false; // Не является специальным флагом, обрабатывается стандартным способом
    }

    /**
     * Обработка флага time-lock
     */
    private boolean handleTimeLockFlag(CommandSender sender, Region region, String value) {
        String lowerValue = value.toLowerCase();

        try {
            // Проверяем допустимые значения
            if (!lowerValue.equals("deny") && !lowerValue.equals("day") &&
                    !lowerValue.equals("night") && !lowerValue.equals("allow") &&
                    !lowerValue.equals("true") && !lowerValue.equals("false")) {
                sender.sendMessage("§cНеверное значение для time-lock! Допустимо: deny, day, night, allow");
                return false;
            }

            // Сохраняем значение флага time-lock в регион
            region.setFlag("time-lock", lowerValue);

            // Особенная логика для значения "deny"
            if (lowerValue.equals("deny") || lowerValue.equals("true")) {
                // Получаем мир региона
                World world = region.getWorld();
                if (world != null) {
                    // Сохраняем текущее время мира
                    long currentTime = world.getTime() % 24000;

                    // Сохраняем фиксированное время как отдельный флаг
                    region.setFlag("fixed-time", currentTime);

                    // Показываем информацию игроку
                    if (sender instanceof Player) {
                        sender.sendMessage("§aФлаг time-lock установлен на 'deny'!");
                        sender.sendMessage("§7Время зафиксировано на текущем значении: " + formatTime(currentTime));
                    }
                } else {
                    if (sender instanceof Player) {
                        sender.sendMessage("§cНе удалось получить мир региона!");
                    }
                    return false;
                }
            }
            // Логика для значения "day"
            else if (lowerValue.equals("day")) {
                region.setFlag("fixed-time", 0L); // Полдень в Minecraft

                if (sender instanceof Player) {
                    sender.sendMessage("§aФлаг time-lock установлен на 'day'!");
                    sender.sendMessage("§7Время зафиксировано на дневном значении (12:00)");
                }
            }
            // Логика для значения "night"
            else if (lowerValue.equals("night")) {
                region.setFlag("fixed-time", 18000L); // Полночь в Minecraft

                if (sender instanceof Player) {
                    sender.sendMessage("§aФлаг time-lock установлен на 'night'!");
                    sender.sendMessage("§7Время зафиксировано на ночном значении (00:00)");
                }
            }
            // Для значений allow/false удаляем фиксированное время
            else {
                // Удаляем фиксированное время
                region.setFlag("fixed-time", null);

                if (sender instanceof Player) {
                    sender.sendMessage("§aФлаг time-lock установлен на '" + value + "'!");
                    sender.sendMessage("§7Время больше не фиксируется в регионе");
                }
            }

            // Сохраняем регион
            return saveRegionToFile(region);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при установке флага time-lock для региона " + region.getName(), e);
            if (sender instanceof Player) {
                sender.sendMessage("§cОшибка при установке флага time-lock!");
            }
            return false;
        }
    }

    /**
     * Форматирование времени для отображения
     */
    private String formatTime(long tickTime) {
        long hours = (tickTime / 1000 + 6) % 24;
        long minutes = (tickTime % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * Временный метод для установки флага напрямую в регион
     */
    private boolean setRegionFlagDirect(Region region, String flagName, Object value) {
        try {
            region.setFlag(flagName, value);
            // Используем альтернативный метод сохранения через RegionManager
            return saveRegionToFile(region);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при установке флага " + flagName + " для региона " + region.getName(), e);
            return false;
        }
    }

    /**
     * Альтернативный метод сохранения региона
     */
    private boolean saveRegionToFile(Region region) {
        try {
            // Пробуем использовать рефлексию для вызова метода сохранения
            return callSaveRegionViaReflection(region);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при сохранении региона " + region.getName(), e);
            return false;
        }
    }

    /**
     * Сохранение региона через сериализацию (альтернативный способ)
     */
    private boolean saveRegionViaSerialization(Region region) {
        try {
            // Получаем данные региона
            Map<String, Object> regionData = region.serialize();

            // Сохраняем напрямую в YAML файл
            return saveRegionToYamlFile(region, regionData);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при сериализации региона " + region.getName(), e);
            return false;
        }
    }

    /**
     * Сохранение региона в YAML файл
     */
    private boolean saveRegionToYamlFile(Region region, Map<String, Object> regionData) {
        try {
            // Создаем папку регионов если не существует
            java.io.File regionsFolder = new java.io.File(plugin.getDataFolder(), "regions");
            if (!regionsFolder.exists() && !regionsFolder.mkdirs()) {
                plugin.getLogger().warning("Не удалось создать папку регионов: " + regionsFolder.getPath());
                return false;
            }

            // Создаем файл региона
            java.io.File regionFile = new java.io.File(regionsFolder, region.getName() + ".yml");
            org.bukkit.configuration.file.YamlConfiguration config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(regionFile);

            // Записываем данные
            for (Map.Entry<String, Object> entry : regionData.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }

            // Сохраняем файл
            config.save(regionFile);
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при сохранении региона в файл " + region.getName(), e);
            return false;
        }
    }

    /**
     * Вызов приватного метода через рефлексию (резервный вариант)
     */
    private boolean callSaveRegionViaReflection(Region region) {
        try {
            java.lang.reflect.Method saveMethod = regionManager.getClass().getDeclaredMethod("saveRegion", Region.class);
            saveMethod.setAccessible(true);
            saveMethod.invoke(regionManager, region);
            return true;
        } catch (Exception e) {
            // Если рефлексия не работает, используем альтернативный способ
            return saveRegionViaSerialization(region);
        }
    }

    /**
     * Получить отображаемое значение флага
     */
    private String getDisplayValue(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue ? "§aРАЗРЕШЕНО" : "§cЗАПРЕЩЕНО";
        } else if (value instanceof String strValue) {
            String lowerValue = strValue.toLowerCase();

            // Проверяем все возможные значения для "разрешить"
            if (lowerValue.equals("true") || lowerValue.equals("allow") ||
                    lowerValue.equals("on") || lowerValue.equals("yes") ||
                    lowerValue.equals("разрешить") || lowerValue.equals("да") ||
                    lowerValue.equals("включить") || lowerValue.equals("1")) {
                return "§aРАЗРЕШЕНО";
            }
            // Проверяем все возможные значения для "запретить"
            else if (lowerValue.equals("false") || lowerValue.equals("deny") ||
                    lowerValue.equals("off") || lowerValue.equals("no") ||
                    lowerValue.equals("запретить") || lowerValue.equals("нет") ||
                    lowerValue.equals("выключить") || lowerValue.equals("0")) {
                return "§cЗАПРЕЩЕНО";
            } else {
                return "§e" + strValue.toUpperCase();
            }
        } else {
            return "§e" + value;
        }
    }

    /**
     * Проверить, разрешено ли значение флага
     */
    private boolean isAllowed(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        } else if (value instanceof String strValue) {
            String lowerValue = strValue.toLowerCase();

            // Проверяем все возможные значения для "разрешить"
            return lowerValue.equals("true") || lowerValue.equals("allow") ||
                    lowerValue.equals("on") || lowerValue.equals("yes") ||
                    lowerValue.equals("разрешить") || lowerValue.equals("да") ||
                    lowerValue.equals("включить") || lowerValue.equals("1");
        }
        return false;
    }

    /**
     * Форматировать значение для отображения
     */
    private String formatValue(Object value) {
        if (value instanceof Boolean boolValue) {
            return boolValue ? "РАЗРЕШЕНО" : "ЗАПРЕЩЕНО";
        } else if (value instanceof String strValue) {
            String lowerValue = strValue.toLowerCase();

            // Используем switch вместо цепочки if
            switch (lowerValue) {
                case "allow", "true", "on", "yes", "разрешить", "да", "включить" -> {
                    return "РАЗРЕШЕНО";
                }
                case "deny", "false", "off", "no", "запретить", "нет", "выключить" -> {
                    return "ЗАПРЕЩЕНО";
                }
                default -> {
                    return strValue.toUpperCase();
                }
            }
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * Получить список предлагаемых значений для флага
     */
    private List<String> getSuggestedValues() {
        // Базовые значения для большинства флагов
        return Arrays.asList("allow", "deny", "true", "false", "on", "off");
    }

    /**
     * Обрабатывает массовую установку флагов (allowall/denyall)
     */
    private void handleBulkFlagOperation(Player player, String regionName, String operation) {
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            player.sendMessage("§cРегион '" + regionName + "' не найден!");
            return;
        }

        boolean allow = "allowall".equalsIgnoreCase(operation);
        Object value = allow;

        // Получаем только флаги, которые игрок может изменять
        List<AbstractRegionFlag<?>> allowedFlags = flagManager.getFlagsWithPermission(player);

        int changed = 0;
        for (AbstractRegionFlag<?> flag : allowedFlags) {
            String flagName = flag.getName();
            if (flagName == null) continue;

            // Пропускаем специальные флаги при массовой установке
            if (flagName.equalsIgnoreCase("time-lock")) {
                continue;
            }

            boolean success = setRegionFlagDirect(region, flagName, value);
            if (success) {
                changed++;
            }
        }

        player.sendMessage("§a" + changed + " флагов установлено в " + (allow ? "§aРАЗРЕШИТЬ" : "§cЗАПРЕТИТЬ") + "§a для региона §e" + regionName);
        player.sendMessage("§7Примечание: специальные флаги (например time-lock) не были изменены");
    }

    /**
     * Показывает меню выбора региона
     */
    private void showRegionSelectionMenu(Player player) {
        player.sendMessage("§6=== Управление флагами ===");
        player.sendMessage("§7Выберите регион для управления флагами:");

        // Получаем регионы игрока через RegionManager
        List<String> playerRegions = regionManager.getPlayerRegionNames(player.getUniqueId());

        if (playerRegions.isEmpty()) {
            player.sendMessage("§cУ вас нет регионов!");
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§a[🏷 Создать регион]",
                            "§7Создать первый регион", "/region claim")
            );
            return;
        }

        int count = 0;
        for (String regionName : playerRegions) {
            if (count >= 10) break;

            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createRunComponent("§7• §e" + regionName,
                            "§7Управление флагами для " + regionName,
                            "/region flag " + regionName)
            );
            count++;
        }

        if (playerRegions.size() > 10) {
            player.sendMessage("§7... и еще " + (playerRegions.size() - 10) + " регионов");
        }

        player.sendMessage("§7Или введите: §e/region flag <регион>");
    }

    /**
     * Показывает меню флагов для региона с указанной страницей
     */
    private void showFlagMenu(Player player, String regionName, int page) {
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            player.sendMessage("§cРегион '" + regionName + "' не найдён!");
            return;
        }

        // Получаем только флаги, которые игрок может изменять
        List<AbstractRegionFlag<?>> availableFlags = flagManager.getFlagsWithPermission(player);

        if (availableFlags.isEmpty()) {
            player.sendMessage("§cУ вас нет прав для изменения флагов!");
            return;
        }

        // Настройки пагинации - 12 флагов на страницу
        int flagsPerPage = 12;
        int totalPages = (int) Math.ceil((double) availableFlags.size() / flagsPerPage);

        // Проверяем валидность страницы
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        // Вычисляем индексы для текущей страницы
        int startIndex = (page - 1) * flagsPerPage;
        int endIndex = Math.min(startIndex + flagsPerPage, availableFlags.size());

        // Заголовок
        player.sendMessage("§6" + "=".repeat(50));
        player.sendMessage("§6⚙ Флаги региона: §e" + regionName + " §6(стр. " + page + "/" + totalPages + ")");
        player.sendMessage("§7Всего доступно флагов: §e" + availableFlags.size());
        player.sendMessage("§6" + "=".repeat(50));

        // Отображаем флаги для текущей страницы
        List<AbstractRegionFlag<?>> currentPageFlags = availableFlags.subList(startIndex, endIndex);

        if (currentPageFlags.isEmpty()) {
            player.sendMessage("§cНет флагов для отображения!");
            return;
        }

        // Показываем флаги
        for (AbstractRegionFlag<?> flag : currentPageFlags) {
            String flagName = flag.getName();
            if (flagName == null) continue;

            String displayValue;
            boolean isAllowed;

            // Получаем текущее значение флага из региона
            Object currentValue = region.getFlag(flagName);
            if (currentValue != null) {
                displayValue = getDisplayValue(currentValue);
                isAllowed = isAllowed(currentValue);
            } else {
                // Используем значение по умолчанию
                Object defaultValue = flag.getDefaultValue();
                displayValue = getDisplayValue(defaultValue);
                isAllowed = isAllowed(defaultValue);
            }

            String description = flagManager.getFlagDescription(flagName);
            String currentStatus = isAllowed ? "§aallow" : "§cdeny";
            String toggleButtonText = isAllowed ? "§c[deny]" : "§a[allow]";
            String toggleCommand = isAllowed ? "deny" : "allow";
            String toggleHover = isAllowed ? "§7Изменить на §cdeny" : "§7Изменить на §aallow";

            // Отображаем флаг
            JsonMessageUtil.sendCompositeMessage(player,
                    JsonMessageUtil.createSuggestComponent("§7§e" + flagName + " " + currentStatus,
                            "§f" + description + "\n§7Текущее: " + displayValue + "\n§a▸ Нажмите для ручной настройки",
                            "/region flag " + regionName + " " + flagName + " "),
                    JsonMessageUtil.createRunComponent(" " + toggleButtonText,
                            toggleHover, "/region flag " + regionName + " " + flagName + " " + toggleCommand)
            );
        }

        // Пагинация
        player.sendMessage("§6" + "=".repeat(50));
        Component paginationLine = Component.empty();

        // Кнопка "Первая страница"
        if (page > 1) {
            paginationLine = paginationLine.append(
                    JsonMessageUtil.createRunComponent("§6[⏪ Первая]",
                            "§7Перейти на первую страницу",
                            "/region flag " + regionName + " 1")
            ).append(Component.text(" "));
        }

        // Кнопка "Предыдущая страница"
        if (page > 1) {
            paginationLine = paginationLine.append(
                    JsonMessageUtil.createRunComponent("§6[◀ Пред]",
                            "§7Предыдущая страница (" + (page - 1) + ")",
                            "/region flag " + regionName + " " + (page - 1))
            ).append(Component.text(" "));
        }

        // Информация о странице
        paginationLine = paginationLine.append(Component.text("§eСтр. " + page + "/" + totalPages + " "));

        // Кнопка "Следующая страница"
        if (page < totalPages) {
            paginationLine = paginationLine.append(
                    JsonMessageUtil.createRunComponent("§6[След ▶]",
                            "§7Следующая страница (" + (page + 1) + ")",
                            "/region flag " + regionName + " " + (page + 1))
            ).append(Component.text(" "));
        }

        // Кнопка "Последняя страница"
        if (page < totalPages) {
            paginationLine = paginationLine.append(
                    JsonMessageUtil.createRunComponent("§6[Посл ⏩]",
                            "§7Перейти на последнюю страницу",
                            "/region flag " + regionName + " " + totalPages)
            );
        }

        player.sendMessage(paginationLine);

        // Информация о флагах и позициях
        int flagsOnThisPage = endIndex - startIndex;
        player.sendMessage("§7Флагов на странице: §e" + flagsOnThisPage + "§7/§e" + availableFlags.size());

        // Быстрые действия
        player.sendMessage("§6⚡ Быстрые действия:");
        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§a[✅ Разрешить все]",
                        "§7Разрешить все доступные флаги", "/region flag " + regionName + " allowall"),
                JsonMessageUtil.createRunComponent(" §c[❌ Запретить все]",
                        "§7Запретить все доступные флаги", "/region flag " + regionName + " denyall")
        );

        // Навигация
        player.sendMessage("§6🧭 Навигация:");
        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§6[📋 Инфо региона]",
                        "§7Вернуться к информации о регионе", "/region info " + regionName),
                JsonMessageUtil.createRunComponent(" §e[🔍 Все флаги]",
                        "§7Показать список всех флагов", "/region flag list"),
                JsonMessageUtil.createRunComponent(" §b[🎯 Поиск]",
                        "§7Поиск конкретного флага", "/region flag " + regionName + " <флаг>")
        );
    }

    /**
     * Показывает меню значений для конкретного флага
     */
    private void showFlagValueMenu(Player player, String regionName, String flagName) {
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            player.sendMessage("§cРегион '" + regionName + "' не найден!");
            return;
        }

        if (!flagManager.flagExists(flagName)) {
            player.sendMessage("§cФлаг '" + flagName + "' не найден!");
            return;
        }

        // Проверка прав
        if (!flagManager.hasFlagPermission(player, flagName)) {
            player.sendMessage("§cУ вас нет прав для изменения этого флага!");
            return;
        }

        AbstractRegionFlag<?> regionFlag = flagManager.getFlag(flagName);
        if (regionFlag == null) {
            player.sendMessage("§cФлаг '" + flagName + "' не найден!");
            return;
        }

        player.sendMessage("§6=== Установка флага: §e" + flagName + " §6===");
        player.sendMessage("§7Регион: §e" + regionName);
        player.sendMessage("§7Описание: §f" + flagManager.getFlagDescription(flagName));

        // Показываем текущее значение
        Object currentValue = region.getFlag(flagName);
        String currentValueStr;
        boolean isAllowed;

        if (currentValue != null) {
            currentValueStr = formatValue(currentValue);
            isAllowed = isAllowed(currentValue);
        } else {
            // Значение по умолчанию
            Object defaultValue = regionFlag.getDefaultValue();
            currentValueStr = "ПО УМОЛЧАНИЮ (" + formatValue(defaultValue) + ")";
            isAllowed = isAllowed(defaultValue);
        }

        player.sendMessage("§7Текущее: §e" + currentValueStr);

        // Показываем переключающуюся кнопку
        String toggleButtonText = isAllowed ? "§c[УСТАНОВИТЬ ЗАПРЕТ]" : "§a[УСТАНОВИТЬ РАЗРЕШЕНИЕ]";
        String toggleCommand = isAllowed ? "deny" : "allow";
        String toggleHover = isAllowed ? "§7Изменить на ЗАПРЕТ" : "§7Изменить на РАЗРЕШЕНИЕ";

        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent(toggleButtonText,
                        toggleHover, "/region flag " + regionName + " " + flagName + " " + toggleCommand)
        );

        // Кнопки для быстрой установки значений
        player.sendMessage("§7Быстрые значения:");

        // Для флага time-lock показываем специальные значения
        if (flagName.equalsIgnoreCase("time-lock")) {
            List<String> timeLockValues = Arrays.asList("deny", "day", "night", "allow");
            for (String value : timeLockValues) {
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createRunComponent("§7[" + value.toUpperCase() + "]",
                                "§7Установить time-lock в " + value,
                                "/region flag " + regionName + " " + flagName + " " + value)
                );
            }
        } else {
            // Используем информацию о флаге для показа соответствующих кнопок
            List<String> suggestedValues = getSuggestedValues();
            for (String value : suggestedValues) {
                Object parsedValue;
                try {
                    parsedValue = regionFlag.parseInput(value);
                } catch (Exception e) {
                    continue; // Пропускаем невалидные значения для этого флага
                }
                String displayValue = getDisplayValue(parsedValue);
                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createRunComponent("§7[" + displayValue + "]",
                                "§7Установить в " + value,
                                "/region flag " + regionName + " " + flagName + " " + value)
                );
            }
        }

        // Кнопка назад
        player.sendMessage("§6=== Навигация ===");
        JsonMessageUtil.sendCompositeMessage(player,
                JsonMessageUtil.createRunComponent("§6[⬅ Назад к флагам]",
                        "§7Вернуться к списку флагов", "/region flag " + regionName),
                JsonMessageUtil.createRunComponent(" §b[📋 Инфо региона]",
                        "§7Вернуться к информации о регионе", "/region info " + regionName)
        );
    }

    private void listFlags(CommandSender sender) {
        List<String> availableFlags;

        if (sender instanceof Player player) {
            // Для игроков показываем только флаги, которые они могут изменять
            availableFlags = flagManager.getFlagNamesWithPermission(player);
        } else {
            // Для консоли показываем все флаги
            availableFlags = flagManager.getAllFlagNames();
        }

        if (sender instanceof Player player) {
            player.sendMessage("§6=== Доступные флаги ===");

            for (String flagName : availableFlags) {
                String description = flagManager.getFlagDescription(flagName);

                JsonMessageUtil.sendCompositeMessage(player,
                        JsonMessageUtil.createSuggestComponent("§7• §e" + flagName + " §7- " + description,
                                "§7Нажмите чтобы использовать этот флаг: /region flag <регион> " + flagName + " <значение>",
                                "/region flag " + flagName + " ")
                );
            }

            player.sendMessage("§7Использование: §e/region flag <регион> <флаг> <значение>");
        } else {
            sender.sendMessage("§6Доступные флаги: " + String.join(", ", availableFlags));
            sender.sendMessage("§6Использование: /region flag <регион> <флаг> <значение>");
        }
    }

    private String getToggleValue(String currentValue) {
        if (currentValue == null || currentValue.isEmpty()) {
            return "deny"; // значение по умолчанию
        }

        String lowerValue = currentValue.toLowerCase();

        // Используем switch вместо цепочки if
        switch (lowerValue) {
            case "true", "allow", "on", "yes", "разрешить", "да", "включить", "1" -> {
                return "deny"; // переключаем на противоположное
            }
            case "false", "deny", "off", "no", "запретить", "нет", "выключить", "0" -> {
                return "allow"; // переключаем на противоположное
            }
            default -> {
                return "deny"; // значение по умолчанию
            }
        }
    }
}