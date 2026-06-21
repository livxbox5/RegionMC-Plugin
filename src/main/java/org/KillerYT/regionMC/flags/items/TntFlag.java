package org.KillerYT.regionMC.flags.items;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.Iterator;

public class TntFlag extends ListenerFlag<Boolean> {

    public TntFlag(RegionMC plugin) {
        super(plugin, "tnt-explosion", false);
    }

    @Override
    public Boolean parseInput(String input) {
        return convertToBoolean(input);
    }

    @Override
    public boolean isAllowed(Boolean value) {
        return value != null ? value : getDefaultValue();
    }

    @Override
    public String getDescription() {
        return "Контроль TNT (allow = взрывает блоки, deny = не взрывает блоки)";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTntPrime(ExplosionPrimeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        Location loc = tnt.getLocation();

        if (isFlagDenied(loc, "tnt-explosion")) {
            event.setCancelled(true);
            tnt.remove();
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("TNT prime prevented in region at " + loc);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTntExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof TNTPrimed tnt)) return;

        Location tntLoc = tnt.getLocation();

        // Проверяем КАЖДЫЙ БЛОК в списке, НЕЗАВИСИМО от места взрыва
        Iterator<Block> iterator = event.blockList().iterator();
        int protectedCount = 0;
        int destroyedCount = 0;

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Location blockLoc = block.getLocation();

            // Проверяем флаг tnt-explosion ДЛЯ КАЖДОГО БЛОКА
            // Блок защищается, если он находится в регионе с deny
            if (isFlagDenied(blockLoc, "tnt-explosion")) {
                iterator.remove(); // Блок НЕ разрушится
                protectedCount++;
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info("Protected block at " + blockLoc + " from TNT explosion");
                }
            } else {
                destroyedCount++;
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info("Block at " + blockLoc + " will be destroyed (allow)");
                }
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info("TNT explosion at " + tntLoc +
                    " - Destroyed: " + destroyedCount + ", Protected: " + protectedCount);
        }

        // Если все блоки защищены - отменяем взрыв полностью
        if (event.blockList().isEmpty()) {
            event.setCancelled(true);
            tnt.remove();
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("TNT explosion completely cancelled (all blocks protected)");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTntPlace(BlockPlaceEvent event) {
        if (event.getBlock().getType() != Material.TNT) return;

        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        String regionName = regionManager.findRegionAtLocation(loc);
        if (regionName == null) return;

        if (shouldBypassRegionProtection(player, regionName)) return;

        if (isFlagDenied(loc, "tnt-explosion") && plugin.isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " placed TNT in region with tnt-explosion=deny at " + loc);
        }
    }
}