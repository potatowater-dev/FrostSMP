package com.frostsmp.pearls.listener;

import com.frostsmp.pearls.config.PluginConfig;
import com.frostsmp.pearls.tracker.PearlCatchTracker;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

public final class WindChargeBoostListener implements Listener {

    private final PluginConfig config;
    private final PearlCatchTracker tracker;

    public WindChargeBoostListener(PluginConfig config, PearlCatchTracker tracker) {
        this.config = config;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWindChargeHit(ProjectileHitEvent event) {
        if (!config.isEnabled() || !config.isWindChargeEnabled()) {
            return;
        }
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof WindCharge)) {
            return;
        }
        if (event.getHitEntity() == null || !(event.getHitEntity() instanceof EnderPearl pearl)) {
            return;
        }

        PearlCatchTracker.TrackedPearl tracked = tracker.get(pearl);
        if (tracked == null) {
            return;
        }

        if (!(projectile.getShooter() instanceof Player shooter)) {
            return;
        }
        if (config.isRequireSamePlayer() && !tracked.throwerId().equals(shooter.getUniqueId())) {
            return;
        }

        Vector current = pearl.getVelocity();
        double boostedY = Math.min(
                config.getMaxVerticalSpeed(),
                current.getY() + config.getWindChargeBoostY()
        );
        pearl.setVelocity(new Vector(0, boostedY, 0));
        tracker.untrack(pearl);

        if (config.isDebug()) {
            shooter.sendMessage("FrostPearls: pearl-catch boost Y=" + String.format("%.2f", boostedY));
        }
    }
}
