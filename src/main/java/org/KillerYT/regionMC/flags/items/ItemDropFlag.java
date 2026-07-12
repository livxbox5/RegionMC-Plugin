package org.KillerYT.regionMC.flags.items;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ItemDropFlag extends ListenerFlag<Boolean> {
    public ItemDropFlag(RegionMC plugin) { super(plugin, "item-drop", true); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Выбрасывание предметов"; }

    /**
     *
     *  Логика item-drop
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player p = event.getPlayer();
        if (isActionDenied(p, p.getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.itemdrop", "§cВы не можете выбрасывать предметы здесь!");
        }
    }
}