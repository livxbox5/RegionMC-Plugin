package org.KillerYT.regionMC.managers;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.region.Region;
import org.KillerYT.regionMC.utils.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("ALL")
public class RegionManager implements Listener {

    // =============================================
    // НАСТРОЙКИ РЕГИОНОВ
    // =============================================

    private final RegionMC plugin;
    private final Map<String, Region> regions;
    private final Map<UUID, Location> pos1Selections;
    private final Map<UUID, Location> pos2Selections;
    private final File regionsFolder;
    private final TimeLockManager timeLockManager;
    private final AdminManager adminManager;

    public RegionManager(RegionMC plugin) {
        this.plugin = plugin;
        this.adminManager = plugin.getAdminManager();
        this.regions = new HashMap<>();
        this.pos1Selections = new HashMap<>();
        this.pos2Selections = new HashMap<>();
        this.regionsFolder = new File(plugin.getDataFolder(), "regions");
        this.timeLockManager = new TimeLockManager(plugin);
        loadRegions();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // =============================================
    // МЕТОДЫ ДЛЯ РАБОТЫ С ВЗРЫВАМИ КРИПЕРОВ
    // =============================================

    /**
     * Проверяет разрешение на взрывы криперов в регионе
     * @deprecated Используйте {@link #getFlagBooleanValue(Region, String, boolean)} с флагом "creeper-explosion"
     */
    @Deprecated
    public boolean canCreeperExplodeInRegion(String regionName) {
        Region region = getRegion(regionName);
        if (region == null) return true;

        return getFlagBooleanValue(region, "creeper-explosion", true);
    }

    // =============================================
    // МЕТОДЫ ДЛЯ СОВМЕСТИМОСТИ
    // =============================================

    /**
     * Находит регион по локации и возвращает его имя
     */
    public String findRegionAtLocation(Location location) {
        Region region = getRegionAtLocation(location);
        return region != null ? region.getName() : null;
    }

    /**
     * Проверяет, является ли игрок владельцем региона
     */
    public boolean isOwner(Player player, String regionName) {
        return isOwner(player.getUniqueId(), regionName);
    }

    /**
     * Проверяет, является ли игрок участником региона
     */
    public boolean isMember(Player player, String regionName) {
        return isMember(player.getUniqueId(), regionName);
    }

    // =============================================
    // ОСНОВНЫЕ МЕТОДЫ РЕГИОНОВ
    // =============================================

    /**
     * Создает регион с указанными позициями
     */
    public boolean createRegion(String name, Player player, Location pos1, Location pos2) {
        // Проверка существования региона
        if (regions.containsKey(name)) {
            player.sendMessage("§cРегион с именем '" + name + "' уже существует!");
            return false;
        }

        // Дополнительная проверка на похожие имена
        for (String existingName : regions.keySet()) {
            if (existingName.equalsIgnoreCase(name) && !existingName.equals(name)) {
                player.sendMessage("§6Внимание: уже существует регион с похожим именем '" +
                        existingName + "'. Будьте внимательны!");
                break;
            }
        }

        // Проверка валидности позиций
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || pos2.getWorld() == null) {
            player.sendMessage("§cОшибка: позиции не установлены или мир не найден!");
            return false;
        }

        // Проверка одинакового мира
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage("§cОшибка: позиции должны быть в одном мире!");
            return false;
        }

        // Расчет объема
        int volume = calculateVolume(pos1, pos2);

        // ========== ПРОВЕРКА РАЗМЕРА РЕГИОНА ==========
        // Проверка включена ли проверка размеров
        if (areSizeChecksEnabled() && areLimitsEnabled()) {
            // Проверяем минимальный размер
            int minSize = getMinRegionSize();
            if (minSize > 0 && volume < minSize) {
                player.sendMessage("§cРегион слишком маленький! Минимальный размер: §e" + minSize + " §cблоков");
                player.sendMessage("§cВаш регион: §e" + volume + " §cблоков");
                return false;
            }

            // Проверяем максимальный размер
            int maxSize = getMaxRegionSizeForPlayer(player);
            if (maxSize > 0 && volume > maxSize) {
                player.sendMessage("§cРегион слишком большой! Максимальный размер: §e" + maxSize + " §cблоков");
                player.sendMessage("§cВаш регион: §e" + volume + " §cблоков");
                return false;
            }

            // Проверяем высоту
            int maxHeight = getMaxRegionHeight();
            int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
            int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());

