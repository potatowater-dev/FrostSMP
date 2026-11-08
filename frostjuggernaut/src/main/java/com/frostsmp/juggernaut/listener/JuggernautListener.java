package com.frostsmp.juggernaut.listener;

import com.frostsmp.juggernaut.config.PluginConfig;
import com.frostsmp.juggernaut.service.BossBarService;
import com.frostsmp.juggernaut.service.EventService;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class JuggernautListener implements Listener {

    private final EventService eventService;
    private final PluginConfig config;
    private final BossBarService bossBarService;

    public JuggernautListener(EventService eventService, PluginConfig config, BossBarService bossBarService) {
        this.eventService = eventService;
        this.config = config;
        this.bossBarService = bossBarService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!eventService.isActive()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (!eventService.isAliveJuggernaut(victim.getUniqueId())) {
            return;
        }

        Player damager = resolveDamager(event);
        if (damager == null) {
            return;
        }
        if (eventService.isJuggernaut(damager.getUniqueId())) {
            return;
        }

        double damage = event.getFinalDamage();
        if (config.isCapOverkillDamage()) {
            // A killing blow can report far more than the health it actually removed
            damage = Math.min(damage, victim.getHealth());
        }
        eventService.getDamageTracker().addDamage(damager, damage);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!eventService.isActive()) {
            return;
        }
        Player dead = event.getEntity();
        if (!eventService.isAliveJuggernaut(dead.getUniqueId())) {
            return;
        }
        eventService.handleJuggernautDeath(dead);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        eventService.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bossBarService.hide(event.getPlayer());
        eventService.handleQuit(event.getPlayer());
    }

    private static Player resolveDamager(EntityDamageByEntityEvent event) {
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
