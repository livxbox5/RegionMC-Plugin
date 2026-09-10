package org.KillerYT.regionMC.utils;

import org.KillerYT.regionMC.managers.AdminManager;
import org.bukkit.entity.Player;

public class PermissionUtil {

    public static boolean hasPermission(Player player, AdminManager adminManager, String permission) {
        if (player == null || permission == null) return false;

        if (adminManager != null && adminManager.isAdmin(player)) return true;
        if (player.isOp()) return true;
        if (player.hasPermission("*")) return true;
        if (player.hasPermission("regionmc.*")) return true;
        if (player.hasPermission(permission)) return true;

        // Wildcard-цепочка: regionmc.command.info -> regionmc.command.* -> regionmc.*
        String[] parts = permission.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            sb.append(parts[i]).append(".");
            if (player.hasPermission(sb + "*")) return true;
        }
        return false;
    }
}