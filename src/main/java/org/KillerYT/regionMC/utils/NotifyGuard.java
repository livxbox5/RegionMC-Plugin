package org.KillerYT.regionMC.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NotifyGuard {

    private static final long COOLDOWN_MS = 2000L;
    private static final Map<String, Long> LAST = new ConcurrentHashMap<>();

    private NotifyGuard() {}

    public static boolean shouldSend(UUID playerId, String regionName, String type) {
        if (playerId == null || regionName == null) return true;

        String key = playerId + "|" + regionName.toLowerCase() + "|" + type;
        long now = System.currentTimeMillis();

        Long last = LAST.get(key);
        if (last != null && now - last < COOLDOWN_MS) {
            return false;
        }
        LAST.put(key, now);

        if (LAST.size() > 1000) {
            LAST.entrySet().removeIf(e -> now - e.getValue() > 60_000L);
        }
        return true;
    }
}