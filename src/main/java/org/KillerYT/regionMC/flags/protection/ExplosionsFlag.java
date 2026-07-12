package org.KillerYT.regionMC.flags.protection;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

public class ExplosionsFlag extends ListenerFlag<Boolean> {

    public ExplosionsFlag(RegionMC plugin) {
        super(plugin, "explosions", false);
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
        return "Общий контроль всех взрывов в регионе (огненные шары и т.д.). TNT и КРИПЕРЫ НЕ ВКЛЮЧЕНЫ (используйте tnt-explosion и creeper-explosion)";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        // ========== ИСКЛЮЧАЕМ TNT ==========
        if (event.getEntity() instanceof TNTPrimed) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("ExplosionsFlag: Skipping TNT explosion (handled by TntFlag)");
            }
            return;
        }

        // ========== ИСКЛЮЧАЕМ КРИПЕРОВ ==========
        if (event.getEntity() instanceof Creeper) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("ExplosionsFlag: Skipping Creeper explosion (handled by CreeperExplosionFlag)");
            }
            return;
        }
        // ==========================================

        Location explosionLoc = event.getLocation();

        // Если место взрыва в регионе с запретом - отменяем весь взрыв
        if (isFlagDenied(explosionLoc, "explosions")) {
            event.setCancelled(true);
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("Explosion completely prevented at " + explosionLoc);
            }
            return;
        }

        // Защита блоков внутри регионов от взрыва
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Location blockLoc = block.getLocation();

            // Если блок в регионе с запретом взрывов - не даём его взорвать
            if (isFlagDenied(blockLoc, "explosions")) {
                iterator.remove();
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info("Protected block at " + blockLoc + " from explosion");
                }
            }
        }
    }
}