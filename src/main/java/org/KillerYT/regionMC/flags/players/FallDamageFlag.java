package org.KillerYT.regionMC.flags.players;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

public class FallDamageFlag extends ListenerFlag<Boolean> {
    public FallDamageFlag(RegionMC plugin) { super(plugin, "fall-damage", true); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Урон от падения"; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p &&
                event.getCause() == EntityDamageEvent.DamageCause.FALL &&
                isActionDenied(p, p.getLocation(), getName())) {
            event.setCancelled(true);
        }
    }
}