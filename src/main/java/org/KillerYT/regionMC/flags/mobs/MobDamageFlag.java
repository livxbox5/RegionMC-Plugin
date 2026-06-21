package org.KillerYT.regionMC.flags.mobs;

import org.KillerYT.regionMC.RegionMC;
import org.KillerYT.regionMC.flags.main.ListenerFlag;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MobDamageFlag extends ListenerFlag<Boolean> {

    public MobDamageFlag(RegionMC plugin) {
        super(plugin, "mob-damage", true);
    }

    @Override
    public Boolean parseInput(String input) { return convertToBoolean(input); }
    @Override
    public boolean isAllowed(Boolean value) { return value != null ? value : getDefaultValue(); }
    @Override
    public String getDescription() { return "Урон от мобов"; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter)
            damager = shooter;
        if (!isHostileMob(damager)) return;
        String region = regionManager.findRegionAtLocation(p.getLocation());
        if (region == null || shouldBypassRegionProtection(p, region)) return;
        if (isFlagDenied(p.getLocation(), getName())) {
            event.setCancelled(true);
            sendMessage(p, "protection.mob-damage", "§cВы не можете получать урон от мобов здесь!");
        }
    }

    private boolean isHostileMob(Entity entity) {
        EntityType type = entity.getType();
        return switch (type) {
            case ZOMBIE, SKELETON, SPIDER, CREEPER, ENDERMAN, WITCH, SLIME, MAGMA_CUBE, GHAST, BLAZE,
                 PHANTOM, ENDER_DRAGON, WITHER, WITHER_SKELETON, HOGLIN, ZOGLIN, PIGLIN, PIGLIN_BRUTE,
                 PILLAGER, VINDICATOR, EVOKER, RAVAGER, VEX, GUARDIAN, ELDER_GUARDIAN, SHULKER,
                 ENDERMITE, WARDEN, BREEZE, SILVERFISH, DROWNED, HUSK, STRAY, CAVE_SPIDER, ILLUSIONER -> true;
            default -> false;
        };
    }
}