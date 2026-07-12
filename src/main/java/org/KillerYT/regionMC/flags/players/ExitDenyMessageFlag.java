package org.KillerYT.regionMC.flags.players;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;

public class ExitDenyMessageFlag extends ListenerFlag<String> {

    public ExitDenyMessageFlag(RegionMC plugin) {
        super(plugin, "exit-deny-message", null);
    }

    @Override
    public String parseInput(String input) {
        if (input == null || input.trim().isEmpty()) return null;
        String trimmed = input.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (trimmed.equalsIgnoreCase("allow") || trimmed.equalsIgnoreCase("deny") ||
                trimmed.equalsIgnoreCase("default") || trimmed.equalsIgnoreCase("remove")) {
            return null;
        }
        return trimmed;
    }

    @Override
    public boolean isAllowed(String value) {
        return true;
    }

    @Override
    public String getDescription() {
        return "Кастомное сообщение при запрете выхода из региона";
    }
}