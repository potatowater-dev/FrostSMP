package com.frostsmp.combat.listener;

import com.frostsmp.combat.config.PluginConfig;
import com.frostsmp.combat.service.ExpoCartTracker;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class ExplosionDamageListener implements Listener {

    private final PluginConfig config;
    private final ExpoCartTracker tracker;

    public ExplosionDamageListener(PluginConfig config, ExpoCartTracker tracker) {
        this.config = config;
        this.tracker = tracker;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent event) {
        if (!config.isExplosionDamageEnabled()) {
            return;
        }
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        Entity source = resolveExplosionSource(event);
        if (!isTntMinecart(source)) {
            return;
        }

        Boolean expo = tracker.consumeExplosionType(source.getUniqueId());
        if (expo == null) {
            expo = false;
        }
        event.setDamage(expo ? config.getExpoCartDamage() : config.getMinecartDamage());
    }

    private static Entity resolveExplosionSource(EntityDamageEvent event) {
        Entity causing = event.getDamageSource().getCausingEntity();
        if (causing != null) {
            return causing;
        }
        return event.getDamageSource().getDirectEntity();
    }

    private static boolean isTntMinecart(Entity entity) {
        return entity instanceof ExplosiveMinecart || entity.getType() == EntityType.TNT_MINECART;
    }
}
