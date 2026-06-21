package org.KillerYT.regionMC.flags.players;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;

public class ExpDropsFlag extends ListenerFlag<Boolean> {
    public ExpDropsFlag(RegionMC plugin) { super(plugin, "exp-drops", true); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Выпадение опыта"; }

    /**
     *
     *  Логика exp-drops
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player p &&
                isActionDenied(p, p.getLocation(), getName())) {
            event.setDroppedExp(0);
        }
    }
}