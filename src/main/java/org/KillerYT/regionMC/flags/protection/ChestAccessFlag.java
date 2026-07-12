package org.KillerYT.regionMC.flags.protection;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Set;

public class ChestAccessFlag extends ListenerFlag<Boolean> {
    private static final Set<Material> CONTAINERS = Set.of(
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
            Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
            Material.DISPENSER, Material.DROPPER, Material.HOPPER,
            Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX, Material.BROWN_SHULKER_BOX,
            Material.RED_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX, Material.CYAN_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX, Material.BLUE_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX,
            Material.PINK_SHULKER_BOX
    );

    public ChestAccessFlag(RegionMC plugin) { super(plugin, "chest-access", false); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Доступ к сундукам"; }

    /**
     *
     *  Логика chest-access
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Material type = event.getClickedBlock().getType();
        // ЭНДЕР-СУНДУК НЕ ОБРАБАТЫВАЕМ — им занимается EnderChestFlag
        if (type == Material.ENDER_CHEST) return;
        if (CONTAINERS.contains(type) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player p = event.getPlayer();
            if (isActionDenied(p, event.getClickedBlock().getLocation(), getName())) {
                event.setCancelled(true);
                sendMessage(p, "protection.chest-access", "§cВы не можете открывать контейнеры здесь!");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        if (event.getInventory().getLocation() == null) return;
        if (event.getInventory().getHolder() instanceof org.bukkit.block.EnderChest) return;
        if (isActionDenied(p, event.getInventory().getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.chest-access", "§cВы не можете открывать контейнеры здесь!");
        }
    }
}