            // Если maxHeight > 0 - проверяем ограничение
            // Если maxHeight == Integer.MAX_VALUE (из-за -1) - безлимитно
            if (maxHeight > 0 && maxHeight != Integer.MAX_VALUE && maxY > maxHeight) {
                player.sendMessage("§cРегион слишком высокий! Максимальная высота: §e" + maxHeight + " §cблоков");
                return false;
            }
        }

        // ========== ПРОВЕРКА ЛИМИТА КОЛИЧЕСТВА РЕГИОНОВ ==========
        if (!canPlayerCreateRegion(player)) {
            int maxRegions = getMaxRegionsForPlayer(player);
            int currentRegions = getPlayerRegionsCount(player.getUniqueId());
            player.sendMessage("§cВы достигли лимита регионов! Максимум: §e" + maxRegions);
            player.sendMessage("§7У вас уже есть §e" + currentRegions + " §7регионов");
            player.sendMessage("§7Используйте §6/rg remove <название> §7чтобы удалить старый регион");
            return false;
        }

        // Проверка конфликтов с другими регионами
        if (hasRegionConflict(pos1, pos2, null)) {
            player.sendMessage("§cРегион пересекается с существующим регионом!");
            return false;
        }

        // Создание региона
        Region region = new Region(name, player.getUniqueId(), pos1, pos2);
        region.setPriority(1);
        setDefaultRegionFlags(region, isGlobalRegion(name));
        regions.put(name, region);
        saveRegion(region);

        // Очистка выделенных позиций
        UUID playerId = player.getUniqueId();
        pos1Selections.remove(playerId);
        pos2Selections.remove(playerId);

        // Информация для игрока
        player.sendMessage("§aРегион '" + name + "' успешно создан с приоритетом 1!");
        player.sendMessage("§7Размер региона: §e" + volume + " §7блоков");

        // Показываем информацию о лимитах только если они включены
        if (areLimitsEnabled()) {
            int maxSize = getMaxRegionSizeForPlayer(player);
            if (maxSize != -1 && maxSize != 0) {
                String percentage = String.format("%.1f", (volume * 100.0 / maxSize));
                player.sendMessage("§7Использовано лимита размера: §6" + percentage + "%");
            } else if (maxSize == -1) {
                player.sendMessage("§aУ вас безлимитный доступ к созданию регионов по размеру!");
            }

            // Показываем информацию о лимите количества регионов
            player.sendMessage(getMaxRegionsMessage(player));
        } else {
            player.sendMessage("§aЛимиты регионов отключены на сервере!");
        }

        return true;
    }

    /**
     * Устанавливает флаги по умолчанию для региона
     */
    private void setDefaultRegionFlags(Region region, boolean isGlobal) {
        if (isGlobal) {
            region.setFlag("build", "allow");
            region.setFlag("entry", "allow");
            region.setFlag("break", "allow");
            region.setFlag("place", "allow");
            region.setFlag("interact", "allow");
            region.setFlag("pvp", "allow");
            region.setFlag("use-items", "allow");
            region.setFlag("enderchest", "allow");
            region.setFlag("time-lock", "allow");
            region.setFlag("exit", "allow");
            region.setFlag("chest-access", "allow");
            region.setFlag("tnt-explosion", "allow");
            region.setFlag("creeper-explosion", "allow");
            region.setFlag("explosions", "allow");
            region.setFlag("enter-notify", "allow");
            region.setFlag("exit-notify", "allow");
        } else {
            region.setFlag("build", "deny");
            region.setFlag("entry", "allow");
            region.setFlag("break", "deny");
            region.setFlag("place", "deny");
            region.setFlag("interact", "deny");
            region.setFlag("pvp", "allow");
            region.setFlag("use-items", "deny");
            region.setFlag("enderchest", "deny");
            region.setFlag("time-lock", "allow");
            region.setFlag("exit", "allow");
            region.setFlag("chest-access", "deny");
            region.setFlag("block-chat", "allow");
            region.setFlag("tnt-explosion", "deny");      // ЗАПРЕЩЕНО
            region.setFlag("creeper-explosion", "deny");  // ЗАПРЕЩЕНО
            region.setFlag("explosions", "deny");         // ЗАПРЕЩЕНО
            region.setFlag("enter-notify", "deny");
            region.setFlag("exit-notify", "deny");
        }
    }

    /**
     * Создает регион с текущими выделенными позициями игрока
     */
    public boolean createRegion(String name, Player player) {
        Location pos1 = getPos1(player);
        Location pos2 = getPos2(player);

        if (pos1 == null || pos2 == null) {
            player.sendMessage("§cСначала установите обе позиции! Используйте /regionmc pos1 и /regionmc pos2");
            return false;
        }

        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage("§cПозиции должны быть в одном мире!");
            return false;
        }

        return createRegion(name, player, pos1, pos2);
    }

    /**
     * Создает мировой регион
     */
    public boolean createWorldRegion(String name, Location pos1, Location pos2) {
        if (regions.containsKey(name)) {
            Region existingRegion = regions.get(name);
            existingRegion.setPos1(pos1);
            existingRegion.setPos2(pos2);
            saveRegion(existingRegion);
            return true;
        }

        Region region = new Region(name, new UUID(0, 0), pos1, pos2);
        region.setPriority(1);

        if (name.startsWith("__Global__")) {
            setDefaultRegionFlags(region, true);
        }

        regions.put(name, region);
        saveRegion(region);
        return true;
    }

    /**
     * Удаляет регион
     */
    public boolean removeRegion(String name, Player player) {
        Region region = getRegion(name);
        if (region == null) {
            if (player != null) player.sendMessage("§cРегион '" + name + "' не найден!");
            return false;
        }

        if (player != null && !hasModifyPermission(player, name)) {
            player.sendMessage("§cУ вас нет прав для удаления этого региона!");
            return false;
        }

        regions.remove(region.getName());
        deleteRegionFile(region.getName());

        if (player != null) player.sendMessage("§aРегион '" + name + "' успешно удален!");
        return true;
    }

    public boolean removeRegion(String name) {
        return removeRegion(name, null);
    }

    /**
     * Проверяет существование региона
     */
    public boolean regionExists(String name) {
        return regions.containsKey(name);
    }

    /**
     * Получает регион по имени
     */
    public Region getRegion(String name) {
        Region exactMatch = regions.get(name);
        if (exactMatch != null) {
            return exactMatch;
        }

        return regions.values().stream()
                .filter(region -> region.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
    *
    */
    public boolean canExitRegion(UUID playerId, String regionName) {
        Region region = getRegion(regionName);
        if (region == null) return true;
        if (region.isOwner(playerId) || region.isMember(playerId)) return true;
        return getFlagBooleanValue(region, "exit", true);
    }

    // =============================================
    // МЕТОДЫ ДЛЯ РАБОТЫ С ИГРОКАМИ
    // =============================================

    public List<Region> getPlayerRegions(UUID playerId) {
        return regions.values().stream()
                .filter(region -> region.isOwner(playerId) || region.isMember(playerId))
                .collect(Collectors.toList());
    }

    public List<String> getPlayerRegionNames(UUID playerId) {
        return regions.values().stream()
                .filter(region -> region.isOwner(playerId) || region.isMember(playerId))
                .map(Region::getName)
                .collect(Collectors.toList());
    }

    // =============================================
    // МЕТОДЫ ДЛЯ ПРОВЕРКИ ДОСТУПА
    // =============================================

    public boolean isGlobalRegion(String regionName) {
        return regionName != null && regionName.startsWith("__Global__");
    }

    private boolean checkRegionAccess(UUID playerId, String regionName, String flagName) {
        Region region = getRegion(regionName);
        if (region == null) return true;

        if (region.isOwner(playerId) || region.isMember(playerId)) {
            return true;
        }

        boolean flagValue = getFlagBooleanValue(region, flagName, false);

        if (isGlobalRegion(regionName)) {
            Object rawFlagValue = region.getFlag(flagName);
            return rawFlagValue == null || flagValue;
        }

        return flagValue;
    }

    public boolean canBuildInRegion(UUID playerId, String regionName) {
        return checkRegionAccess(playerId, regionName, "build");
    }

    public boolean canBreakInRegion(UUID playerId, String regionName) {
        return checkRegionAccess(playerId, regionName, "break");
    }

    public boolean canEnterRegion(UUID playerId, String regionName) {
        Region region = getRegion(regionName);
        if (region == null) return true;

        if (isOwner(playerId, regionName) || isMember(playerId, regionName)) {
            return true;
        }

        return getFlagBooleanValue(region, "entry", true);
    }

    public boolean getFlagBooleanValue(Region region, String flagName, boolean defaultValue) {
        if (region == null) return defaultValue;

        Object flagValue = region.getFlag(flagName);
        if (flagValue == null) return defaultValue;

        if (flagValue instanceof String) {
            String strValue = ((String) flagValue).toLowerCase().trim();

            if (strValue.equals("true") || strValue.equals("allow") || strValue.equals("yes") ||
                    strValue.equals("1") || strValue.equals("разрешить") ||
                    flagValue.equals("ALLOW") || flagValue.equals("Allow")) {
                return true;
            }

            if (strValue.equals("false") || strValue.equals("deny") || strValue.equals("no") ||
                    strValue.equals("0") || strValue.equals("запретить") ||
                    flagValue.equals("DENY") || flagValue.equals("Deny")) {
                return false;
            }

            return defaultValue;
        }

        if (flagValue instanceof Boolean) {
            return (Boolean) flagValue;
        }

        return Boolean.parseBoolean(flagValue.toString());
    }

    public boolean canPlaceInRegion(UUID playerId, String regionName) {
        return checkRegionAccess(playerId, regionName, "place");
    }

    public boolean canInteractInRegion(UUID playerId, String regionName) {
        return checkRegionAccess(playerId, regionName, "interact");
    }

    public boolean canUseInRegion(UUID playerId, String regionName) {
        return checkRegionAccess(playerId, regionName, "use-items");
    }

    public boolean canPvPInRegion(String regionName) {
        Region region = getRegion(regionName);
        if (region == null) return true;
        return getFlagBooleanValue(region, "pvp", true);
    }

    public boolean canOpenEnderChestInRegion(UUID playerId, String regionName) {
        return checkRegionAccess(playerId, regionName, "enderchest");
    }

    // =============================================
// МЕТОДЫ ДЛЯ ПРОВЕРКИ ДОСТУПА С УЧЁТОМ АДМИНИСТРАТОРА
// =============================================

    public boolean isAdminBypass(Player player) {
        return hasAdminBypass(player);
    }

    public boolean canBuildInRegion(Player player, String regionName) {
        if (hasAdminBypass(player)) return true;
        return canBuildInRegion(player.getUniqueId(), regionName);
    }

    public boolean canBreakInRegion(Player player, String regionName) {
        if (hasAdminBypass(player)) return true;
        return canBreakInRegion(player.getUniqueId(), regionName);
    }

    public boolean canPlaceInRegion(Player player, String regionName) {
        if (hasAdminBypass(player)) return true;
        return canPlaceInRegion(player.getUniqueId(), regionName);
    }

    public boolean canInteractInRegion(Player player, String regionName) {
        if (hasAdminBypass(player)) return true;
        return canInteractInRegion(player.getUniqueId(), regionName);
    }

    public boolean canUseInRegion(Player player, String regionName) {
        if (hasAdminBypass(player)) return true;
        return canUseInRegion(player.getUniqueId(), regionName);
    }

    public boolean canOpenEnderChestInRegion(Player player, String regionName) {
        if (hasAdminBypass(player)) return true;
        return canOpenEnderChestInRegion(player.getUniqueId(), regionName);
    }

    public boolean canEnterRegion(Player player, String regionName) {
        if (hasAdminBypass(player)) return true;
        return canEnterRegion(player.getUniqueId(), regionName);
    }

    public boolean canExitRegion(Player player, String regionName) {
        if (hasAdminBypass(player)) return true;
        return canExitRegion(player.getUniqueId(), regionName);
    }

    // =============================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ ПРОВЕРКИ ПРАВ
    // =============================================

    public boolean isOwner(UUID playerId, String regionName) {
        Region region = getRegion(regionName);
        return region != null && region.isOwner(playerId);
    }

    public boolean isMember(UUID playerId, String regionName) {
        Region region = getRegion(regionName);
        return region != null && region.isMember(playerId);
    }

    // =============================================
    // МЕТОДЫ ДЛЯ РАБОТЫ С ЛОКАЦИЯМИ
    // =============================================

    public List<Region> getRegionsAtLocation(Location location) {
        return regions.values().stream()
                .filter(region -> region.contains(location))
                .sorted(Comparator.comparingInt(Region::getPriority).reversed())
                .collect(Collectors.toList());
    }

    public Region getRegionAtLocation(Location location) {
        List<Region> regionsAtLocation = getRegionsAtLocation(location);
        return regionsAtLocation.isEmpty() ? null : regionsAtLocation.get(0);
    }

    public Region getGlobalRegion(World world) {
        String globalRegionName = "__Global__" + world.getName();
        return regions.get(globalRegionName);
    }

    private String formatLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return "Неверная локация";
        }
        return String.format("Мир: %s, X: %.1f, Y: %.1f, Z: %.1f",
                loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    public int calculateVolume(Location pos1, Location pos2) {
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public void setPos1(Player player, Location location) {
        if (location == null || location.getWorld() == null) {
            if (player != null) {
                player.sendMessage("§cОшибка: неверная локация!");
            }
            if (player != null) {
                pos1Selections.remove(player.getUniqueId());
            }
            return;
        }

        if (player != null) {
            pos1Selections.put(player.getUniqueId(), location);
            player.sendMessage("§aПозиция 1 установлена: " + formatLocation(location));
        }
    }

    public void setPos2(Player player, Location location) {
        if (location == null || location.getWorld() == null) {
            if (player != null) {
                player.sendMessage("§cОшибка: неверная локация!");
            }
            if (player != null) {
                pos2Selections.remove(player.getUniqueId());
            }
            return;
        }

        if (player != null) {
            pos2Selections.put(player.getUniqueId(), location);
            player.sendMessage("§aПозиция 2 установлена: " + formatLocation(location));
        }
    }

    public void setPos1Silently(Player player, Location location) {
        if (player == null || location == null) {
            if (player != null) {
                pos1Selections.remove(player.getUniqueId());
            }
            return;
        }

        if (location.getWorld() == null) {
            plugin.getLogger().warning("Attempted to set pos1 with null world for player: " + player.getName());
            return;
        }

        pos1Selections.put(player.getUniqueId(), location);
    }

    public void setPos2Silently(Player player, Location location) {
        if (player == null || location == null) {
            if (player != null) {
                pos2Selections.remove(player.getUniqueId());
            }
            return;
        }

        if (location.getWorld() == null) {
            plugin.getLogger().warning("Attempted to set pos2 with null world for player: " + player.getName());
            return;
        }

        pos2Selections.put(player.getUniqueId(), location);
    }

    public Location getPos1(Player player) {
        if (player == null) return null;

        Location pos1 = pos1Selections.get(player.getUniqueId());
        if (pos1 != null && pos1.getWorld() == null) {
            plugin.getLogger().warning("Found pos1 with null world for player: " + player.getName());
            return null;
        }
        return pos1;
    }

    public Location getPos2(Player player) {
        if (player == null) return null;

        Location pos2 = pos2Selections.get(player.getUniqueId());
        if (pos2 != null && pos2.getWorld() == null) {
            plugin.getLogger().warning("Found pos2 with null world for player: " + player.getName());
            return null;
        }
        return pos2;
    }

    // =============================================
    // УНИФИЦИРОВАННЫЕ МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ
    // =============================================

    private Region validateRegionAccess(String regionName, Player executor, String operation) {
        Region region = getRegion(regionName);
        if (region == null) {
            if (executor != null) executor.sendMessage("§cРегион '" + regionName + "' не найден!");
            return null;
        }

        if (executor != null && !hasModifyPermission(executor, regionName)) {
            executor.sendMessage("§cУ вас нет прав для " + operation + " этого региона!");
            return null;
        }

        return region;
    }

    private Player findPlayer(String playerName, Player executor) {
        Player targetPlayer = Bukkit.getPlayerExact(playerName);
        if (targetPlayer == null && executor != null) {
            executor.sendMessage("§cИгрок '" + playerName + "' не найден или не в сети!");
        }
        return targetPlayer;
    }

    private boolean modifyRegionMembership(String regionName, String playerName, Player executor,
                                           String operation, boolean isOwnerOperation) {
        Region region = validateRegionAccess(regionName, executor, "изменения");
        if (region == null) return false;

        Player targetPlayer = findPlayer(playerName, executor);
        if (targetPlayer == null) return false;

        boolean result;
        String successMessage;
        String failMessage;

        if ("add".equals(operation)) {
            if (isOwnerOperation) {
                result = region.addOwner(targetPlayer.getUniqueId());
                successMessage = "§aИгрок '" + playerName + "' добавлен в регион '" + regionName + "' как владелец!";
                failMessage = "§cИгрок '" + playerName + "' уже является владельцем региона!";
            } else {
                result = region.addMember(targetPlayer.getUniqueId());
                successMessage = "§aИгрок '" + playerName + "' добавлен в регион '" + regionName + "' как участник!";
                failMessage = "§cИгрок '" + playerName + "' уже является участником региона!";
            }
        } else {
            if (isOwnerOperation) {
                result = region.removeOwner(targetPlayer.getUniqueId());
                successMessage = "§aИгрок '" + playerName + "' удален из владельцев региона '" + regionName + "'!";
                failMessage = "§cИгрок '" + playerName + "' не является владельцем региона!";
            } else {
                result = region.removeMember(targetPlayer.getUniqueId());
                successMessage = "§aИгрок '" + playerName + "' удален из участников региона '" + regionName + "'!";
                failMessage = "§cИгрок '" + playerName + "' не является участником региона!";
            }
        }

        if (result) {
            saveRegion(region);
            if (executor != null) {
                executor.sendMessage(successMessage);
            }
        } else if (executor != null) {
            executor.sendMessage(failMessage);
        }
        return result;
    }

    // =============================================
    // МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ УЧАСТНИКАМИ
    // =============================================

    public boolean addMember(String regionName, String playerName, Player executor) {
        return modifyRegionMembership(regionName, playerName, executor, "add", false);
    }

    public boolean addOwner(String regionName, String playerName, Player executor) {
        return modifyRegionMembership(regionName, playerName, executor, "add", true);
    }

    public boolean removeMember(String regionName, String playerName, Player executor) {
        return modifyRegionMembership(regionName, playerName, executor, "remove", false);
    }

    public boolean removeOwner(String regionName, String playerName, Player executor) {
        return modifyRegionMembership(regionName, playerName, executor, "remove", true);
    }

    public boolean addMember(String regionName, String playerName) {
        return addMember(regionName, playerName, null);
    }

    public boolean addOwner(String regionName, String playerName) {
        return addOwner(regionName, playerName, null);
    }

    public boolean removeMember(String regionName, String playerName) {
        return removeMember(regionName, playerName, null);
    }

    public boolean removeOwner(String regionName, String playerName) {
        return removeOwner(regionName, playerName, null);
    }

    // =============================================
    // МЕТОДЫ ДЛЯ УПРАВЛЕНИЯ ПРИОРИТЕТОМ
    // =============================================

    public boolean setPriority(String regionName, int priority, Player player) {
        Region region = validateRegionAccess(regionName, player, "изменения приоритета");
        if (region == null) return false;

        region.setPriority(priority);
        saveRegion(region);

        if (player != null) player.sendMessage("§aПриоритет региона '" + regionName + "' установлен на " + priority + "!");
        return true;
    }

    public boolean setPriority(String regionName, int priority) {
        return setPriority(regionName, priority, null);
    }

    // =============================================
    // МЕТОДЫ ДЛЯ РАБОТЫ С ЛИМИТАМИ РАЗМЕРА
    // =============================================

    private File findLimitFile() {
        // Приоритетный поиск в папке Settings
        String[] possiblePaths = {
                "Settings/limit.yml",
                "settings/limit.yml",
                "SETTINGS/limit.yml",
                "Setting/limit.yml",
                "limit.yml"
        };

        File dataFolder = plugin.getDataFolder();

        for (String path : possiblePaths) {
            File file = new File(dataFolder, path);
            if (file.exists()) {
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info("Found limit.yml at: " + file.getPath());
                }
                return file;
            }
        }

        // Если не нашли - создаем в корне
        File defaultFile = new File(dataFolder, "limit.yml");
        if (!defaultFile.exists()) {
            createDefaultLimitFile(defaultFile);
        }
        return defaultFile;
    }

    private void createDefaultLimitFile(File file) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            YamlConfiguration config = new YamlConfiguration();
            config.set("Enable", true);
            config.set("size-limits.enable-size-checks", true);
            config.set("size-limits.min-size", 5);
            config.set("size-limits.max-size", 100000);
            config.set("size-limits.max-height", 256);
            config.set("default-max-regions", 5);
            config.set("check-priority", "permission-first");
            config.save(file);

            plugin.getLogger().info("Created default limit.yml at: " + file.getPath());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create default limit.yml: " + e.getMessage());
        }
    }

    public boolean areLimitsEnabled() {
        try {
            File limitFile = findLimitFile();
            if (limitFile == null) return false;

            YamlConfiguration limitConfig = YamlConfiguration.loadConfiguration(limitFile);
            return limitConfig.getBoolean("Enable", false);
        } catch (Exception e) {
            return false;
        }
    }

    // =============================================
    // МЕТОДЫ ДЛЯ РАБОТЫ С РАЗМЕРАМИ РЕГИОНОВ ИЗ limit.yml
    // =============================================

    public boolean areSizeChecksEnabled() {
        try {
            File limitFile = findLimitFile();
            if (limitFile == null) return true;

            YamlConfiguration limitConfig = YamlConfiguration.loadConfiguration(limitFile);

            if (!limitConfig.getBoolean("Enable", true)) {
                return false;
            }

            return limitConfig.getBoolean("size-limits.enable-size-checks", true);
        } catch (Exception e) {
            return true;
        }
    }

    public int getMinRegionSize() {
        try {
            File limitFile = findLimitFile();
            if (limitFile == null) return 5;

            YamlConfiguration limitConfig = YamlConfiguration.loadConfiguration(limitFile);

            if (!limitConfig.getBoolean("Enable", true)) {
                return 0;
            }

            int minSize = limitConfig.getInt("size-limits.min-size", 5);

            // -1 или 0 → безлимитный минимум
            if (minSize <= 0) {
                return 0;
            }

            return minSize;
        } catch (Exception e) {
            return 5;
        }
    }

    // В методе getMaxRegionHeight()
    public int getMaxRegionHeight() {
        try {
            File limitFile = findLimitFile();
            if (limitFile == null) return 32000;

            YamlConfiguration limitConfig = YamlConfiguration.loadConfiguration(limitFile);

            if (!limitConfig.getBoolean("Enable", true)) {
                return 32000;
            }

            int maxHeight = limitConfig.getInt("size-limits.max-height", 256);

            // -1 или 0 → безлимитно (возвращаем максимальную высоту мира)
            if (maxHeight <= 0) {
                return Integer.MAX_VALUE; // Важно! Не 32000, а Integer.MAX_VALUE
            }

            return maxHeight;
        } catch (Exception e) {
            return 256;
        }
    }

    public int getMaxRegionSizeForPlayer(Player player) {
        if (!areLimitsEnabled()) {
            return -1;
        }

        if (player == null) {
            return getDefaultLimit();
        }

        if (player.hasPermission("regionmc.limit.unlimited") ||
                player.hasPermission("regionmc.admin") ||
                player.isOp()) {
            return -1;
        }

        try {
            File limitFile = findLimitFile();
            if (limitFile == null) {
                return getDefaultLimit();
            }

            YamlConfiguration limitConfig = YamlConfiguration.loadConfiguration(limitFile);

            if (!limitConfig.getBoolean("Enable", true)) {
                return -1;
            }

            String checkPriority = limitConfig.getString("check-priority", "permission-first");
            int defaultLimit = limitConfig.getInt("size-limits.max-size", 100000);

            if ("permission-first".equalsIgnoreCase(checkPriority)) {
                int permissionLimit = getPermissionLimit(player, limitConfig);
                if (permissionLimit != Integer.MIN_VALUE) {
                    return permissionLimit;
                }
                return defaultLimit;
            } else {
                int groupLimit = getGroupLimitForSize(player, limitConfig);
                if (groupLimit != Integer.MIN_VALUE) {
                    return groupLimit;
                }
                return defaultLimit;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при получении лимита размера: " + e.getMessage());
            return 100000;
        }
    }

    private int getGroupLimitForSize(Player player, YamlConfiguration limitConfig) {
        try {
            ConfigurationSection groupLimits = limitConfig.getConfigurationSection("group-limits");
            if (groupLimits == null) return Integer.MIN_VALUE;

            List<Integer> availableLimits = new ArrayList<>();

            for (String group : groupLimits.getKeys(false)) {
                ConfigurationSection groupSection = groupLimits.getConfigurationSection(group);
                if (groupSection == null) continue;

                int limit = groupSection.getInt("size", -1);
                if (limit == -1) continue;

                boolean hasGroup = player.hasPermission("regionmc.group." + group) ||
                        player.hasPermission("group." + group) ||
                        hasVaultGroup(player, group);

                if (hasGroup) {
                    availableLimits.add(limit);
                }
            }

            if (!availableLimits.isEmpty()) {
                availableLimits.sort(Collections.reverseOrder());
                return availableLimits.get(0);
            }

            return Integer.MIN_VALUE;
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    private int getPermissionLimit(Player player, YamlConfiguration limitConfig) {
        try {
            if (limitConfig == null) {
                File limitFile = findLimitFile();
                if (limitFile == null) return Integer.MIN_VALUE;
                limitConfig = YamlConfiguration.loadConfiguration(limitFile);
            }

            List<?> permissionLimitsList = limitConfig.getList("permission-limits", new ArrayList<>());
            if (permissionLimitsList.isEmpty()) return Integer.MIN_VALUE;

            List<Map<?, ?>> permissionLimits = new ArrayList<>();
            for (Object obj : permissionLimitsList) {
                if (obj instanceof Map) {
                    permissionLimits.add((Map<?, ?>) obj);
                }
            }

            List<Integer> availableLimits = new ArrayList<>();
            for (Map<?, ?> limitMap : permissionLimits) {
                String permission = (String) limitMap.get("permission");
                Object limitObj = limitMap.get("limit");

                if (permission == null || limitObj == null) continue;

                int limit = convertToInt(limitObj);
                if (player.hasPermission(permission)) {
                    availableLimits.add(limit);
                }
            }

            if (!availableLimits.isEmpty()) {
                availableLimits.sort(Collections.reverseOrder());
                return availableLimits.get(0);
            }

            return Integer.MIN_VALUE;
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    private int getGroupLimit(Player player, YamlConfiguration limitConfig) {
        try {
            ConfigurationSection groupLimits = limitConfig.getConfigurationSection("group-limits");
            if (groupLimits == null) return Integer.MIN_VALUE;

            List<Integer> availableLimits = new ArrayList<>();
            for (String group : groupLimits.getKeys(false)) {
                Object limitObj = groupLimits.get(group);
                if (limitObj != null) {
                    int limit = convertToInt(limitObj);

                    boolean hasGroup = player.hasPermission("regionmc.group." + group) ||
                            player.hasPermission("group." + group) ||
                            hasVaultGroup(player, group);

                    if (hasGroup) {
                        availableLimits.add(limit);
                    }
                }
            }

            if (!availableLimits.isEmpty()) {
                availableLimits.sort(Collections.reverseOrder());
                return availableLimits.get(0);
            }

            return Integer.MIN_VALUE;
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    public int getDefaultLimit() {
        try {
            File limitFile = findLimitFile();
            if (limitFile == null) return 26000;

            YamlConfiguration limitConfig = YamlConfiguration.loadConfiguration(limitFile);
            return limitConfig.getInt("default-limit", 26000);
        } catch (Exception e) {
            return 26000;
        }
    }

    private boolean hasVaultGroup(Player player, String group) {
        try {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                return player.hasPermission("group." + group);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Vault не доступен: " + e.getMessage());
        }
        return false;
    }

    private int convertToInt(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Double) return ((Double) obj).intValue();
        if (obj instanceof Long) return ((Long) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    // =============================================
    // МЕТОДЫ ДЛЯ ПРОВЕРКИ КОЛИЧЕСТВА РЕГИОНОВ ИГРОКА
    // =============================================

    /**
     * Главный метод получения максимального количества регионов для игрока
     */
    public int getMaxRegionsForPlayer(Player player) {
        // Администраторы и ОП - безлимит
        if (player.hasPermission("regionmc.admin") || player.isOp()) {
            return -1;
        }

        // ========== 1. ПРОВЕРКА ПРАВ ИЗ permission-limits В limit.yml ==========
        int permissionLimit = getMaxRegionsFromPermissions(player);
        if (permissionLimit != Integer.MIN_VALUE) {
            return permissionLimit;
        }

        // ========== 2. ПРОВЕРКА ПРЯМЫХ ПРАВ (без limit.yml) ==========
        int directPermissionLimit = getMaxRegionsFromDirectPermissions(player);
        if (directPermissionLimit != Integer.MIN_VALUE) {
            return directPermissionLimit;
        }

        // ========== 3. ПРОВЕРКА ГРУПП ИЗ limit.yml ==========
        int groupLimit = getMaxRegionsFromGroups(player);
        if (groupLimit != Integer.MIN_VALUE) {
            return groupLimit;
        }

        // ========== 4. ЗНАЧЕНИЕ ПО УМОЛЧАНИЮ ==========
        int limitYmlLimit = getDefaultMaxRegionsFromLimitYml();
        if (limitYmlLimit > 0) {
            return limitYmlLimit;
        }

        int mainYmlLimit = getMaxRegionsPerPlayerFromMainConfig();
        if (mainYmlLimit > 0) {
            return mainYmlLimit;
        }

        return 5; // Значение по умолчанию
    }

    /**
     * Проверяет права напрямую (без чтения limit.yml)
     * Права: regionmc.maxregions.1, regionmc.maxregions.5, regionmc.maxregions.10 и т.д.
     */
    private int getMaxRegionsFromDirectPermissions(Player player) {
        // Список возможных лимитов (от большего к меньшему)
        int[] possibleLimits = {1000, 500, 250, 100, 50, 25, 20, 15, 10, 5, 3, 2, 1};

        for (int limit : possibleLimits) {
            String permission = "regionmc.maxregions." + limit;
            if (player.hasPermission(permission)) {
                plugin.getLogger().info("Player " + player.getName() + " has direct permission " + permission + " -> limit " + limit);
                return limit;
            }
        }

        // Проверка на безлимит
        if (player.hasPermission("regionmc.maxregions.unlimited")) {
            plugin.getLogger().info("Player " + player.getName() + " has unlimited regions permission");
            return -1;
        }

        return Integer.MIN_VALUE;
    }

    /**
     * Получает лимит из permission-limits в limit.yml
     */
    private int getMaxRegionsFromPermissions(Player player) {
        try {
            File limitFile = findLimitFile();
            if (limitFile == null) return Integer.MIN_VALUE;

            YamlConfiguration limitConfig = YamlConfiguration.loadConfiguration(limitFile);
            List<?> permissionLimitsList = limitConfig.getList("permission-limits", new ArrayList<>());

            if (permissionLimitsList.isEmpty()) return Integer.MIN_VALUE;

            // Собираем все доступные лимиты и выбираем максимальный
            int maxFound = Integer.MIN_VALUE;

            for (Object obj : permissionLimitsList) {
                if (obj instanceof Map<?, ?> limitMap) {
                    String permission = (String) limitMap.get("permission");
                    Object maxRegionsObj = limitMap.get("max-regions");

                    if (permission != null && maxRegionsObj != null) {
                        if (player.hasPermission(permission)) {
                            int limit = convertToInt(maxRegionsObj);
                            plugin.getLogger().info("Player " + player.getName() + " has permission " + permission + " from limit.yml -> limit " + limit);
                            if (limit > maxFound) {
                                maxFound = limit;
                            }
                        }
                    }
                }
            }

            if (maxFound != Integer.MIN_VALUE) {
                return maxFound;
            }

            return Integer.MIN_VALUE;
        } catch (Exception e) {
            plugin.getLogger().warning("Error reading permission limits: " + e.getMessage());
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Получает лимит из групп в limit.yml
     */
    private int getMaxRegionsFromGroups(Player player) {
        try {
            File limitFile = findLimitFile();
            if (limitFile == null) return Integer.MIN_VALUE;

            YamlConfiguration limitConfig = YamlConfiguration.loadConfiguration(limitFile);
            ConfigurationSection groupLimits = limitConfig.getConfigurationSection("group-limits");
            if (groupLimits == null) return Integer.MIN_VALUE;

            int maxFound = Integer.MIN_VALUE;

            for (String group : groupLimits.getKeys(false)) {
                ConfigurationSection groupSection = groupLimits.getConfigurationSection(group);
                if (groupSection == null) continue;

                int maxRegions = groupSection.getInt("max-regions", -1);
                if (maxRegions == -1) continue;

                boolean hasGroup = player.hasPermission("regionmc.group." + group) ||
                        player.hasPermission("group." + group) ||
                        hasVaultGroup(player, group);

                if (hasGroup) {
                    plugin.getLogger().info("Player " + player.getName() + " is in group " + group + " -> max-regions " + maxRegions);
                    if (maxRegions > maxFound) {
                        maxFound = maxRegions;
                    }
                }
            }

            if (maxFound != Integer.MIN_VALUE) {
                return maxFound;
            }

            return Integer.MIN_VALUE;
        } catch (Exception e) {
            plugin.getLogger().warning("Error reading group limits: " + e.getMessage());
            return Integer.MIN_VALUE;
        }
    }

    private int getDefaultMaxRegionsFromLimitYml() {
        try {
            File limitFile = findLimitFile();
            if (limitFile == null) return -1;

            YamlConfiguration limitConfig = YamlConfiguration.loadConfiguration(limitFile);
            if (!limitConfig.getBoolean("Enable", true)) return -1;

            return limitConfig.getInt("default-max-regions", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    private int getMaxRegionsPerPlayerFromMainConfig() {
        try {
            File mainFile = new File(plugin.getDataFolder(), "main.yml");
            if (!mainFile.exists()) return -1;

            YamlConfiguration mainConfig = YamlConfiguration.loadConfiguration(mainFile);
            return mainConfig.getInt("regions.max-regions-per-player", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    public int getPlayerRegionsCount(UUID playerId) {
        if (playerId == null) return 0;

        int count = 0;
        for (Region region : regions.values()) {
            if (isGlobalRegion(region.getName())) continue;
            if (region.isOwner(playerId)) count++;
        }
        return count;
    }

    public String getMaxRegionsMessage(Player player) {
        int maxRegions = getMaxRegionsForPlayer(player);
        int currentRegions = getPlayerRegionsCount(player.getUniqueId());

        if (maxRegions == -1) {
            return "§aУ вас безлимитный доступ к созданию регионов!";
        }

        int remaining = maxRegions - currentRegions;
        if (remaining <= 0) {
            return "§cВы достигли лимита регионов! Максимум: §e" + maxRegions +
                    " §cрегионов, у вас: §e" + currentRegions;
        }

        return "§7У вас осталось §e" + remaining + " §7из §e" + maxRegions + " §7доступных регионов";
    }

    public boolean canPlayerCreateRegion(Player player) {
        int maxRegions = getMaxRegionsForPlayer(player);
        if (maxRegions == -1) return true;

        int currentRegions = getPlayerRegionsCount(player.getUniqueId());
        return currentRegions < maxRegions;
    }

    // =============================================
    // МЕТОДЫ ДЛЯ РАБОТЫ С ФАЙЛАМИ
    // =============================================

    private void loadRegions() {
        if (!regionsFolder.exists()) {
            if (!regionsFolder.mkdirs()) {
                plugin.getLogger().warning("Failed to create regions folder: " + regionsFolder.getPath());
            }
            return;
        }

        File[] regionFiles = regionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (regionFiles == null) return;

        int loadedCount = 0;
        for (File file : regionFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Map<String, Object> data = config.getValues(false);

                Region region = new Region(data);
                regions.put(region.getName(), region);
                loadedCount++;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load region from file: " + file.getName() + " - " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + loadedCount + " regions");
    }

    public TimeLockManager getTimeLockManager() {
        return timeLockManager;
    }

    public void saveRegion(Region region) {
        try {
            File file = new File(regionsFolder, region.getName() + ".yml");
            YamlConfiguration config = new YamlConfiguration();

            Map<String, Object> data = region.serialize();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }

            config.save(file);

            if (timeLockManager != null) {
                Object timeLockFlag = region.getFlag("time-lock");
                if (timeLockFlag != null && timeLockFlag.toString().equalsIgnoreCase("deny")) {
                    if (region.getWorld() != null) {
                        timeLockManager.updateFixedTime(region.getWorld().getName(), region.getWorld().getTime());
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save region: " + region.getName() + " - " + e.getMessage());
        }
    }

    private void deleteRegionFile(String regionName) {
        File file = new File(regionsFolder, regionName + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Failed to delete region file: " + file.getName());
        }
    }

    // =============================================
    // ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ
    // =============================================

    public List<Region> getAvailableRegionsForPlayer(Player player) {
        if (player == null) return new ArrayList<>(regions.values());
        return getPlayerRegions(player.getUniqueId());
    }

    public Location getRegionPos1(String regionName) {
        Region region = getRegion(regionName);
        return region != null ? region.getPos1() : null;
    }

    public Location getRegionPos2(String regionName) {
        Region region = getRegion(regionName);
        return region != null ? region.getPos2() : null;
    }

    public void reload() {
        regions.clear();
        loadRegions();
        printConsoleRegionStats();
    }

    public boolean getFlagValueWithPriority(Location location, String flag, boolean defaultValue) {
        return getRegionsAtLocation(location).stream()
                .map(region -> region.getFlag(flag))
                .filter(Objects::nonNull)
                .findFirst()
                .map(flagValue -> getFlagBooleanValue(getRegionAtLocation(location), flag, defaultValue))
                .orElse(defaultValue);
    }

    // =============================================
    // МЕТОДЫ ДЛЯ РАБОТЫ С РЕГИОНАМИ
    // =============================================

    public boolean hasRegionConflict(Location pos1, Location pos2, String excludeRegionName) {
        return regions.values().stream()
                .filter(region -> !region.getName().equals(excludeRegionName))
                .filter(region -> !isGlobalRegion(region.getName()))
                .anyMatch(region -> regionsIntersect(region.getPos1(), region.getPos2(), pos1, pos2));
    }

    private boolean regionsIntersect(Location r1Pos1, Location r1Pos2, Location r2Pos1, Location r2Pos2) {
        if (r1Pos1 == null || r1Pos2 == null || r2Pos1 == null || r2Pos2 == null) return false;

        if (!r1Pos1.getWorld().equals(r2Pos1.getWorld()) ||
                !r1Pos1.getWorld().equals(r1Pos2.getWorld()) ||
                !r2Pos1.getWorld().equals(r2Pos2.getWorld())) return false;

        double r1MinX = Math.min(r1Pos1.getX(), r1Pos2.getX());
        double r1MaxX = Math.max(r1Pos1.getX(), r1Pos2.getX());
        double r1MinY = Math.min(r1Pos1.getY(), r1Pos2.getY());
        double r1MaxY = Math.max(r1Pos1.getY(), r1Pos2.getY());
        double r1MinZ = Math.min(r1Pos1.getZ(), r1Pos2.getZ());
        double r1MaxZ = Math.max(r1Pos1.getZ(), r1Pos2.getZ());

        double r2MinX = Math.min(r2Pos1.getX(), r2Pos2.getX());
        double r2MaxX = Math.max(r2Pos1.getX(), r2Pos2.getX());
        double r2MinY = Math.min(r2Pos1.getY(), r2Pos2.getY());
        double r2MaxY = Math.max(r2Pos1.getY(), r2Pos2.getY());
        double r2MinZ = Math.min(r2Pos1.getZ(), r2Pos2.getZ());
        double r2MaxZ = Math.max(r2Pos1.getZ(), r2Pos2.getZ());

        return (r1MinX <= r2MaxX && r1MaxX >= r2MinX) &&
                (r1MinY <= r2MaxY && r1MaxY >= r2MinY) &&
                (r1MinZ <= r2MaxZ && r1MaxZ >= r2MinZ);
    }

    public void saveRegions() {
        for (Region region : regions.values()) {
            saveRegion(region);
        }
        plugin.getLogger().info("Все регионы сохранены (" + regions.size() + " шт.)");
    }

    public boolean hasModifyPermission(Player player, String regionName) {
        if (hasAdminBypass(player)) return true;
        Region region = getRegion(regionName);
        if (region == null) return false;
        return region.isOwner(player.getUniqueId()) || player.hasPermission("regionmc.admin");
    }

    public boolean canModifyRegion(Player player, String regionName) {
        return hasModifyPermission(player, regionName);
    }

    // =============================================
    // GETTERS
    // =============================================

    public File getRegionsFolder() {
        return regionsFolder;
    }

    public Collection<Region> getAllRegions() {
        return new ArrayList<>(regions.values());
    }

    public int getRegionCount() {
        return regions.size();
    }

    // =============================================
    // МЕТОДЫ ДЛЯ СТАТИСТИКИ И УВЕДОМЛЕНИЙ
    // =============================================

    public void printConsoleRegionStats() {
        plugin.getLogger().info("=== Статистика регионов ===");
        plugin.getLogger().info("Всего регионов: " + getRegionCount());
        plugin.getLogger().info("Загружено из файлов: " + regions.size());
        plugin.getLogger().info("Регионы в памяти: " + getAllRegions().size());
    }

    public void showLimitInfo(Player player) {
        if (!areLimitsEnabled()) {
            player.sendMessage("§6=== Информация о лимитах ===");
            player.sendMessage("§aВсе лимиты ОТКЛЮЧЕНЫ (Enable: false)");
            player.sendMessage("§7Игроки могут создавать регионы без ограничений");
            return;
        }

        int maxSize = getMaxRegionSizeForPlayer(player);
        int maxRegions = getMaxRegionsForPlayer(player);
        int currentRegions = getPlayerRegionsCount(player.getUniqueId());
        int minSize = getMinRegionSize();
        int maxHeight = getMaxRegionHeight();
        Location pos1 = getPos1(player);
        Location pos2 = getPos2(player);

        player.sendMessage("§6=== Ваши лимиты регионов ===");

        if (areSizeChecksEnabled()) {
            player.sendMessage("§7Проверка размеров: §aВКЛЮЧЕНА");
            player.sendMessage("§aМинимальный размер: §e" + minSize + " §aблоков");

            // ИСПРАВЛЕНИЕ: отображение безлимитной высоты
            if (maxHeight == Integer.MAX_VALUE) {
                player.sendMessage("§aМаксимальная высота: §6безлимитно");
            } else {
                player.sendMessage("§aМаксимальная высота: §e" + maxHeight + " §aблоков");
            }
        } else {
            player.sendMessage("§7Проверка размеров: §cОТКЛЮЧЕНА");
        }

        if (maxSize == -1) {
            player.sendMessage("§aЛимит размера: §6безлимитно");
        } else {
            player.sendMessage("§aЛимит размера: §e" + maxSize + " §aблоков");
        }

        if (maxRegions == -1) {
            player.sendMessage("§aЛимит регионов: §6безлимитно");
        } else {
            player.sendMessage("§aЛимит регионов: §e" + maxRegions + " §aрегионов");
            player.sendMessage("§7Создано регионов: §e" + currentRegions + " §7из §e" + maxRegions);
            if (currentRegions >= maxRegions) {
                player.sendMessage("§c⚠ Вы достигли лимита регионов!");
            }
        }

        if (pos1 != null && pos2 != null) {
            int volume = calculateVolume(pos1, pos2);
            player.sendMessage("§aТекущий выделенный размер: §e" + volume + " §aблоков");

            if (maxSize != -1 && maxSize > 0) {
                String percentage = String.format("%.1f", (volume * 100.0 / maxSize));
                player.sendMessage("§aИспользовано лимита размера: §6" + percentage + "%");
                if (volume > maxSize) {
                    player.sendMessage("§c⚠ Внимание: текущий размер превышает ваш лимит!");
                }
            }
        } else {
            player.sendMessage("§7Установите позиции с помощью §f/rg pos1 §7и §f/rg pos2");
            player.sendMessage("§7для проверки размера выделенной области");
        }

        player.sendMessage(getMaxRegionsMessage(player));
    }

    // =============================================
    // ОБРАБОТКА СОБЫТИЙ (ЗАЩИТА ОТ ВЗРЫВОВ)
    // =============================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;

        Location center = event.getLocation();
        List<org.bukkit.block.Block> blocksToRemove = new ArrayList<>(event.blockList());

        for (org.bukkit.block.Block block : blocksToRemove) {
            Location blockLoc = block.getLocation();
            Region region = getRegionAtLocation(blockLoc);

            if (region != null) {
                boolean canExplode = getFlagBooleanValue(region, "explosion", true);
                if (!canExplode) {
                    event.blockList().remove(block);
                    continue;
                }
            }

            Region blockRegion = getRegionAtLocation(blockLoc);
            Region explosionRegion = getRegionAtLocation(center);

            if (blockRegion != null && explosionRegion == null) {
                boolean canExplode = getFlagBooleanValue(blockRegion, "explosion", true);
                if (!canExplode) {
                    event.blockList().remove(block);
                }
            }
        }

        if (plugin.isDebugEnabled() && event.blockList().size() < blocksToRemove.size()) {
            plugin.getLogger().info("Защищено блоков от взрыва: " + (blocksToRemove.size() - event.blockList().size()));
        }
    }

    // =============================================
    // ДОПОЛНИТЕЛЬНЫЕ МЕНЕДЖЕРЫ ДЛЯ СОВМЕСТИМОСТИ
    // =============================================

    private PlayerTimeManager playerTimeManager;
    private LanguageManager languageManager;
    private SettingsManager settingsManager;

    public PlayerTimeManager getPlayerTimeManager() {
        if (playerTimeManager == null) {
            playerTimeManager = new PlayerTimeManager(plugin);
        }
        return playerTimeManager;
    }

    public LanguageManager getLanguageManager() {
        if (languageManager == null) {
            languageManager = new LanguageManager(plugin);
        }
        return languageManager;
    }

    public SettingsManager getSettingsManager() {
        if (settingsManager == null) {
            settingsManager = new SettingsManager(plugin);
        }
        return settingsManager;
    }

    private boolean hasAdminBypass(Player player) {
        return player != null && adminManager != null && adminManager.isAdmin(player);
    }
}