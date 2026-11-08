package com.frostsmp.event.listener;

import com.frostsmp.event.FrostEventPlugin;
import com.frostsmp.event.config.PluginConfig;
import com.frostsmp.event.service.EventService;
import com.frostsmp.event.service.SpawnProtectionService;
import com.frostsmp.event.util.ArmorUtil;
import com.frostsmp.event.util.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class EventListener implements Listener {

    private final FrostEventPlugin plugin;
    private final PluginConfig config;
    private final EventService eventService;
    private final SpawnProtectionService spawnProtection;

    public EventListener(FrostEventPlugin plugin, PluginConfig config, EventService eventService) {
        this.plugin = plugin;
        this.config = config;
        this.eventService = eventService;
        this.spawnProtection = eventService.getSpawnProtection();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Relogging must not keep spawn protection
        spawnProtection.clearQuiet(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (!eventService.isRunning()) {
            return;
        }
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (eventService.isRunning()) {
                spawnProtection.grant(player, true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!eventService.isRunning()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event);
        if (attacker == null || attacker.equals(victim)) {
            return;
        }

        // Hitting someone else ends your spawn protection
        if (spawnProtection.isProtected(attacker)) {
            spawnProtection.clear(attacker);
        }

        // Protected players cannot be hit
        if (spawnProtection.isProtected(victim)) {
            event.setCancelled(true);
            MessageUtil.send(attacker, config.getSpawnProtectionBlockedAttacker());
            return;
        }

        if (!ArmorUtil.isNaked(victim)) {
            return;
        }
        event.setCancelled(true);
        MessageUtil.send(attacker, config.getNakedBlockedAttacker());
        MessageUtil.send(victim, config.getNakedBlockedVictim());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        if (!eventService.isRunning()) {
            return;
        }
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) {
            return;
        }
        if (ArmorUtil.isNaked(victim) || spawnProtection.isProtected(victim)) {
            return;
        }
        eventService.recordKill(killer);
    }

    private static Player resolveAttacker(EntityDamageByEntityEvent event) {
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
