package org.KillerYT.regionMC.utils;

import org.KillerYT.regionMC.managers.AdminManager;
import org.bukkit.entity.Player;

public class PermissionUtil {

    public static boolean hasPermission(Player player, AdminManager adminManager, String permission) {
        if (player == null) return false;
        // Только admin-user даёт доступ
        if (adminManager != null && adminManager.isAdmin(player)) {
            return true;
        }
// Временное решение – использовать права
        return player.isOp() ||
                player.hasPermission("*") ||
                player.hasPermission("regionmc.*") ||
                player.hasPermission("regionmc.admin") ||
                player.hasPermission(permission);

    }
}