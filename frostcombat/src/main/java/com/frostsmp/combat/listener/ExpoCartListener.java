package com.frostsmp.combat.listener;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.service.ExpoCartTracker;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

public final class ExpoCartListener implements Listener {

    private final PluginConfig config;
    private final ExpoCartTracker tracker;

    public ExpoCartListener(PluginConfig config, ExpoCartTracker tracker) {
        this.config = config;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getHitEntity() instanceof ExplosiveMinecart cart)) {
            return;
        }
        if (isFlamingProjectile(event.getEntity())) {
            tracker.tagExpo(cart.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!(event.getEntity() instanceof ExplosiveMinecart cart)) {
            return;
        }
        boolean expo = tracker.wasProjectileTagged(cart.getUniqueId()) || isHighVelocity(cart.getVelocity());
        tracker.classifyExplosion(cart.getUniqueId(), expo);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMinecartRemoved(EntityDamageEvent event) {
        if (event.getEntity().getType() != EntityType.TNT_MINECART) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            tracker.clear(event.getEntity().getUniqueId());
        }
    }

    private boolean isHighVelocity(Vector velocity) {
        return velocity.length() >= config.getExpoCartVelocityThreshold();
    }

    private static boolean isFlamingProjectile(Entity projectile) {
        if (projectile instanceof AbstractArrow arrow) {
            return arrow.getFireTicks() > 0;
        }
        return projectile instanceof Fireball || projectile instanceof SmallFireball;
    }
}
