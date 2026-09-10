package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.region.ColorUtil;
import org.KillerYT.regionMC.utils.PermissionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RegionWandCommand {

    private final RegionMC plugin;
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();
    private final NamespacedKey WAND_KEY;

    public RegionWandCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.WAND_KEY = new NamespacedKey(plugin, "region_wand");
    }

    public boolean execute(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.colorize("&cЭта команда доступна только игрокам!"));
            return true;
        }

        // Проверка прав
        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.wand") &&
                !PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.admin")) {
            // old: if (!player.hasPermission("regionmc.command.wand") && !player.hasPermission("regionmc.admin")) {
            player.sendMessage(ColorUtil.colorize("&cУ вас нет прав для получения палочки выделения!"));
            return true;
        }

        FileConfiguration config = plugin.getConfig();

        if (!config.getBoolean("selection-tools.selection-wand.enabled", true)) {
            player.sendMessage(ColorUtil.colorize("&cПалочка выделения отключена в настройках!"));
            return true;
        }

        ItemStack wand = createWandItem(config);

        if (player.getInventory().firstEmpty() == -1) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getType() == Material.AIR) {
                player.getInventory().setItemInMainHand(wand);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), wand);
                player.sendMessage(ColorUtil.colorize("&eПалочка выделения упала на землю (инвентарь полный)!"));
                return true;
            }
        } else {
            player.getInventory().addItem(wand);
        }

        player.sendMessage(ColorUtil.colorize("&aПалочка выделения получена!"));
        return true;
    }

    private ItemStack createWandItem(FileConfiguration config) {
        String materialName = config.getString("selection-tools.selection-wand.material", "WOODEN_AXE");
        Material material = getMaterialByName(materialName);

        String name = config.getString("selection-tools.selection-wand.name", "&6Топор для выделения");
        String coloredName = ColorUtil.colorize(name);

        List<String> loreList = config.getStringList("selection-tools.selection-wand.lore");
        if (loreList.isEmpty()) {
            loreList = new ArrayList<>();
            loreList.add("&7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            loreList.add("&6➤ &eЛКМ &7- Установить &fpos1");
            loreList.add("&6➤ &eПКМ &7- Установить &fpos2");
            loreList.add("&6➤ &eShift+ПКМ &7- Информация");
            loreList.add("&7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }

        ItemStack wand = new ItemStack(material, 1);
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.displayName(legacySerializer.deserialize(coloredName));

            List<Component> translatedLore = new ArrayList<>();
            for (String loreLine : loreList) {
                String coloredLine = ColorUtil.colorize(loreLine);
                translatedLore.add(legacySerializer.deserialize(coloredLine));
            }
            meta.lore(translatedLore);

            meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.BYTE, (byte) 1);

            try {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } catch (Exception e) {
                // Игнорируем
            }

            int durability = config.getInt("selection-tools.selection-wand.durability", -1);
            if (durability >= 0) {
                meta.setUnbreakable(true);
            }

            wand.setItemMeta(meta);
        }

        return wand;
    }

    private Material getMaterialByName(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return Material.WOODEN_AXE;
        }

        Material material = Material.getMaterial(materialName.toUpperCase());
        if (material != null) {
            return material;
        }

        material = tryAlternativeNames(materialName.toUpperCase());
        if (material != null) {
            return material;
        }

        plugin.getLogger().warning("Material '" + materialName + "' not found. Using WOODEN_AXE as default.");
        return Material.WOODEN_AXE;
    }

    private Material tryAlternativeNames(String materialName) {
        switch (materialName) {
            case "WOODEN_AXE":
            case "WOOD_AXE":
                return Material.getMaterial("WOODEN_AXE");
            case "STONE_AXE":
                return Material.getMaterial("STONE_AXE");
            case "IRON_AXE":
                return Material.getMaterial("IRON_AXE");
            case "GOLDEN_AXE":
            case "GOLD_AXE":
                return Material.getMaterial("GOLDEN_AXE");
            case "DIAMOND_AXE":
                return Material.getMaterial("DIAMOND_AXE");
            case "NETHERITE_AXE":
                return Material.getMaterial("NETHERITE_AXE");
            case "STICK":
                return Material.getMaterial("STICK");
            case "BLAZE_ROD":
                return Material.getMaterial("BLAZE_ROD");
            case "BONE":
                return Material.getMaterial("BONE");
            default:
                return null;
        }
    }

    public List<String> onTabComplete() {
        return new ArrayList<>();
    }

    public boolean isWandItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            try {
                Byte isWand = meta.getPersistentDataContainer().get(WAND_KEY, PersistentDataType.BYTE);
                if (isWand != null && isWand == 1) {
                    return true;
                }
            } catch (Exception e) {
                // Игнорируем
            }
        }

        String configMaterial = plugin.getConfig().getString("selection-tools.selection-wand.material", "WOODEN_AXE");
        return item.getType().name().equalsIgnoreCase(configMaterial);
    }

    public ItemStack createAutoDetectionWand() {
        FileConfiguration config = plugin.getConfig();
        String materialName = config.getString("selection-tools.selection-wand.material", "WOODEN_AXE");
        Material material = getMaterialByName(materialName);

        ItemStack wand = new ItemStack(material, 1);
        ItemMeta meta = wand.getItemMeta();

        if (meta != null) {
            meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.BYTE, (byte) 1);
            try {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } catch (Exception e) {
                // Игнорируем
            }
            wand.setItemMeta(meta);
        }

        return wand;
    }
}