package org.KillerYT.regionMC.managers;

import org.KillerYT.regionMC.RegionMC;
import org.killeryt.killerCoreAPI.utils.vault.VaultUtils;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RentManager {

    private final RegionMC plugin;
    private final RegionManager regionManager;
    private final File rentsFile;
    private final Map<String, PriceData> prices = new ConcurrentHashMap<>();
    private final Map<String, RentData> rents = new ConcurrentHashMap<>();
    private VaultUtils vaultUtils; // теперь не final – ленивая инициализация

    // Внутренние классы данных (record)
    public record PriceData(double price, String unit) {
        public PriceData(double price, String unit) {
            this.price = price;
            this.unit = unit.toLowerCase();
        }
    }

    public record RentData(UUID renter, long endTime, double pricePaid) {}

    // Конструктор
    public RentManager(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.rentsFile = new File(plugin.getDataFolder(), "rents.yml");
        // НЕ СОЗДАЁМ VaultUtils ЗДЕСЬ – отложим до первого использования
        loadData();
        startExpiryChecker();
        plugin.getLogger().info("RentManager initialized (economy will be checked on first use)");
    }

    // Ленивая инициализация VaultUtils
    private VaultUtils getVaultUtils() {
        if (vaultUtils == null) {
            vaultUtils = new VaultUtils(plugin);
            if (!vaultUtils.isEnabled()) {
                plugin.getLogger().warning("Экономика всё ещё недоступна! Аренда не будет работать.");
            } else {
                plugin.getLogger().info("Экономика подключена успешно.");
            }
        }
        return vaultUtils;
    }

    // ===================== ЗАГРУЗКА / СОХРАНЕНИЕ =====================

    private void loadData() {
        if (!rentsFile.exists()) {
            plugin.getLogger().info("No rents.yml found, creating default.");
            saveData();
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(rentsFile);

        // Загрузка цен
        if (config.contains("prices")) {
            var pricesSection = config.getConfigurationSection("prices");
            if (pricesSection != null) {
                for (String region : pricesSection.getKeys(false)) {
                    double price = pricesSection.getDouble(region + ".price");
                    String unit = pricesSection.getString(region + ".unit", "d");
                    prices.put(region, new PriceData(price, unit));
                }
            }
        }

        // Загрузка аренд
        if (config.contains("rents")) {
            var rentsSection = config.getConfigurationSection("rents");
            if (rentsSection != null) {
                for (String region : rentsSection.getKeys(false)) {
                    String renterStr = rentsSection.getString(region + ".renter");
                    if (renterStr == null) continue;
                    long endTime = rentsSection.getLong(region + ".endTime");
                    double pricePaid = rentsSection.getDouble(region + ".pricePaid", 0);
                    try {
                        UUID renter = UUID.fromString(renterStr);
                        rents.put(region, new RentData(renter, endTime, pricePaid));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in rent data for region " + region);
                    }
                }
            }
        }
        plugin.getLogger().info("Loaded " + prices.size() + " price entries and " + rents.size() + " active rents.");
    }

    public void saveData() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, PriceData> entry : prices.entrySet()) {
            config.set("prices." + entry.getKey() + ".price", entry.getValue().price());
            config.set("prices." + entry.getKey() + ".unit", entry.getValue().unit());
        }
        for (Map.Entry<String, RentData> entry : rents.entrySet()) {
            config.set("rents." + entry.getKey() + ".renter", entry.getValue().renter().toString());
            config.set("rents." + entry.getKey() + ".endTime", entry.getValue().endTime());
            config.set("rents." + entry.getKey() + ".pricePaid", entry.getValue().pricePaid());
        }
        try {
            config.save(rentsFile);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save rents.yml: " + e.getMessage());
        }
    }

    private void startExpiryChecker() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            boolean changed = false;
            Iterator<Map.Entry<String, RentData>> it = rents.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, RentData> entry = it.next();
                if (entry.getValue().endTime() <= now) {
                    String regionName = entry.getKey();
                    RentData data = entry.getValue();
                    Region region = regionManager.getRegion(regionName);
                    if (region != null) {
                        OfflinePlayer owner = Bukkit.getOfflinePlayer(region.getOwner());
                        if (owner.isOnline()) {
                            owner.getPlayer().sendMessage("§eАренда региона " + regionName + " истекла.");
                        }
                        Player renter = Bukkit.getPlayer(data.renter());
                        if (renter != null && renter.isOnline()) {
                            renter.sendMessage("§cВаша аренда региона " + regionName + " истекла.");
                        }
                    }
                    it.remove();
                    changed = true;
                }
            }
            if (changed) {
                Bukkit.getScheduler().runTask(plugin, this::saveData);
            }
        }, 0L, 20L * 60L);
    }

    // ===================== ПУБЛИЧНЫЕ МЕТОДЫ =====================

    public boolean setPrice(Player setter, String regionName, double price, String unit) {
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            setter.sendMessage("§cРегион не найден.");
            return false;
        }
        if (!regionManager.canModifyRegion(setter, regionName)) {
            setter.sendMessage("§cУ вас нет прав для установки цены на этот регион.");
            return false;
        }
        if (price <= 0) {
            setter.sendMessage("§cЦена должна быть положительным числом.");
            return false;
        }
        if (!isValidUnit(unit)) {
            setter.sendMessage("§cНеверная единица времени. Допустимые: s, m, h, d, mo, y");
            return false;
        }
        prices.put(regionName, new PriceData(price, unit));
        saveData();
        setter.sendMessage("§aЦена аренды для региона " + regionName + " установлена: " + price + " за " + unit);
        return true;
    }

    public boolean rentRegion(Player renter, String regionName, int amount, String unit) {
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            renter.sendMessage("§cРегион не найден.");
            return false;
        }
        if (region.isOwner(renter.getUniqueId())) {
            renter.sendMessage("§cВы владелец региона, не можете арендовать свой регион.");
            return false;
        }
        if (rents.containsKey(regionName)) {
            renter.sendMessage("§cРегион уже арендован другим игроком.");
            return false;
        }
        PriceData priceData = prices.get(regionName);
        if (priceData == null) {
            renter.sendMessage("§cЦена для аренды не установлена владельцем.");
            return false;
        }
        if (!unit.equalsIgnoreCase(priceData.unit())) {
            renter.sendMessage("§cЕдиница времени должна быть '" + priceData.unit() + "' (как установлено владельцем).");
            return false;
        }
        long unitMillis = getUnitMillis(unit);
        if (unitMillis == 0) {
            renter.sendMessage("§cНеверная единица времени.");
            return false;
        }
        long duration = unitMillis * amount;
        long endTime = System.currentTimeMillis() + duration;
        double totalCost = priceData.price() * amount;

        VaultUtils vu = getVaultUtils();  // ленивая инициализация
        if (!vu.isEnabled()) {
            renter.sendMessage("§cЭкономика не настроена.");
            return false;
        }
        if (!vu.hasBalance(renter, totalCost)) {
            renter.sendMessage("§cНедостаточно денег. Нужно: " + totalCost);
            return false;
        }
        if (!vu.withdraw(renter, totalCost)) {
            renter.sendMessage("§cОшибка при списании денег.");
            return false;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(region.getOwner());
        vu.deposit(owner, totalCost);

        rents.put(regionName, new RentData(renter.getUniqueId(), endTime, totalCost));
        saveData();

        renter.sendMessage("§aВы арендовали регион " + regionName + " на " + amount + " " + unit + " за " + totalCost);
        if (owner.isOnline()) {
            owner.getPlayer().sendMessage("§eРегион " + regionName + " арендован игроком " + renter.getName() +
                    " на " + amount + " " + unit + ". Вы получили " + totalCost);
        }
        return true;
    }

    public boolean cancelRent(Player executor, String regionName) {
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            executor.sendMessage("§cРегион не найден.");
            return false;
        }
        if (!rents.containsKey(regionName)) {
            executor.sendMessage("§cРегион не арендован.");
            return false;
        }
        RentData data = rents.get(regionName);
        boolean isOwner = region.isOwner(executor.getUniqueId());
        boolean isRenter = data.renter().equals(executor.getUniqueId());
        boolean isAdmin = executor.hasPermission("regionmc.admin") || plugin.getAdminManager().isAdmin(executor);
        if (!isOwner && !isRenter && !isAdmin) {
            executor.sendMessage("§cУ вас нет прав для отмены этой аренды.");
            return false;
        }

        long now = System.currentTimeMillis();
        long remaining = data.endTime() - now;
        if (remaining > 0 && isRenter) {
            double totalPaid = data.pricePaid();
            long totalDuration = data.endTime() - (now - remaining);
            double refund = totalPaid * ((double) remaining / totalDuration);
            VaultUtils vu = getVaultUtils();
            if (vu.isEnabled() && refund > 0) {
                vu.deposit(executor, refund);
                executor.sendMessage("§aВам возвращено " + refund + " за неиспользованное время.");
            }
        }

        rents.remove(regionName);
        saveData();
        executor.sendMessage("§aАренда региона " + regionName + " отменена.");
        OfflinePlayer other = isRenter ? Bukkit.getOfflinePlayer(region.getOwner()) : Bukkit.getOfflinePlayer(data.renter());
        if (other.isOnline()) {
            other.getPlayer().sendMessage("§eАренда региона " + regionName + " была отменена игроком " + executor.getName());
        }
        return true;
    }

    public boolean extendRent(Player renter, String regionName, int amount, String unit) {
        if (!rents.containsKey(regionName)) {
            renter.sendMessage("§cРегион не арендован вами.");
            return false;
        }
        RentData data = rents.get(regionName);
        if (!data.renter().equals(renter.getUniqueId())) {
            renter.sendMessage("§cВы не арендатор этого региона.");
            return false;
        }
        PriceData priceData = prices.get(regionName);
        if (priceData == null) {
            renter.sendMessage("§cЦена больше не установлена.");
            return false;
        }
        if (!unit.equalsIgnoreCase(priceData.unit())) {
            renter.sendMessage("§cЕдиница времени должна быть '" + priceData.unit() + "'.");
            return false;
        }
        long unitMillis = getUnitMillis(unit);
        if (unitMillis == 0) {
            renter.sendMessage("§cНеверная единица времени.");
            return false;
        }
        long additional = unitMillis * amount;
        double cost = priceData.price() * amount;

        VaultUtils vu = getVaultUtils();
        if (!vu.isEnabled() || !vu.hasBalance(renter, cost)) {
            renter.sendMessage("§cНедостаточно денег. Нужно: " + cost);
            return false;
        }
        if (!vu.withdraw(renter, cost)) {
            renter.sendMessage("§cОшибка при списании денег.");
            return false;
        }
        Region region = regionManager.getRegion(regionName);
        if (region != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(region.getOwner());
            vu.deposit(owner, cost);
        }
        long newEndTime = data.endTime() + additional;
        rents.put(regionName, new RentData(data.renter(), newEndTime, data.pricePaid() + cost));
        saveData();
        renter.sendMessage("§aАренда региона " + regionName + " продлена на " + amount + " " + unit + ".");
        return true;
    }

    public void getInfo(CommandSender sender, String regionName) {
        Region region = regionManager.getRegion(regionName);
        if (region == null) {
            sender.sendMessage("§cРегион не найден.");
            return;
        }
        sender.sendMessage("§6=== Аренда региона " + regionName + " ===");
        PriceData priceData = prices.get(regionName);
        if (priceData == null) {
            sender.sendMessage("§7Цена не установлена.");
        } else {
            sender.sendMessage("§7Цена: §e" + priceData.price() + " §7за " + priceData.unit());
        }
        RentData rentData = rents.get(regionName);
        if (rentData == null) {
            sender.sendMessage("§7Статус: §aНе арендован");
        } else {
            String renterName = Bukkit.getOfflinePlayer(rentData.renter()).getName();
            if (renterName == null) renterName = "Неизвестно";
            sender.sendMessage("§7Арендатор: §e" + renterName);
            long remaining = rentData.endTime() - System.currentTimeMillis();
            if (remaining > 0) {
                sender.sendMessage("§7Осталось: §e" + formatTime(remaining));
            } else {
                sender.sendMessage("§cАренда истекла (должна была быть завершена автоматически)");
            }
            sender.sendMessage("§7Оплачено: §e" + rentData.pricePaid());
        }
    }

    @SuppressWarnings("deprecation") // getLines() устарел, но пока работает
    public void handleSignClick(Player player, org.bukkit.block.Sign sign) {
        String[] lines = sign.getLines();
        if (!lines[0].equalsIgnoreCase("[Rent]")) return;
        String regionName = lines[1].trim();
        if (regionName.isEmpty()) {
            player.sendMessage("§cНекорректная табличка: не указан регион.");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(lines[2].trim());
        } catch (NumberFormatException e) {
            player.sendMessage("§cНекорректная цена на табличке.");
            return;
        }
        String unit = lines[3].trim().toLowerCase();
        if (!isValidUnit(unit)) {
            player.sendMessage("§cНекорректная единица времени на табличке.");
            return;
        }
        PriceData pd = prices.get(regionName);
        if (pd == null) {
            player.sendMessage("§cЦена для этого региона не установлена.");
            return;
        }
        if (Math.abs(pd.price() - price) > 0.001 || !pd.unit().equals(unit)) {
            player.sendMessage("§cЦена на табличке не соответствует текущей цене региона.");
            return;
        }
        rentRegion(player, regionName, 1, unit);
    }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====================

    private boolean isValidUnit(String unit) {
        return unit.matches("s|m|h|d|mo|y");
    }

    private long getUnitMillis(String unit) {
        return switch (unit.toLowerCase()) {
            case "s" -> 1000L;
            case "m" -> 60_000L;
            case "h" -> 3_600_000L;
            case "d" -> 86_400_000L;
            case "mo" -> 2_592_000_000L;
            case "y" -> 31_536_000_000L;
            default -> 0;
        };
    }

    private String formatTime(long millis) {
        if (millis < 0) return "0";
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) return days + "д " + (hours % 24) + "ч";
        if (hours > 0) return hours + "ч " + (minutes % 60) + "м";
        if (minutes > 0) return minutes + "м " + (seconds % 60) + "с";
        return seconds + "с";
    }

    public void reload() {
        prices.clear();
        rents.clear();
        loadData();
    }
}