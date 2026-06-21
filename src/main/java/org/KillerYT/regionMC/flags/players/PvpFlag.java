package org.KillerYT.regionMC.flags.players;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PvpFlag extends ListenerFlag<Boolean> {

    public PvpFlag(RegionMC plugin) {
        super(plugin, "pvp", true);
    }

    @Override
    public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override
    public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override
    public String getDescription() { return "Управление PvP"; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player target)) return;
        String damagerRegion = regionManager.findRegionAtLocation(damager.getLocation());
        String targetRegion = regionManager.findRegionAtLocation(target.getLocation());
        if ((damagerRegion != null && shouldBypassRegionProtection(damager, damagerRegion))
                || (targetRegion != null && shouldBypassRegionProtection(damager, targetRegion))) return;
        if ((damagerRegion != null && isFlagDenied(damager.getLocation(), "pvp"))
                || (targetRegion != null && isFlagDenied(target.getLocation(), "pvp"))) {
            event.setCancelled(true);
            sendMessage(damager, "protection.pvp", "§cPvP отключен здесь!");
        }
    }
}