package org.KillerYT.regionMC.Placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class RegionMCPlaceholderExpansion extends PlaceholderExpansion {
    private final RegionMC plugin;
    private final RegionManager regionManager;
    private final PluginDescriptionFile description;

    // Список глобальных регионов, которые нужно исключить из подсчета
    private static final Set<String> GLOBAL_REGIONS = Set.of(
            "__Global__",
            "__Global__world_nether",
            "__Global__world_the_end"
    );

    public RegionMCPlaceholderExpansion(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.description = plugin.getDescription();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "regionmc";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", description.getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return description.getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * Упрощенный метод регистрации с проверкой PlaceholderAPI
     */
    public boolean registerExpansion() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            plugin.getLogger().info("PlaceholderAPI не найден");
            return false;
        }

        try {
            // Используем метод register() из родительского класса
            boolean result = this.register();

            if (result) {
                plugin.getLogger().info("✓ PlaceholderAPI расширение зарегистрировано");
            } else {
                plugin.getLogger().warning("✗ Не удалось зарегистрировать PlaceholderAPI расширение");
            }

            return result;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка регистрации PlaceholderAPI", e);
            return false;
        }
    }

    /**
     * Упрощенный метод отмены регистрации
     */
    public void unregisterExpansion() {
        try {
            // Используем метод unregister() из родительского класса
            this.unregister();
            plugin.getLogger().info("✓ PlaceholderAPI расширение отменено");
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при отмене регистрации PlaceholderAPI: " + e.getMessage());
        }
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        String lowerIdentifier = identifier.toLowerCase();

        try {
            switch (lowerIdentifier) {
                // Основные плейсхолдеры
                case "version":
                    return description.getVersion();
                case "test":
                    return "Работает! Версия: " + description.getVersion();

                // Статистика
                case "total_regions":
                case "region_count":
                    return String.valueOf(getNonGlobalRegionsCount());
                case "region_limit":
                case "player_max_regions":
                    return String.valueOf(plugin.getConfig().getInt("regions.player-limit", 5));
                case "player_regions_count":
                case "player_region_count":
                case "player_total_regions":
                    return String.valueOf(getPlayerRegionsCount(player.getUniqueId()));
                case "player_regions_remaining": {
                    int limit = plugin.getConfig().getInt("regions.player-limit", 5);
                    int current = getPlayerRegionsCount(player.getUniqueId());
                    return String.valueOf(Math.max(0, limit - current));
                }
                case "has_region":
                    return getPlayerRegionsCount(player.getUniqueId()) > 0 ? "Да" : "Нет";

                // Текущее положение игрока
                case "current_name":
                case "current_region_name": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        return region.getName();
                    }
                    return "";
                }
                case "in_region": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    return (region != null && !region.getName().equalsIgnoreCase("__Global__")) ? "Да" : "Нет";
                }
                case "current_world":
                    return player.getWorld().getName();

                // Разрешения игрока (текущий регион)
                case "can_build":
                case "player_can_build": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        boolean canBuild = regionManager.canBuildInRegion(player.getUniqueId(), region.getName());
                        return canBuild ? "Да" : "Нет";
                    }
                    return "Да";
                }
                case "can_break": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        boolean canBreak = regionManager.canBreakInRegion(player.getUniqueId(), region.getName());
                        return canBreak ? "Да" : "Нет";
                    }
                    return "Да";
                }
                case "can_interact": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        boolean canInteract = regionManager.canInteractInRegion(player.getUniqueId(), region.getName());
                        return canInteract ? "Да" : "Нет";
                    }
                    return "Да";
                }

                // Информация о текущем регионе
                case "current_owner":
                case "region_owner": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        return region.getOwnersString();
                    }
                    return "";
                }
                case "current_members":
                case "region_member": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        return region.getMembersString();
                    }
                    return "";
                }
                case "current_priority": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        return String.valueOf(region.getPriority());
                    }
                    return "0";
                }
                case "current_size": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        return String.valueOf(regionManager.calculateVolume(region.getPos1(), region.getPos2()));
                    }
                    return "0";
                }

                // Права игрока в текущем регионе
                case "player_is_owner": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        return region.isOwner(player.getUniqueId()) ? "Да" : "Нет";
                    }
                    return "Нет";
                }
                case "player_is_member": {
                    Region region = regionManager.getRegionAtLocation(player.getLocation());
                    if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                        return region.isMember(player.getUniqueId()) ? "Да" : "Нет";
                    }
                    return "Нет";
                }

                default:
                    return handleDynamicPlaceholders(player, identifier, lowerIdentifier);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка обработки плейсхолдера " + identifier, e);
            return null;
        }
    }

    private String handleDynamicPlaceholders(Player player, String identifier, String lowerIdentifier) {
        UUID playerId = player.getUniqueId();

        // Формат: %regionmc_current_owner_{regionname}%
        if (lowerIdentifier.startsWith("current_owner_")) {
            String regionName = identifier.substring("current_owner_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                return region.getOwnersString();
            }
            return "";
        }

        // Формат: %regionmc_current_members_{regionname}%
        if (lowerIdentifier.startsWith("current_members_")) {
            String regionName = identifier.substring("current_members_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                return region.getMembersString();
            }
            return "";
        }

        // Формат: %regionmc_current_priority_{regionname}%
        if (lowerIdentifier.startsWith("current_priority_")) {
            String regionName = identifier.substring("current_priority_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                return String.valueOf(region.getPriority());
            }
            return "0";
        }

        // Формат: %regionmc_current_size_{regionname}%
        if (lowerIdentifier.startsWith("current_size_")) {
            String regionName = identifier.substring("current_size_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                return String.valueOf(regionManager.calculateVolume(region.getPos1(), region.getPos2()));
            }
            return "0";
        }

        // Формат: %regionmc_in_region_{regionname}%
        if (lowerIdentifier.startsWith("in_region_")) {
            String regionName = identifier.substring("in_region_".length());
            Region region = regionManager.getRegionAtLocation(player.getLocation());
            if (region != null && region.getName().equals(regionName)) {
                return "Да";
            }
            return "Нет";
        }

        // Формат: %regionmc_player_is_owner_{regionname}%
        if (lowerIdentifier.startsWith("player_is_owner_")) {
            String regionName = identifier.substring("player_is_owner_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                return region.isOwner(playerId) ? "Да" : "Нет";
            }
            return "Нет";
        }

        // Формат: %regionmc_player_is_member_{regionname}%
        if (lowerIdentifier.startsWith("player_is_member_")) {
            String regionName = identifier.substring("player_is_member_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                return region.isMember(playerId) ? "Да" : "Нет";
            }
            return "Нет";
        }

        // Формат: %regionmc_can_build_{regionname}%
        if (lowerIdentifier.startsWith("can_build_")) {
            String regionName = identifier.substring("can_build_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                boolean canBuild = regionManager.canBuildInRegion(playerId, region.getName());
                return canBuild ? "Да" : "Нет";
            }
            return "Да";
        }

        // Формат: %regionmc_can_break_{regionname}%
        if (lowerIdentifier.startsWith("can_break_")) {
            String regionName = identifier.substring("can_break_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                boolean canBreak = regionManager.canBreakInRegion(playerId, region.getName());
                return canBreak ? "Да" : "Нет";
            }
            return "Да";
        }

        // Формат: %regionmc_can_interact_{regionname}%
        if (lowerIdentifier.startsWith("can_interact_")) {
            String regionName = identifier.substring("can_interact_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                boolean canInteract = regionManager.canInteractInRegion(playerId, region.getName());
                return canInteract ? "Да" : "Нет";
            }
            return "Да";
        }

        // Старые форматы
        if (lowerIdentifier.startsWith("region_owner_")) {
            String regionName = identifier.substring("region_owner_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                return region.getOwnersString();
            }
            return "";
        }

        if (lowerIdentifier.startsWith("region_member_")) {
            String regionName = identifier.substring("region_member_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                return region.getMembersString();
            }
            return "";
        }

        if (lowerIdentifier.startsWith("info_")) {
            String regionName = identifier.substring("info_".length());
            Region region = regionManager.getRegion(regionName);
            if (region != null && !region.getName().equalsIgnoreCase("__Global__")) {
                return formatRegionInfo(region);
            }
            return "";
        }

        return null;
    }

    private String formatRegionInfo(Region region) {
        return String.format("Имя: %s, Приоритет: %d, Владелец: %s",
                region.getName(),
                region.getPriority(),
                region.getOwnersString());
    }

    /**
     * Получает количество не-Global регионов
     */
    private int getNonGlobalRegionsCount() {
        int count = 0;
        for (Region region : regionManager.getAllRegions()) {
            if (!GLOBAL_REGIONS.contains(region.getName())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Вспомогательный метод для получения количества регионов игрока
     * Используется вместо deprecated метода
     */
    @SuppressWarnings("deprecation")
    private int getPlayerRegionsCount(UUID playerId) {
        try {
            // Пробуем использовать deprecated метод, если нет альтернативы
            return regionManager.getPlayerRegionsCount(playerId);
        } catch (NoSuchMethodError e) {
            // Если метод недоступен, возвращаем 0
            plugin.getLogger().warning("Метод getPlayerRegionsCount недоступен");
            return 0;
        }
    }
}