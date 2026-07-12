package org.KillerYT.regionMC.commands.subcommands;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.managers.RegionManager;
import org.KillerYT.regionMC.utils.ParticleUtil;
import org.KillerYT.regionMC.utils.PermissionUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RegionShowHideCommand {

    private final RegionMC plugin;
    private final RegionManager regionManager;
    private final Map<UUID, Set<String>> showingRegions = new HashMap<>();

    private static final int[] GREEN = {0, 255, 0};
    private static final int[] BLUE = {0, 0, 255};
    private static final int[] YELLOW = {255, 255, 0};
    private static final int[] RED = {255, 0, 0};

    public RegionShowHideCommand(RegionMC plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        startParticleScheduler();
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }

        if (!PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.command.show")) {
            // old: if (!player.hasPermission("regionmc.command.show")) {
            plugin.getLanguageManager().sendMessage(player, "commands.no-permission");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /region show <region_name>");
            player.sendMessage("§cUsage: /region hide <region_name>");
            player.sendMessage("§cUsage: /region hide all - hide all regions");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String regionName = args[1];

        switch (subCommand) {
            case "show" -> showRegionBounds(player, regionName);
            case "hide" -> {
                if (regionName.equalsIgnoreCase("all")) {
                    hideAllRegions(player);
                } else {
                    hideRegionBounds(player, regionName);
                }
            }
            default -> player.sendMessage("§cUnknown command. Use: show or hide");
        }
        return true;
    }

    private void showRegionBounds(Player player, String regionName) {
        if (!regionManager.regionExists(regionName)) {
            player.sendMessage("§cRegion '" + regionName + "' not found!");
            return;
        }

        if (!regionManager.isOwner(player, regionName) &&
                !PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.admin")) {
            // old: !player.hasPermission("regionmc.admin")
            player.sendMessage("§cYou don't have permission to view this region's boundaries!");
            return;
        }

        Location pos1 = regionManager.getRegionPos1(regionName);
        Location pos2 = regionManager.getRegionPos2(regionName);

        if (pos1 == null || pos2 == null) {
            player.sendMessage("§cRegion '" + regionName + "' doesn't have set boundaries!");
            return;
        }

        showingRegions.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(regionName);

        player.sendMessage("§aShowing boundaries of region '" + regionName + "'");
        player.sendMessage("§7To hide boundaries, use: /region hide " + regionName);

        showParticles(player, pos1, pos2);
    }

    private void hideRegionBounds(Player player, String regionName) {
        UUID playerId = player.getUniqueId();
        Set<String> playerRegions = showingRegions.get(playerId);

        if (playerRegions != null && playerRegions.remove(regionName)) {
            if (playerRegions.isEmpty()) {
                showingRegions.remove(playerId);
            }
            player.sendMessage("§eHiding boundaries of region '" + regionName + "'");
        } else {
            player.sendMessage("§eRegion '" + regionName + "' boundaries are not displayed");
        }
    }

    private void hideAllRegions(Player player) {
        if (showingRegions.remove(player.getUniqueId()) != null) {
            player.sendMessage("§eHiding boundaries of all regions");
        } else {
            player.sendMessage("§eNo active region boundary displays");
        }
    }

    private void startParticleScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                showingRegions.entrySet().removeIf(entry -> {
                    Player player = plugin.getServer().getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        showPlayerRegions(player, entry.getValue());
                        return false;
                    }
                    return true;
                });
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void showPlayerRegions(Player player, Set<String> regionNames) {
        Location playerLoc = player.getLocation();
        int maxDistance = 100;

        for (String regionName : regionNames) {
            if (regionManager.regionExists(regionName) &&
                    (regionManager.isOwner(player, regionName) ||
                            PermissionUtil.hasPermission(player, plugin.getAdminManager(), "regionmc.admin"))) {
                // old: player.hasPermission("regionmc.admin")

                Location pos1 = regionManager.getRegionPos1(regionName);
                Location pos2 = regionManager.getRegionPos2(regionName);

                if (pos1 != null && pos2 != null && isRegionNearPlayer(playerLoc, pos1, pos2, maxDistance)) {
                    showParticles(player, pos1, pos2);
                }
            }
        }
    }

    private boolean isRegionNearPlayer(Location playerLoc, Location pos1, Location pos2, int maxDistance) {
        if (!playerLoc.getWorld().equals(pos1.getWorld())) {
            return false;
        }

        Location center = new Location(
                pos1.getWorld(),
                (pos1.getX() + pos2.getX()) / 2,
                (pos1.getY() + pos2.getY()) / 2,
                (pos1.getZ() + pos2.getZ()) / 2
        );

        return center.distance(playerLoc) <= maxDistance;
    }

    private void showParticles(Player player, Location pos1, Location pos2) {
        RegionBounds bounds = RegionBounds.from(pos1, pos2);
        showCorners(player, bounds);
        showEdges(player, bounds);
    }

    private void showCorners(Player player, RegionBounds bounds) {
        spawnParticle(player, bounds.minX(), bounds.minY(), bounds.minZ(), GREEN);
        spawnParticle(player, bounds.maxX(), bounds.minY(), bounds.minZ(), GREEN);
        spawnParticle(player, bounds.minX(), bounds.minY(), bounds.maxZ(), GREEN);
        spawnParticle(player, bounds.maxX(), bounds.minY(), bounds.maxZ(), GREEN);

        spawnParticle(player, bounds.minX(), bounds.maxY(), bounds.minZ(), BLUE);
        spawnParticle(player, bounds.maxX(), bounds.maxY(), bounds.minZ(), BLUE);
        spawnParticle(player, bounds.minX(), bounds.maxY(), bounds.maxZ(), BLUE);
        spawnParticle(player, bounds.maxX(), bounds.maxY(), bounds.maxZ(), BLUE);
    }

    private void showEdges(Player player, RegionBounds bounds) {
        double density = 2.0;
        drawHorizontalEdges(player, bounds, density);
        drawVerticalEdges(player, bounds, density);
    }

    private void drawHorizontalEdges(Player player, RegionBounds bounds, double density) {
        for (double x = bounds.minX(); x <= bounds.maxX(); x += density) {
            spawnParticle(player, x, bounds.minY(), bounds.minZ(), GREEN);
            spawnParticle(player, x, bounds.minY(), bounds.maxZ(), GREEN);
        }
        for (double z = bounds.minZ(); z <= bounds.maxZ(); z += density) {
            spawnParticle(player, bounds.minX(), bounds.minY(), z, GREEN);
            spawnParticle(player, bounds.maxX(), bounds.minY(), z, GREEN);
        }

        for (double x = bounds.minX(); x <= bounds.maxX(); x += density) {
            spawnParticle(player, x, bounds.maxY(), bounds.minZ(), YELLOW);
            spawnParticle(player, x, bounds.maxY(), bounds.maxZ(), YELLOW);
        }
        for (double z = bounds.minZ(); z <= bounds.maxZ(); z += density) {
            spawnParticle(player, bounds.minX(), bounds.maxY(), z, YELLOW);
            spawnParticle(player, bounds.maxX(), bounds.maxY(), z, YELLOW);
        }
    }

    private void drawVerticalEdges(Player player, RegionBounds bounds, double density) {
        for (double y = bounds.minY(); y <= bounds.maxY(); y += density * 2) {
            spawnParticle(player, bounds.minX(), y, bounds.minZ(), RED);
            spawnParticle(player, bounds.maxX(), y, bounds.minZ(), RED);
            spawnParticle(player, bounds.minX(), y, bounds.maxZ(), RED);
            spawnParticle(player, bounds.maxX(), y, bounds.maxZ(), RED);
        }
    }

    private void spawnParticle(Player player, double x, double y, double z, int[] color) {
        Location loc = new Location(player.getWorld(), x + 0.5, y + 0.5, z + 0.5);
        ParticleUtil.displayColoredParticle(player, loc, color[0], color[1], color[2]);
    }

    private record RegionBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        static RegionBounds from(Location pos1, Location pos2) {
            return new RegionBounds(
                    Math.min(pos1.getBlockX(), pos2.getBlockX()),
                    Math.min(pos1.getBlockY(), pos2.getBlockY()),
                    Math.min(pos1.getBlockZ(), pos2.getBlockZ()),
                    Math.max(pos1.getBlockX(), pos2.getBlockX()),
                    Math.max(pos1.getBlockY(), pos2.getBlockY()),
                    Math.max(pos1.getBlockZ(), pos2.getBlockZ())
            );
        }
    }
}