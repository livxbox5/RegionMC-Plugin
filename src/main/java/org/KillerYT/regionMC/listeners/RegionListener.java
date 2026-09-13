package org.KillerYT.regionMC.listeners;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class RegionListener implements Listener {

    private final RegionManager regionManager;
    private final RegionMC plugin;

    public RegionListener(RegionManager regionManager, RegionMC plugin) {
        this.regionManager = regionManager;
        this.plugin = plugin;
    }

    // =====================================================
    // ЛОМАНИЕ БЛОКА — регион по блоку
    // =====================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Region region = regionManager.getRegionAtLocation(event.getBlock().getLocation());
        if (region == null) return;

        if (regionManager.canBypass(player, region.getName())) return;
        if (region.isOwner(player.getUniqueId()) || region.isMember(player.getUniqueId())) return;

        if (!regionManager.getFlagBooleanValue(region, "break", true)) {
            event.setCancelled(true);
            sendMessage(player, "protection.break", "§cВы не можете разрушать блоки в этом регионе!");
        }
    }

    // =====================================================
    // УСТАНОВКА БЛОКА — регион по блоку КУДА ставится
    // =====================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Region region = regionManager.getRegionAtLocation(event.getBlockPlaced().getLocation());
        if (region == null) return;

        if (regionManager.canBypass(player, region.getName())) return;
        if (region.isOwner(player.getUniqueId()) || region.isMember(player.getUniqueId())) return;

        if (!regionManager.getFlagBooleanValue(region, "build", true)) {
            event.setCancelled(true);
            sendMessage(player, "protection.build", "§cВы не можете строить в этом регионе!");
        }
    }

    // =====================================================
    // ВЗАИМОДЕЙСТВИЕ
    // =====================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            return;
        }

        // ---------- ПРАВЫЙ КЛИК ПО БЛОКУ ----------
        if (action == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) return;

            Region region = regionManager.getRegionAtLocation(clicked.getLocation());
            if (region == null) return;

            if (regionManager.canBypass(player, region.getName())) return;
            if (region.isOwner(player.getUniqueId()) || region.isMember(player.getUniqueId())) return;

            Material type = clicked.getType();

            if ((type == Material.CHEST || type == Material.TRAPPED_CHEST)
                    && !regionManager.getFlagBooleanValue(region, "chest-access", true)) {
                event.setCancelled(true);
                sendMessage(player, "protection.chest", "§cСундуки в этом регионе закрыты!");
                return;
            }

            if (type == Material.ENDER_CHEST
                    && !regionManager.getFlagBooleanValue(region, "enderchest", true)) {
                event.setCancelled(true);
                sendMessage(player, "protection.enderchest", "§cЭндер-сундук запрещён в этом регионе!");
                return;
            }

            if (!regionManager.getFlagBooleanValue(region, "interact", true)) {
                event.setCancelled(true);
                sendMessage(player, "protection.interact", "§cВзаимодействие запрещено в этом регионе!");
            }
            return;
        }

        // ---------- ПРАВЫЙ КЛИК В ВОЗДУХ → use-items ----------
        if (action == Action.RIGHT_CLICK_AIR) {
            if (event.getItem() == null) return;

            Region region = regionManager.getRegionAtLocation(player.getLocation());
            if (region == null) return;

            if (regionManager.canBypass(player, region.getName())) return;
            if (region.isOwner(player.getUniqueId()) || region.isMember(player.getUniqueId())) return;

            if (!regionManager.getFlagBooleanValue(region, "use-items", true)) {
                event.setCancelled(true);
                sendMessage(player, "protection.use", "§cВы не можете использовать предметы здесь!");
            }
        }
    }

    // =====================================================
    // PvP — регион по жертве
    // =====================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        Region region = regionManager.getRegionAtLocation(event.getEntity().getLocation());
        if (region == null) return;

        if (regionManager.canBypass(player, region.getName())) return;

        if (!regionManager.getFlagBooleanValue(region, "pvp", true)
                && !region.isOwner(player.getUniqueId())
                && !region.isMember(player.getUniqueId())) {
            event.setCancelled(true);
            sendMessage(player, "protection.pvp", "§cPvP отключён в этом регионе!");
        }
    }

    // =====================================================
    // entry / exit — по позиции игрока
    // =====================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) return;

        Player player = event.getPlayer();
        Region fromRegion = regionManager.getRegionAtLocation(from);
        Region toRegion = regionManager.getRegionAtLocation(to);

        // --- Вход ---
        if (toRegion != null && toRegion != fromRegion) {
            boolean bypassed = regionManager.canBypass(player, toRegion.getName());
            boolean isMember = toRegion.isOwner(player.getUniqueId())
                    || toRegion.isMember(player.getUniqueId());

            if (!bypassed && !isMember
                    && !regionManager.getFlagBooleanValue(toRegion, "entry", true)) {
                event.setCancelled(true);
                return;
            }
        }

        // --- Выход ---
        if (fromRegion != null && fromRegion != toRegion) {
            boolean bypassed = regionManager.canBypass(player, fromRegion.getName());
            boolean isMember = fromRegion.isOwner(player.getUniqueId())
                    || fromRegion.isMember(player.getUniqueId());

            if (!bypassed && !isMember
                    && !regionManager.getFlagBooleanValue(fromRegion, "exit", true)) {
                event.setCancelled(true);
            }
        }
    }

    private void sendMessage(Player player, String key, String fallback) {
        if (plugin.getLanguageManager() != null) {
            plugin.getLanguageManager().sendMessage(player, key);
        } else {
            player.sendMessage(fallback);
        }
    }
}