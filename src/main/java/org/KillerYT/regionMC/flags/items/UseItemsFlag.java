package org.KillerYT.regionMC.flags.items;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class UseItemsFlag extends ListenerFlag<Boolean> {
    public UseItemsFlag(RegionMC plugin) {
        super(plugin, "use-items", false);
    }

    @Override
    public Boolean parseInput(String input) {
        return convertToBoolean(input);
    }

    @Override
    public boolean isAllowed(Boolean value) {
        return value != null ? value : getDefaultValue();
    }

    @Override
    public String getDescription() {
        return "Использование предметов (еда, зелья, вёдра и т.д.)";
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null) return;

        Player p = event.getPlayer();
        Material itemType = event.getItem().getType();

        // Размещение блоков – пропускаем
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && itemType.isBlock()) {
            return;
        }

        // Если предмет – еда или зелье, всегда разрешаем (можно вынести в конфиг)
        if (itemType.isEdible() || itemType.name().contains("POTION")) {
            return;
        }

        Location loc = (event.getClickedBlock() != null)
                ? event.getClickedBlock().getLocation()
                : p.getLocation();

        if (isActionDenied(p, loc, getName())) {
            event.setCancelled(true);
            sendDenyMessage(p, loc, "protection.use-items", "§cВы не можете использовать предметы здесь!");
        }
    }
}