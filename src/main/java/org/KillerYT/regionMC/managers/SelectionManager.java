package org.KillerYT.regionMC.managers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SelectionManager {
    private final Map<UUID, Location> pos1Map = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2Map = new ConcurrentHashMap<>();

    public void setPos1(Player player, Location location) {
        pos1Map.put(player.getUniqueId(), location);
    }

    public void setPos2(Player player, Location location) {
        pos2Map.put(player.getUniqueId(), location);
    }

    public Location getPos1(Player player) {
        return pos1Map.get(player.getUniqueId());
    }

    public Location getPos2(Player player) {
        return pos2Map.get(player.getUniqueId());
    }

    public void clearSelection(Player player) {
        UUID id = player.getUniqueId();
        pos1Map.remove(id);
        pos2Map.remove(id);
    }

    public boolean hasValidSelection(Player player) {
        return getPos1(player) != null && getPos2(player) != null;
    }

    public Location[] getSelection(Player player) {
        Location pos1 = getPos1(player);
        Location pos2 = getPos2(player);
        return (pos1 != null && pos2 != null) ? new Location[]{pos1, pos2} : null;
    }
}