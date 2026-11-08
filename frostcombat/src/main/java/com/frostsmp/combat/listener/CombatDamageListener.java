package com.frostsmp.combat.listener;

import com.frostsmp.combat.service.CombatService;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class CombatDamageListener implements Listener {

    private final CombatService combatService;

    public CombatDamageListener(CombatService combatService) {
        this.combatService = combatService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player damager = resolvePlayerDamager(event.getDamager());
        if (damager == null) {
            return;
        }

        Entity victim = event.getEntity();
        if (victim instanceof Player playerVictim) {
            combatService.tag(playerVictim, damager);
            combatService.tag(damager);
        } else if (isMannequin(victim)) {
            combatService.tag(damager);
        }
    }

    private static boolean isMannequin(Entity entity) {
        return entity instanceof Mannequin || entity.getType() == EntityType.MANNEQUIN;
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
