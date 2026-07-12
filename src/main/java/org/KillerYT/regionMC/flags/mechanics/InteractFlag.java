package org.KillerYT.regionMC.flags.mechanics;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractFlag extends ListenerFlag<Boolean> {
    public InteractFlag(RegionMC plugin) { super(plugin, "interact", false); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Взаимодействие с блоками"; }

    /**
     *
     *  Логика interact
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player p = event.getPlayer();
        Material type = event.getClickedBlock().getType();
        if (type == Material.CHEST || type == Material.TRAPPED_CHEST || type.name().contains("SHULKER_BOX")
                || type == Material.BARREL || type == Material.ENDER_CHEST) return;
        if (isActionDenied(p, event.getClickedBlock().getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.interact", "§cВы не можете взаимодействовать здесь!");
        }
    }
}