package org.KillerYT.regionMC.commands;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SubCommand {
    void execute(@NotNull Player player, @NotNull String[] args);

    @NotNull List<String> onTabComplete(@NotNull Player player, @NotNull String[] args);
}