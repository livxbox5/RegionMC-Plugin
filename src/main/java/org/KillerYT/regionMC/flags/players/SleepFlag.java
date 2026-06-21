package org.KillerYT.regionMC.flags.players;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBedEnterEvent;

public class SleepFlag extends ListenerFlag<Boolean> {
    public SleepFlag(RegionMC plugin) { super(plugin, "sleep", false); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Сон в кровати"; }

    /**
     *
     *  Логика sleep
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        Player p = event.getPlayer();
        if (isActionDenied(p, event.getBed().getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.sleep", "§cВы не можете спать здесь!");
        }
    }
}