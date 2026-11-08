package com.frostsmp.ffa.kill;

import com.frostsmp.ffa.arena.MembershipCache;
import com.frostsmp.ffa.config.PluginConfig;
import com.frostsmp.ffa.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KillCreditListener implements Listener {

    private static final long EXPIRY_MS = 15_000L;

    private final PluginConfig config;
    private final MembershipCache membership;
    private final Map<UUID, Hit> lastHit = new ConcurrentHashMap<>();

    private record Hit(UUID attacker, long at) {
    }

    public KillCreditListener(PluginConfig config, MembershipCache membership) {
        this.config = config;
        this.membership = membership;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolve(event);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        lastHit.put(victim.getUniqueId(), new Hit(attacker.getUniqueId(), System.currentTimeMillis()));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Hit hit = lastHit.remove(victim.getUniqueId());
        if (hit == null || System.currentTimeMillis() - hit.at() > EXPIRY_MS) {
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null) {
            killer = victim.getServer().getPlayer(hit.attacker());
        }
        if (killer == null || killer.equals(victim)) {
            return;
        }
        if (!membership.isInArena(victim) && !membership.isInArena(killer)) {
            return;
        }
        MessageUtil.broadcast(config.prefixed("kill",
                "killer", killer.getName(),
                "victim", victim.getName()));
    }

    private static Player resolve(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
