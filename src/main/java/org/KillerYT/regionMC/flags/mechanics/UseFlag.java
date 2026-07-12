package org.KillerYT.regionMC.flags.mechanics;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;

public class UseFlag extends ListenerFlag<Boolean> {
    public UseFlag(RegionMC plugin) { super(plugin, "use", false); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Использование предметов и блоков"; }

    /**
     *
     *  Логика use (использование блоков и предметов)
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        // Проверяем и клик по блоку, и использование предмета в руке
        if (event.getClickedBlock() != null || event.getItem() != null) {
            if (isActionDenied(p, p.getLocation(), getName())) {
                event.setCancelled(true);
                sendMessage(p, "protection.use", "§cВы не можете использовать это здесь!");
            }
        }
    }
}