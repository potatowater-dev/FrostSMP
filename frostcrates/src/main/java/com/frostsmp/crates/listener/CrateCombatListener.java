package com.frostsmp.crates.listener;

import com.frostsmp.crates.service.CrateClaimService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class CrateCombatListener implements Listener {

    private final CrateClaimService claimService;

    public CrateCombatListener(CrateClaimService claimService) {
        this.claimService = claimService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!claimService.getSessions().isRolling(victim)) {
            return;
        }
        if (!isHitDamage(event)) {
            return;
        }
        claimService.interruptForCombat(victim);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = resolvePlayerDamager(event.getDamager());
        if (attacker == null || !claimService.getSessions().isRolling(attacker)) {
            return;
        }
        claimService.interruptForCombat(attacker);
    }

    private static boolean isHitDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            return true;
        }
        return switch (event.getCause()) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, THORNS, MAGIC, SONIC_BOOM -> true;
            default -> false;
        };
    }

    private static Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
