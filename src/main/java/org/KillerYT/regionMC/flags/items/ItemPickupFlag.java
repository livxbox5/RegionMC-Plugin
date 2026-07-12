package org.KillerYT.regionMC.flags.items;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class ItemPickupFlag extends ListenerFlag<Boolean> {
    public ItemPickupFlag(RegionMC plugin) { super(plugin, "item-pickup", true); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Подбор предметов"; }

    /**
     *
     *  Логика item-pickup
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p &&
                isActionDenied(p, event.getItem().getLocation(), getName())) {
            event.setCancelled(true);
        }
    }
}