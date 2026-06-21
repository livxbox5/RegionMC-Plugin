package org.KillerYT.regionMC.flags.mobs;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import java.util.Iterator;

public class CreeperExplosionFlag extends ListenerFlag<Boolean> {

    public CreeperExplosionFlag(RegionMC plugin) {
        super(plugin, "creeper-explosion", true);
    }

    @Override
    public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override
    public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override
    public String getDescription() { return "Управляет взрывами криперов (allow = взрывает блоки, deny = не взрывает блоки)"; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;

        Location creeperLoc = creeper.getLocation();

        // Проверяем КАЖДЫЙ БЛОК в радиусе взрыва
        Iterator<Block> iterator = event.blockList().iterator();
        int protectedCount = 0;
        int destroyedCount = 0;

        while (iterator.hasNext()) {
            Block block = iterator.next();
            Location blockLoc = block.getLocation();

            // Если блок в регионе с запретом криперов - защищаем его
            if (isFlagDenied(blockLoc, getName())) {
                iterator.remove();
                protectedCount++;
                if (plugin.isDebugEnabled()) {
                    plugin.getLogger().info("Protected block at " + blockLoc + " from creeper explosion");
                }
            } else {
                destroyedCount++;
            }
        }

        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info("Creeper explosion at " + creeperLoc +
                    " - Destroyed: " + destroyedCount + ", Protected: " + protectedCount);
        }

        // Если все блоки защищены - отменяем взрыв полностью
        if (event.blockList().isEmpty()) {
            event.setCancelled(true);
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().info("Creeper explosion completely cancelled (all blocks protected)");
            }
        }

        // Отменяем урон игрокам от взрыва
        cancelCreeperExplosionDamage(creeper);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(ExplosionPrimeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;
        Location loc = creeper.getLocation();
        if (isFlagDenied(loc, getName())) {
            event.setRadius(0);
            creeper.setExplosionRadius(0);
            debugLog("Creeper ignition cancelled at " + loc);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Creeper creeper && event.getEntity() instanceof Player p) {
            Location loc = creeper.getLocation();
            if (isFlagDenied(loc, getName())) {
                event.setCancelled(true);
                debugLog("Creeper damage to " + p.getName() + " cancelled");
            }
        }
    }

    private void cancelCreeperExplosionDamage(Creeper creeper) {
        double radius = creeper.isPowered() ? 8.0 : 3.0;
        creeper.getWorld().getLivingEntities().stream()
                .filter(e -> e.getLocation().distance(creeper.getLocation()) <= radius)
                .forEach(e -> {
                    e.setNoDamageTicks(20);
                    if (e instanceof Player p) p.sendMessage("§aВзрыв крипера был заблокирован!");
                });
    }
}