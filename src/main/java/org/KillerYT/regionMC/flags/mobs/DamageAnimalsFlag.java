package org.KillerYT.regionMC.flags.mobs;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class DamageAnimalsFlag extends ListenerFlag<Boolean> {

    public DamageAnimalsFlag(RegionMC plugin) {
        super(plugin, "damage-animals", true);
    }

    @Override
    public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override
    public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override
    public String getDescription() { return "Урон животным"; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p)) return;
        if (!isFriendly(event.getEntity())) return;
        String region = regionManager.findRegionAtLocation(event.getEntity().getLocation());
        if (region == null || shouldBypassRegionProtection(p, region)) return;
        if (isFlagDenied(event.getEntity().getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.damage-animals", "§cВы не можете наносить урон животным здесь!");
        }
    }

    private boolean isFriendly(Entity entity) {
        return entity instanceof Animals || entity instanceof WaterMob || entity instanceof Ambient
                || entity instanceof Golem || entity instanceof Villager;
    }
}