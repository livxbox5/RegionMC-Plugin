package org.KillerYT.regionMC.flags.movement;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;

public class ExitNotifyFlag extends ListenerFlag<Boolean> {
    public ExitNotifyFlag(RegionMC plugin) {
        super(plugin, "exit-notify", false);
    }

    @Override
    public Boolean parseInput(String input) {
        return convertToBoolean(input);
    }

    @Override
    public boolean isAllowed(Boolean value) {
        return value != null && value;
    }

    @Override
    public String getDescription() {
        return "Уведомлять владельцев и участников о выходе игрока из региона";
    }
}