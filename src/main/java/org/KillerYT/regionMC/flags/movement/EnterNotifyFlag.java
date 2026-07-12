package org.KillerYT.regionMC.flags.movement;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;

public class EnterNotifyFlag extends ListenerFlag<Boolean> {
    public EnterNotifyFlag(RegionMC plugin) {
        super(plugin, "enter-notify", false);
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
        return "Уведомлять владельцев и участников о входе игрока в регион";
    }
}