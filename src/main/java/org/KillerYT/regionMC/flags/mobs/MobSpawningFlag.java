package org.KillerYT.regionMC.flags.mobs;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.KillerYT.regionMC.region.Region;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.ArrayList;
import java.util.List;

public class MobSpawningFlag extends ListenerFlag<Object> {

    public MobSpawningFlag(RegionMC plugin) {
        super(plugin, "mob-spawning", true);
    }

    @Override
    public Object parseInput(String input) {
        if (input == null || input.trim().isEmpty()) return true;
        String lower = input.toLowerCase().trim();

        // Boolean значения
        if (lower.equals("true") || lower.equals("allow") || lower.equals("yes") ||
                lower.equals("1") || lower.equals("разрешить"))
            return true;
        if (lower.equals("false") || lower.equals("deny") || lower.equals("no") ||
                lower.equals("0") || lower.equals("запретить"))
            return false;

        // Список мобов
        return parseMobList(input);
    }

    @Override
    public boolean isAllowed(Object value) {
        return value instanceof Boolean b ? b : true;
    }

    @Override
    public String getDescription() {
        return "Управление спавном мобов и работой спавнеров (allow/deny или список: cow,pig,zombie)";
    }

    private List<String> parseMobList(String input) {
        List<String> mobs = new ArrayList<>();
        for (String part : input.split("[,\\s]+")) {
            String mob = part.trim().toLowerCase();
            if (!mob.isEmpty()) mobs.add(mob);
        }
        return mobs;
    }

    /**
     *
     *  Логика mob-spawning (полный контроль над спавнерами и естественным спавном)
     *
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        String regionName = regionManager.findRegionAtLocation(event.getLocation());
        if (regionName == null) return;

        Region region = regionManager.getRegion(regionName);
        if (region == null) return;

        Object flagValue = region.getFlag(getName());
        if (flagValue == null) return;

        String mobName = event.getEntityType().name().toLowerCase();

        if (flagValue instanceof Boolean b) {
            // allow/true = всё работает как в ванилле
            // deny/false = спавнеры и естественный спавн полностью отключены
            if (!b) {
                event.setCancelled(true);
            }
        } else if (flagValue instanceof List<?> list) {
            // Список разрешённых мобов — спавнеры работают только для этих мобов
            boolean allowed = list.stream()
                    .anyMatch(o -> o.toString().toLowerCase().equals(mobName));
            if (!allowed) {
                event.setCancelled(true);
            }
        }
    }
}