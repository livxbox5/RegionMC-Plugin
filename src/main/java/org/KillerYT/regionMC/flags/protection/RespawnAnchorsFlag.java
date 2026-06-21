package org.KillerYT.regionMC.flags.protection;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class RespawnAnchorsFlag extends ListenerFlag<Boolean> {
    public RespawnAnchorsFlag(RegionMC plugin) { super(plugin, "respawn-anchors", true); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Якоря возрождения"; }

    /**
     *
     *  Логика respawn-anchors
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.RESPAWN_ANCHOR
                || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = event.getPlayer();
        if (isActionDenied(p, event.getClickedBlock().getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.respawn-anchors", "§cВы не можете использовать якоря возрождения здесь!");
        }
    }
}