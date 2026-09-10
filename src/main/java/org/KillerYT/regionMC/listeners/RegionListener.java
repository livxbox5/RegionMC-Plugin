package org.KillerYT.regionMC.listeners;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isDenied(event.getPlayer(), event.getBlock().getLocation(), "break")) {
            event.setCancelled(true);
            if (plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().sendMessage(event.getPlayer(), "protection.break");
            } else {
                event.getPlayer().sendMessage("§cВы не можете разрушать блоки в этом регионе!");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isDenied(event.getPlayer(), event.getBlock().getLocation(), "build")) {
            event.setCancelled(true);
            if (plugin.getLanguageManager() != null) {
                plugin.getLanguageManager().sendMessage(event.getPlayer(), "protection.build");
            } else {
                event.getPlayer().sendMessage("§cВы не можете строить в этом регионе!");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            if (isDenied(event.getPlayer(), event.getClickedBlock().getLocation(), "interact")) {
                event.setCancelled(true);
                if (plugin.getLanguageManager() != null) {
                    plugin.getLanguageManager().sendMessage(event.getPlayer(), "protection.interact");
                }
                return;
            }
        }

        if (event.getItem() != null) {
            if (isDenied(event.getPlayer(), event.getPlayer().getLocation(), "use-items")) {
                event.setCancelled(true);
                if (plugin.getLanguageManager() != null) {
                    plugin.getLanguageManager().sendMessage(event.getPlayer(), "protection.use");
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (regionManager.isAdminBypass(player)) return;

            Region region = regionManager.getRegionAtLocation(event.getEntity().getLocation());
            if (region != null) {
                boolean pvpAllowed = regionManager.getFlagBooleanValue(region, "pvp", true);
                if (!pvpAllowed && !region.isOwner(player.getUniqueId()) && !region.isMember(player.getUniqueId())) {
                    event.setCancelled(true);
                    if (plugin.getLanguageManager() != null) {
                        plugin.getLanguageManager().sendMessage(player, "protection.pvp");
                    } else {
                        player.sendMessage("§cPvP отключен в этом регионе!");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) return;

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Region toRegion = regionManager.getRegionAtLocation(to);
        Region fromRegion = regionManager.getRegionAtLocation(from);

        // Сообщения о входе/выходе отключены (дублируются с флагами)
    }

    private boolean isDenied(Player player, Location location, String flagName) {
        if (regionManager.isAdminBypass(player)) return false;

        Region region = regionManager.getRegionAtLocation(location);
        if (region == null) return false;

        if (region.isOwner(player.getUniqueId()) || region.isMember(player.getUniqueId())) {
            return false;
        }

        return !regionManager.getFlagBooleanValue(region, flagName, true);
    }
}