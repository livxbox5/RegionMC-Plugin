package org.KillerYT.regionMC.flags.items;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;

public class UseItemsFlag extends ListenerFlag<Boolean> {
    public UseItemsFlag(RegionMC plugin) { super(plugin, "use-items", false); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Использование предметов"; }

    /**
     *
     *  Логика use-items
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        Player p = event.getPlayer();
        if (isActionDenied(p, p.getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.use-items", "§cВы не можете использовать предметы здесь!");
        }
    }
}