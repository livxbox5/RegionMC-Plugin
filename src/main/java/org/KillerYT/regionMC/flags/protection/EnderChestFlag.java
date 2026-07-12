package org.KillerYT.regionMC.flags.protection;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class EnderChestFlag extends ListenerFlag<Boolean> {
    public EnderChestFlag(RegionMC plugin) { super(plugin, "enderchest", false); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Использование эндер-сундука"; }

    /**
     *
     *  Логика enderchest (клик по блоку)
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.ENDER_CHEST) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        String regionName = regionManager.findRegionAtLocation(event.getClickedBlock().getLocation());

        if (regionName == null) return; // Нет региона — разрешаем
        if (shouldBypassRegionProtection(p, regionName)) return; // Байпас

        org.KillerYT.regionMC.region.Region region = regionManager.getRegion(regionName);
        if (region == null) return;

        // Владельцы и участники всегда могут открывать
        if (region.isOwner(p.getUniqueId()) || region.isMember(p.getUniqueId())) return;

        // Проверяем значение флага
        Boolean flagValue = region.getFlagBoolean("enderchest");

        // Если флаг НЕ установлен — разрешаем (по умолчанию)
        if (flagValue == null) return;

        // Если флаг false (deny) — запрещаем
        if (!flagValue) {
            event.setCancelled(true);
            sendMessage(p, "protection.enderchest", "§cВы не можете использовать эндерсундуки здесь!");
        }
        // Если флаг true (allow) — разрешаем (ничего не делаем)
    }

    /**
     *
     *  Логика enderchest (открытие инвентаря)
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof org.bukkit.block.EnderChest)) return;
        if (!(event.getPlayer() instanceof Player p)) return;

        String regionName = regionManager.findRegionAtLocation(p.getLocation());
        if (regionName == null) return;
        if (shouldBypassRegionProtection(p, regionName)) return;

        org.KillerYT.regionMC.region.Region region = regionManager.getRegion(regionName);
        if (region == null) return;

        if (region.isOwner(p.getUniqueId()) || region.isMember(p.getUniqueId())) return;

        Boolean flagValue = region.getFlagBoolean("enderchest");
        if (flagValue == null) return;

        if (!flagValue) {
            event.setCancelled(true);
            sendMessage(p, "protection.enderchest", "§cВы не можете использовать эндерсундуки здесь!");
        }
    }
}