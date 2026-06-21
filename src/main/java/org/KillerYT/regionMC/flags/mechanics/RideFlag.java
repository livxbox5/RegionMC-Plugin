package org.KillerYT.regionMC.flags.mechanics;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class RideFlag extends ListenerFlag<Boolean> {
    public RideFlag(RegionMC plugin) { super(plugin, "ride", true); }
    @Override public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override public String getDescription() { return "Посадка на транспорт/животных"; }

    /**
     *
     *  Логика ride (транспорт)
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player p)) return;
        if (isFlagDenied(event.getVehicle().getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.ride", "§cВы не можете садиться на транспорт здесь!");
        }
    }

    /**
     *
     *  Логика ride (животные)
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player p = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Vehicle || entity instanceof Sittable)) return;
        if (isFlagDenied(entity.getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.ride", "§cВы не можете садиться на животных здесь!");
        }
    }
}